package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudApplication;
import com.netflix.spinnaker.clouddriver.model.Application;
import com.netflix.spinnaker.clouddriver.model.ApplicationProvider;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
final class EcloudApplicationProvider implements ApplicationProvider {

  private final Cache cacheView;
  private final ObjectMapper objectMapper;

  @Autowired
  EcloudApplicationProvider(Cache cacheView, ObjectMapper objectMapper) {
    this.cacheView = cacheView;
    this.objectMapper = objectMapper;
  }

  @Override
  public Set<EcloudApplication> getApplications(boolean expand) {
    RelationshipCacheFilter filter =
        expand
            ? RelationshipCacheFilter.include(Keys.Namespace.CLUSTERS.ns)
            : RelationshipCacheFilter.none();
    Collection<CacheData> caches = cacheView.getAll(Keys.Namespace.APPLICATIONS.ns, filter);
    return this.translateApplications(caches);
  }

  @Override
  public Application getApplication(String name) {
    CacheData cache = cacheView.get(Keys.Namespace.APPLICATIONS.ns, Keys.getApplicationKey(name));
    return this.translateApplication(cache);
  }

  private Set<EcloudApplication> translateApplications(Collection<CacheData> datas) {
    Set<EcloudApplication> apps = new HashSet<>(16);
    if (datas != null && !datas.isEmpty()) {
      for (CacheData data : datas) {
        apps.add(translateApplication(data));
      }
    }
    return apps;
  }

  private EcloudApplication translateApplication(CacheData cache) {
    if (cache != null) {
      Map<String, String> applicationKey = Keys.parse(cache.getId());
      if (applicationKey != null) {
        EcloudApplication app = new EcloudApplication();
        app.setName(applicationKey.get("name"));
        Map<String, Set<String>> clusterNames = new HashMap<>();
        Set<String> clusterKeys = this.getRelationships(cache, Keys.Namespace.CLUSTERS.ns);
        for (String clusterKey : clusterKeys) {
          Map<String, String> clusterKeyInfo = Keys.parse(clusterKey);
          String account = clusterKeyInfo.get("account");
          String clusterName = clusterKeyInfo.get("name");
          Set<String> nameSet = clusterNames.get(account);
          if (nameSet == null) {
            nameSet = new HashSet<>();
            clusterNames.put(account, nameSet);
          }
          nameSet.add(clusterName);
        }
        Map<String, String> attributes = new HashMap<>();
        attributes.put("name", applicationKey.get("name"));
        app.setAttributes(attributes);
        app.setClusterNames(clusterNames);
        return app;
      }
    }
    return null;
  }

  private Set<String> getRelationships(CacheData cacheData, String relation) {
    Collection<String> relationships = cacheData.getRelationships().get(relation);
    return relationships == null ? Collections.emptySet() : new HashSet<>(relationships);
  }
}
