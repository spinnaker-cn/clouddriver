package com.netflix.spinnaker.clouddriver.ctyun.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.frigga.Names
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.SecurityGroupProvider
import com.netflix.spinnaker.clouddriver.model.securitygroups.Rule
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunSecurityGroup
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.provider.CtyunInfrastructureProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.SECURITY_GROUPS

@Slf4j
@Component
class CtyunSecurityGroupProvider implements SecurityGroupProvider<CtyunSecurityGroup> {
  final String cloudProvider = CtyunCloudProvider.ID
  final Cache cacheView
  final ObjectMapper objectMapper
  private final CtyunInfrastructureProvider ctyunProvider

  @Autowired
  CtyunSecurityGroupProvider(CtyunInfrastructureProvider tCloudProvider, Cache cacheView, ObjectMapper objectMapper) {
    this.ctyunProvider = tCloudProvider
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<CtyunSecurityGroup> getAll(boolean includeRules) {
    log.info("Enter CtyunSecurityGroupProvider getAll,includeRules=${includeRules}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', '*', '*'), includeRules)
  }

  @Override
  Set<CtyunSecurityGroup> getAllByRegion(boolean includeRules, String region) {
    log.info("Enter CtyunSecurityGroupProvider getAllByRegion,includeRules=${includeRules},region=${region}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', '*', region), includeRules)
  }

  @Override
  Set<CtyunSecurityGroup> getAllByAccount(boolean includeRules, String account) {
    log.info("Enter CtyunSecurityGroupProvider getAllByAccount,includeRules=${includeRules},account=${account}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', account, '*'), includeRules)
  }

  @Override
  Set<CtyunSecurityGroup> getAllByAccountAndName(boolean includeRules, String account, String securityGroupName) {
    log.info("Enter CtyunSecurityGroupProvider getAllByAccountAndName,includeRules=${includeRules}," +
      "account=${account},securityGroupName=${securityGroupName}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*', securityGroupName, account,'*'), includeRules)
  }

  @Override
  Set<CtyunSecurityGroup> getAllByAccountAndRegion(boolean includeRules, String account, String region) {
    log.info("Enter CtyunSecurityGroupProvider getAllByAccountAndRegion,includeRules=${includeRules}," +
      "account=${account},region=${region}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', account, region), includeRules)
  }

  @Override
  CtyunSecurityGroup get(String account, String region, String securityGroupName, String other) {
    log.info("Enter CtyunSecurityGroupProvider get,account=${account},region=${region},securityGroupName=${securityGroupName},vpcId=${other}")
    Set<CtyunSecurityGroup> csgSet=getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*', securityGroupName, account, region), true)
    CtyunSecurityGroup csg=csgSet?.find {it.vpcId==other}
    return csg
  }

  @Override
  CtyunSecurityGroup getById(String account, String region, String id, String vpcId) {
    //TO-DO
    log.info("Enter CtyunSecurityGroupProvider get,account=${account},region=${region},id=${id}")
    return get(account,region,id,vpcId)
  }

  Set<CtyunSecurityGroup> getAllMatchingKeyPattern(String pattern, boolean includeRules) {
    log.info("Enter getAllMatchingKeyPattern pattern = ${pattern}")
    loadResults(includeRules, cacheView.filterIdentifiers(SECURITY_GROUPS.ns, pattern))
  }

  Set<CtyunSecurityGroup> loadResults(boolean includeRules, Collection<String> identifiers) {
    def transform = this.&fromCacheData.curry(includeRules)
    def data = cacheView.getAll(SECURITY_GROUPS.ns, identifiers, RelationshipCacheFilter.none())
    def transformed = data.collect(transform)

    return transformed
  }

  CtyunSecurityGroup fromCacheData(boolean includeRules, CacheData cacheData) {
    //log.info("securityGroup cacheData = ${cacheData.id},${cacheData.attributes[SECURITY_GROUPS.ns]}")
    CtyunSecurityGroupDescription sg = objectMapper.convertValue(cacheData.attributes[SECURITY_GROUPS.ns], CtyunSecurityGroupDescription)
    def parts = Keys.parse(cacheData.id)
    def names = Names.parseName(sg.securityGroupName)

    new CtyunSecurityGroup(
      id: sg.securityGroupId,
      name: sg.securityGroupName,
      description: sg.securityGroupDesc,
      accountName: parts.account?: "none",
      application: names?.app?: "none",
      region: parts.region?: "none",
      vpcId: sg.vpcId,
      inboundRules: [],
      outboundRules: [],
      inRules: sg.inRules,
      outRules: sg.outRules
    )
  }

}
