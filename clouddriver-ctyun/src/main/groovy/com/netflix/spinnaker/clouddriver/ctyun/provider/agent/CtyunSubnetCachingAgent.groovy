package com.netflix.spinnaker.clouddriver.ctyun.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.AccountAware
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.client.CloudVirtualMachineClient
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunVirtualPrivateCloudClient
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunSubnetDescription
import com.netflix.spinnaker.clouddriver.ctyun.provider.CtyunInfrastructureProvider
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.SUBNETS
import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.ZONES

@Slf4j
class CtyunSubnetCachingAgent implements CachingAgent, AccountAware {
  final ObjectMapper objectMapper
  final String region
  final String accountName
  final CtyunNamedAccountCredentials credentials
  final String providerName = CtyunInfrastructureProvider.name

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(SUBNETS.ns)
  ] as Set


  CtyunSubnetCachingAgent(
    CtyunNamedAccountCredentials creds,
    ObjectMapper objectMapper,
    String region
  ) {
    this.accountName = creds.name
    this.credentials = creds
    this.objectMapper = objectMapper
    this.region = region
  }

  @Override
  String getAgentType() {
    return "$accountName/$region/${this.class.simpleName}"
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info("Describing items in ${agentType}")

    def subnets = loadSubnetsAll()

    List<CacheData> data = subnets.collect() { CtyunSubnetDescription subnet ->
      Map<String, Object> attributes = [(SUBNETS.ns): subnet]
      new DefaultCacheData(Keys.getSubnetKey(subnet.subnetId, accountName, region),
        attributes,  [:])
    }

    log.info("Caching ${data.size()} items in ${agentType}")
    new DefaultCacheResult([(SUBNETS.ns): data])
  }

  private Set<CtyunSubnetDescription> loadSubnetsAll() {
    CtyunVirtualPrivateCloudClient vpcClient = new CtyunVirtualPrivateCloudClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )
    def newZones=[]
    try{
      def client = new CloudVirtualMachineClient(
        credentials.credentials.accessKey,
        credentials.credentials.securityKey,
        region
      )
      def myZones = client.getMyZones()
      def zones=client.getZones()
      myZones.each {s->
        Map<String,String> map=new HashMap()
        zones.each {ss->
          if(s.getAzName().equals(ss.name)){
            map.put("id",s.azID)
            map.put("name",s.azName)
            map.put("displayName",ss.azDisplayName)
            newZones.add(map)
          }
        }
      }
    }catch(Exception e){
      log.error("加载ctyun zone数据失败!{}",e)
    }

    def eips=[]
    try{
      def client = new CtyunVirtualPrivateCloudClient(
        credentials.credentials.accessKey,
        credentials.credentials.securityKey,
        region
      )
      def myeips = client.getEipsDownAll()
      myeips.each {s->
        Map<String,String> map=new HashMap()
        map.put("ID",s.ID)
        map.put("name",s.name)
        map.put("eipAddress",s.eipAddress)
        eips.add(map)
      }
    }catch(Exception e){
      log.error("加载ctyun eip数据失败!{}",e)
    }



    def vpcSet=vpcClient.getNetworksAll()
    def allSubnets=[] as Set<CtyunSubnetDescription>
    vpcSet.each {
      def subnetSet = vpcClient.getSubnetsAll(it.getVpcID())
      def subnetDescriptionSet =  subnetSet.collect {
        def subnetDesc = new CtyunSubnetDescription()
        subnetDesc.subnetId = it.subnetID
        subnetDesc.vpcId = it.vpcID
        subnetDesc.subnetName = it.name
        subnetDesc.cidrBlock = it.CIDR
        subnetDesc.isDefault = false
        subnetDesc.zone = it.availabilityZones==null?null:String.join(",", it.availabilityZones);
        //该账户下所有可用区域
        subnetDesc.myAllZones = newZones
        //eip数据
        subnetDesc.eips = eips
        subnetDesc
      }
      allSubnets.addAll(subnetDescriptionSet)
    }


    return allSubnets
  }

}
