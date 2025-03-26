package com.netflix.spinnaker.clouddriver.ecloud.controller;

import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.IMAGES;
import static com.netflix.spinnaker.clouddriver.ecloud.cache.Keys.Namespace.NAMED_IMAGES;

import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.kork.web.exceptions.InvalidRequestException;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ecloud/images")
public class EcloudNamedImageLookupController {
  private static final int MAX_SEARCH_RESULTS = 1000;
  private static final int MIN_NAME_FILTER = 3;
  private static final String EXCEPTION_REASON =
      "Minimum of " + MIN_NAME_FILTER + " characters required to filter namedImages";

  // private final String IMG_GLOB_PATTERN = "^img-([a-f0-9]{8})$";
  // [0-9a-fA-F\-]{36}
  private final String IMG_GLOB_PATTERN =
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$$";
  @Autowired private final Cache cacheView;

  public EcloudNamedImageLookupController(Cache cacheView) {
    this.cacheView = cacheView;
  }

  @GetMapping(value = "/{account}/{region}/{imageId:.+}")
  public List<NamedImage> getByImgId(
      @PathVariable String account, @PathVariable String region, @PathVariable String imageId) {
    CacheData cache = cacheView.get(IMAGES.ns, Keys.getImageKey(imageId, account, region));
    if (cache == null) {
      throw new NotFoundException(imageId + " not found in " + account + "/" + region);
    }
    Collection<String> namedImageKeys = cache.getRelationships().get(NAMED_IMAGES.ns);
    if (namedImageKeys == null) {
      throw new NotFoundException(
          "Name not found on image " + imageId + " in " + account + "/" + region);
    }
    return render(Collections.singletonList(cache), null, region, null);
  }

  @GetMapping(value = "/find")
  public List<NamedImage> list(LookupOptions lookupOptions, HttpServletRequest request) {
    validateLookupOptions(lookupOptions);

    String glob = lookupOptions.q.trim();
    boolean isImgId = glob.matches(IMG_GLOB_PATTERN);

    if (!isImgId
        && !glob.contains("*")
        && !glob.contains("?")
        && !glob.contains("[")
        && !glob.contains("\\")) {
      glob = "*" + glob + "*";
    }

    String namedImageSearch =
        Keys.getNamedImageKey(glob, lookupOptions.account != null ? lookupOptions.account : "*");
    String imageSearch =
        Keys.getImageKey(
            glob,
            lookupOptions.account != null ? lookupOptions.account : "*",
            lookupOptions.region != null ? lookupOptions.region : "*");

    Collection<String> namedImageIdentifiers =
        !isImgId
            ? cacheView.filterIdentifiers(NAMED_IMAGES.ns, namedImageSearch)
            : Collections.emptyList();
    Collection<String> imageIdentifiers =
        namedImageIdentifiers.isEmpty()
            ? cacheView.filterIdentifiers(IMAGES.ns, imageSearch)
            : Collections.emptyList();

    namedImageIdentifiers =
        new ArrayList<>(namedImageIdentifiers)
            .subList(0, Math.min(MAX_SEARCH_RESULTS, namedImageIdentifiers.size()));
    Collection<CacheData> matchesByName =
        cacheView.getAll(
            NAMED_IMAGES.ns, namedImageIdentifiers, RelationshipCacheFilter.include(IMAGES.ns));
    Collection<CacheData> matchesByImageId = cacheView.getAll(IMAGES.ns, imageIdentifiers);

    return render(matchesByName, matchesByImageId, lookupOptions.region, request);
  }

  private void validateLookupOptions(LookupOptions lookupOptions) {
    if (lookupOptions.q == null || lookupOptions.q.length() < MIN_NAME_FILTER) {
      throw new InvalidRequestException(EXCEPTION_REASON);
    }

    /*  String glob = lookupOptions.q.trim();
    boolean isImgId = glob.matches(IMG_GLOB_PATTERN);
    if (glob.equals("img") || (!isImgId && glob.startsWith("img-"))) {
      throw new InvalidRequestException(
          "Searches by Image Id must be an exact match (img-xxxxxxxx)");
    }*/
  }

  private List<NamedImage> render(
      Collection<CacheData> namedImages,
      Collection<CacheData> images,
      String requiredRegion,
      HttpServletRequest request) {
    Map<String, NamedImage> byImageName = new HashMap<>();

    for (CacheData cacheData : namedImages) {
      Map<String, String> keyParts = Keys.parse(cacheData.getId());
      NamedImage namedImage =
          byImageName.computeIfAbsent(keyParts.get("imageName"), k -> new NamedImage(k));
      namedImage.attributes.putAll(cacheData.getAttributes());
      namedImage.attributes.remove("name");
      namedImage.accounts.add(keyParts.get("account"));

      Collection<String> imageKeys = cacheData.getRelationships().get(IMAGES.ns);
      if (imageKeys != null) {
        for (String imageKey : imageKeys) {
          Map<String, String> imageParts = Keys.parse(imageKey);
          namedImage
              .imgIds
              .computeIfAbsent(imageParts.get("region"), k -> new HashSet<>())
              .add(imageParts.get("imageId"));
        }
      }
    }

    if (images != null) {
      for (CacheData cacheData : images) {
        Map<String, String> keyParts = Keys.parse(cacheData.getId());
        Map<String, String> namedImageKeyParts =
            Keys.parse(
                Objects.requireNonNull(cacheData.getRelationships().get(NAMED_IMAGES.ns))
                    .iterator()
                    .next());
        NamedImage namedImage =
            byImageName.computeIfAbsent(
                namedImageKeyParts.get("imageName"), k -> new NamedImage(k));
        Map<String, Object> image = (Map<String, Object>) cacheData.getAttributes().get("image");
        namedImage.attributes.put("osPlatform", image.get("osPlatform"));
        namedImage.attributes.put("osType", image.get("osType"));
        namedImage.attributes.put("type", image.get("type"));
        namedImage.attributes.put("snapshotId", cacheData.getAttributes().get("snapshotId"));
        namedImage.attributes.put("createdTime", image.get("createdTime"));
        namedImage.attributes.put("name", image.get("name"));
        namedImage.attributes.put("isPublic", image.get("isPublic"));
        namedImage.attributes.put("minDisk", image.get("minDisk"));
        namedImage.accounts.add(namedImageKeyParts.get("account"));
        namedImage
            .imgIds
            .computeIfAbsent(keyParts.get("region"), k -> new HashSet<>())
            .add(keyParts.get("imageId"));
      }
    }
    String imageName = request != null ? request.getParameter("imageName") : null;

    List<NamedImage> results = new ArrayList<>(byImageName.values());
    results.removeIf(
        namedImage ->
            !StringUtils.isEmpty(requiredRegion) && !namedImage.imgIds.containsKey(requiredRegion));
    results.removeIf(
        namedImage -> !StringUtils.isEmpty(imageName) && !namedImage.imageName.contains(imageName));

    return results;
  }

  @Data
  private class NamedImage {
    String imageName;
    Map<String, Object> attributes = new HashMap<>();
    Set<String> accounts = new HashSet<>();
    Map<String, Collection<String>> imgIds = new HashMap<>();

    NamedImage(String imageName) {
      this.imageName = imageName;
    }
  }

  static class LookupOptions {
    String q;
    String account;
    String region;

    public void setQ(String q) {
      this.q = q;
    }

    public void setAccount(String account) {
      this.account = account;
    }

    public void setRegion(String region) {
      this.region = region;
    }
  }
}
