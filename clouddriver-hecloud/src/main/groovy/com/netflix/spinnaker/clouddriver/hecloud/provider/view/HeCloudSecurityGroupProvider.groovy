package com.netflix.spinnaker.clouddriver.hecloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.frigga.Names
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.SecurityGroupProvider
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudSecurityGroup
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.hecloud.provider.HeCloudInfrastructureProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.SECURITY_GROUPS

@Slf4j
@Component
class HeCloudSecurityGroupProvider implements SecurityGroupProvider<HeCloudSecurityGroup> {
  final String cloudProvider = HeCloudProvider.ID
  final Cache cacheView
  final ObjectMapper objectMapper
  private final HeCloudInfrastructureProvider hecloudProvider

  @Autowired
  HeCloudSecurityGroupProvider(HeCloudInfrastructureProvider tCloudProvider, Cache cacheView, ObjectMapper objectMapper) {
    this.hecloudProvider = tCloudProvider
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<HeCloudSecurityGroup> getAll(boolean includeRules) {
    log.info("Enter HeCloudSecurityGroupProvider getAll,includeRules=${includeRules}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', '*', '*'), includeRules)
  }

  @Override
  Set<HeCloudSecurityGroup> getAllByRegion(boolean includeRules, String region) {
    log.info("Enter HeCloudSecurityGroupProvider getAllByRegion,includeRules=${includeRules},region=${region}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', '*', region), includeRules)
  }

  @Override
  Set<HeCloudSecurityGroup> getAllByAccount(boolean includeRules, String account) {
    log.info("Enter HeCloudSecurityGroupProvider getAllByAccount,includeRules=${includeRules},account=${account}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', account, '*'), includeRules)
  }

  @Override
  Set<HeCloudSecurityGroup> getAllByAccountAndName(boolean includeRules, String account, String securityGroupName) {
    log.info("Enter HeCloudSecurityGroupProvider getAllByAccountAndName,includeRules=${includeRules}," +
      "account=${account},securityGroupName=${securityGroupName}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*', securityGroupName, account,'*'), includeRules)
  }

  @Override
  Set<HeCloudSecurityGroup> getAllByAccountAndRegion(boolean includeRules, String account, String region) {
    log.info("Enter HeCloudSecurityGroupProvider getAllByAccountAndRegion,includeRules=${includeRules}," +
      "account=${account},region=${region}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', account, region), includeRules)
  }

  @Override
  HeCloudSecurityGroup get(String account, String region, String securityGroupName, String other) {
    log.info("Enter HeCloudSecurityGroupProvider get,account=${account},region=${region},securityGroupName=${securityGroupName}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*', securityGroupName, account, region), true)[0]
  }

  @Override
  HeCloudSecurityGroup getById(String account, String region, String id, String vpcId) {
    //TO-DO
    log.info("Enter HeCloudSecurityGroupProvider get,account=${account},region=${region},id=${id}")
    return get(account,region,id,vpcId)
  }

  Set<HeCloudSecurityGroup> getAllMatchingKeyPattern(String pattern, boolean includeRules) {
    log.info("Enter getAllMatchingKeyPattern pattern = ${pattern}")
    loadResults(includeRules, cacheView.filterIdentifiers(SECURITY_GROUPS.ns, pattern))
  }

  Set<HeCloudSecurityGroup> loadResults(boolean includeRules, Collection<String> identifiers) {
    def transform = this.&fromCacheData.curry(includeRules)
    def data = cacheView.getAll(SECURITY_GROUPS.ns, identifiers, RelationshipCacheFilter.none())
    def transformed = data.collect(transform)

    return transformed
  }

  HeCloudSecurityGroup fromCacheData(boolean includeRules, CacheData cacheData) {
    //log.info("securityGroup cacheData = ${cacheData.id},${cacheData.attributes[SECURITY_GROUPS.ns]}")
    HeCloudSecurityGroupDescription sg = objectMapper.convertValue(cacheData.attributes[SECURITY_GROUPS.ns], HeCloudSecurityGroupDescription)
    def parts = Keys.parse(cacheData.id)
    def names = Names.parseName(sg.securityGroupName)

    new HeCloudSecurityGroup(
      id: sg.securityGroupId,
      name: sg.securityGroupName,
      description: sg.securityGroupDesc,
      accountName: parts.account?: "none",
      application: names?.app?: "none",
      region: parts.region?: "none",
      inboundRules: [],
      outboundRules: [],
      inRules: sg.inRules,
      outRules: sg.outRules
    )
  }

}
