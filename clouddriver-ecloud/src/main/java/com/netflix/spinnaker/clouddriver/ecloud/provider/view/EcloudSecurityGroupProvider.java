package com.netflix.spinnaker.clouddriver.ecloud.provider.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.frigga.Names;
import com.netflix.spinnaker.cats.cache.Cache;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudProvider;
import com.netflix.spinnaker.clouddriver.ecloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudSecurityGroup;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudSecurityGroupDescription;
import com.netflix.spinnaker.clouddriver.model.SecurityGroupProvider;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EcloudSecurityGroupProvider implements SecurityGroupProvider<EcloudSecurityGroup> {

  private final Cache cacheView;
  private final ObjectMapper objectMapper;
  private final EcloudProvider provider;

  @Autowired
  public EcloudSecurityGroupProvider(
      EcloudProvider provider, Cache cacheView, ObjectMapper objectMapper) {
    this.provider = provider;
    this.cacheView = cacheView;
    this.objectMapper = objectMapper;
  }

  @Override
  public String getCloudProvider() {
    return EcloudProvider.ID;
  }

  @Override
  public Set<EcloudSecurityGroup> getAll(boolean includeRules) {
    log.info("Enter ECloudSecurityGroupProvider getAll,includeRules={}", includeRules);
    return getAllMatchingKeyPattern(Keys.getSecurityGroupKey("*", "*", "*", "*"), includeRules);
  }

  @Override
  public Set<EcloudSecurityGroup> getAllByRegion(boolean includeRules, String region) {
    log.info(
        "Enter ECloudSecurityGroupProvider getAllByRegion,includeRules={},region={}",
        includeRules,
        region);
    return getAllMatchingKeyPattern(Keys.getSecurityGroupKey("*", "*", "*", region), includeRules);
  }

  @Override
  public Set<EcloudSecurityGroup> getAllByAccount(boolean includeRules, String account) {
    log.info(
        "Enter ECloudSecurityGroupProvider getAllByAccount,includeRules={},account={}",
        includeRules,
        account);
    return getAllMatchingKeyPattern(Keys.getSecurityGroupKey("*", "*", account, "*"), includeRules);
  }

  @Override
  public Set<EcloudSecurityGroup> getAllByAccountAndName(
      boolean includeRules, String account, String securityGroupName) {
    log.info(
        "Enter ECloudSecurityGroupProvider getAllByAccountAndName,includeRules={},account={},securityGroupName={}",
        includeRules,
        account,
        securityGroupName);
    return getAllMatchingKeyPattern(
        Keys.getSecurityGroupKey("*", securityGroupName, account, "*"), includeRules);
  }

  @Override
  public Set<EcloudSecurityGroup> getAllByAccountAndRegion(
      boolean includeRules, String account, String region) {
    log.info(
        "Enter ECloudSecurityGroupProvider getAllByAccountAndRegion,includeRules={},account={},region={}",
        includeRules,
        account,
        region);
    return getAllMatchingKeyPattern(
        Keys.getSecurityGroupKey("*", "*", account, region), includeRules);
  }

  @Override
  public EcloudSecurityGroup get(
      String account, String region, String securityGroupName, String other) {
    log.info(
        "Enter ECloudSecurityGroupProvider get,account={},region={},securityGroupName={}",
        account,
        region,
        securityGroupName);
    return getAllMatchingKeyPattern(
            Keys.getSecurityGroupKey("*", securityGroupName, account, region), true)
        .stream()
        .findFirst()
        .orElse(null);
  }

  @Override
  public EcloudSecurityGroup getById(String account, String region, String id, String vpcId) {
    // TO-DO
    log.info(
        "Enter ECloudSecurityGroupProvider get,account={},region={},id={}", account, region, id);
    return get(account, region, id, vpcId);
  }

  private Set<EcloudSecurityGroup> getAllMatchingKeyPattern(String pattern, boolean includeRules) {
    log.info("Enter getAllMatchingKeyPattern pattern = {}", pattern);
    return loadResults(
        includeRules, cacheView.filterIdentifiers(Keys.Namespace.SECURITY_GROUPS.ns, pattern));
  }

  private Set<EcloudSecurityGroup> loadResults(
      boolean includeRules, Collection<String> identifiers) {
    Set<EcloudSecurityGroup> transformed = new HashSet<>();
    for (String identifier : identifiers) {
      CacheData cacheData = cacheView.get(Keys.Namespace.SECURITY_GROUPS.ns, identifier);
      if (cacheData != null) {
        transformed.add(fromCacheData(includeRules, cacheData));
      }
    }
    return transformed;
  }

  private EcloudSecurityGroup fromCacheData(boolean includeRules, CacheData cacheData) {
    log.info("securityGroup cacheData = {},includeRules={}", cacheData, includeRules);
    EcloudSecurityGroupDescription sg =
        objectMapper.convertValue(
            cacheData.getAttributes().get(Keys.Namespace.SECURITY_GROUPS.ns),
            EcloudSecurityGroupDescription.class);
    Map<String, String> parts = Keys.parse(cacheData.getId());
    Names names = Names.parseName(sg.getSecurityGroupName());
    return EcloudSecurityGroup.builder()
        .id(sg.getSecurityGroupId())
        .name(sg.getSecurityGroupName())
        .description(sg.getSecurityGroupDesc())
        .accountName(parts.get("account") != null ? parts.get("account") : "none")
        .application(names.getApp())
        .region(parts.get("region") != null ? parts.get("region") : "none")
        .inboundRules(new HashSet<>())
        .outboundRules(new HashSet<>())
        .inRules(sg.getInRules())
        .outRules(sg.getOutRules())
        .build();
  }
}
