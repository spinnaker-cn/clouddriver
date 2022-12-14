package com.netflix.spinnaker.clouddriver.hecloud.client

import com.netflix.spinnaker.clouddriver.hecloud.constants.HeCloudConstants
import com.netflix.spinnaker.clouddriver.hecloud.exception.HeCloudOperationException
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudSecurityGroupRule

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import com.huaweicloud.sdk.vpc.v2.VpcClient
import com.huaweicloud.sdk.vpc.v2.model.*
import groovy.util.logging.Slf4j

@Slf4j
class HeCloudVirtualPrivateCloudClient {
  private final DEFAULT_LIMIT = 100
  String region
  VpcClient client

  HeCloudVirtualPrivateCloudClient(String accessKeyId, String accessSecretKey, String region) {
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey).withIamEndpoint(HeCloudConstants.Region.getIamEndPoint(region))
    def regionId = new Region(region, "https://vpc." + region + "." + HeCloudConstants.END_POINT_SUFFIX)
    def config = HttpConfig.getDefaultHttpConfig()
    this.region = region
    client = VpcClient.newBuilder()
      .withHttpConfig(config)
      .withCredential(auth)
      .withRegion(regionId)
      .build()
  }

  String createSecurityGroup(String groupName) {
    try {
      CreateSecurityGroupRequest req = new CreateSecurityGroupRequest()
      CreateSecurityGroupRequestBody body = new CreateSecurityGroupRequestBody()
      CreateSecurityGroupOption securityGroupbody = new CreateSecurityGroupOption()
      securityGroupbody.setName(groupName)
      body.setSecurityGroup(securityGroupbody);
      req.setBody(body);
      CreateSecurityGroupResponse resp = client.CreateSecurityGroup(req)
      return resp.getSecurityGroup().getId()
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  String createSecurityGroupRule(String groupId, HeCloudSecurityGroupRule inRule) {
    try {
      CreateSecurityGroupRuleRequest req = new CreateSecurityGroupRuleRequest()
      CreateSecurityGroupRuleRequestBody body = new CreateSecurityGroupRuleRequestBody()
      CreateSecurityGroupRuleOption securityGroupRulebody = new CreateSecurityGroupRuleOption()
      securityGroupRulebody.withSecurityGroupId(groupId)
        .withDirection("ingress");

      if (inRule.getIpProtocol() != null) {
        securityGroupRulebody.setProtocol(inRule.getIpProtocol());
      }
      if (inRule.getStartPortRange() != null) {
        securityGroupRulebody.setPortRangeMin(inRule.getStartPortRange())
      }
      if (inRule.getEndPortRange() != null) {
        securityGroupRulebody.setPortRangeMax(inRule.getEndPortRange())
      }
      if (inRule.getSourceCidrIp() != null) {
        securityGroupRulebody.setRemoteIpPrefix(inRule.getSourceCidrIp())
      }
      if (inRule.getRemoteGroupId() != null) {
        securityGroupRulebody.setRemoteGroupId(inRule.getRemoteGroupId())
      }

      body.withSecurityGroupRule(securityGroupRulebody)
      req.setBody(body)
      client.createSecurityGroupRule(req)
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
    return ""
  }

  String deleteSecurityGroupInRule(String ruleId) {
    try {
      DeleteSecurityGroupRuleRequest req = new DeleteSecurityGroupRuleRequest()
      req.setSecurityGroupRuleId(ruleId)
      client.deleteSecurityGroupRule(request);
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
    return ""
  }

  List<SecurityGroup> getSecurityGroupsAll() {
    def marker = ""
    List<SecurityGroup> securityGroupAll = []
    while (true) {
      def req = new ListSecurityGroupsRequest()
      req.setLimit(DEFAULT_LIMIT)
      if (marker) {
        req.setMarker(marker)
      }
      def resp
      try {
        resp = client.listSecurityGroups(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listSecurityGroups (limit: {}, marker: {}, region: {})",
          DEFAULT_LIMIT,
          req.getMarker(),
          region,
          e
        )
      }
      if (resp == null || resp.getSecurityGroups() == null || resp.getSecurityGroups().size() == 0) {
        break
      }
      def sgs = resp.getSecurityGroups()
      securityGroupAll.addAll(sgs)
      marker = sgs[sgs.size() - 1].getId()
    }
    return securityGroupAll
  }

  List<SecurityGroup> getSecurityGroupById(String securityGroupId) {
    try {
      ShowSecurityGroupRequest req = new ShowSecurityGroupRequest()
      req.setSecurityGroupId(securityGroupId)
      ShowSecurityGroupResponse resp = client.showSecurityGroup(req)
      return resp.getSecurityGroup()
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  void deleteSecurityGroup(String securityGroupId) {
    try {
      DeleteSecurityGroupRequest req = new DeleteSecurityGroupRequest()
      req.setSecurityGroupId(securityGroupId)
      client.DeleteSecurityGroup(req)
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  List<Vpc> getNetworksAll() {
    def marker = ""
    List<Vpc> networkAll = []
    while (true) {
      def req = new ListVpcsRequest().withLimit(DEFAULT_LIMIT)
      if (marker) {
        req.setMarker(marker)
      }
      def resp
      try {
        resp = client.listVpcs(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listVpcs (limit: {}, marker: {}, region: {})",
          DEFAULT_LIMIT,
          req.getMarker(),
          region,
          e
        )
      }
      if (resp == null || resp.getVpcs() == null || resp.getVpcs().size() == 0) {
        break
      }
      def vpcs = resp.getVpcs()
      networkAll.addAll(vpcs)
      marker = vpcs[vpcs.size() - 1].getId()

    }
    return networkAll
  }

  List<Subnet> getSubnetsAll() {
    def marker = ""
    List<Subnet> subnetAll = []
    while (true) {
      def req = new ListSubnetsRequest()
      req.setLimit(DEFAULT_LIMIT)
      if (marker) {
        req.setMarker(marker)
      }
      def resp
      try {
        resp = client.listSubnets(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listSubnets (limit: {}, marker: {}, region: {})",
          DEFAULT_LIMIT,
          req.getMarker(),
          region,
          e
        )
      }
      if (resp == null || resp.getSubnets() == null || resp.getSubnets().size() == 0) {
        break
      }
      def subnets = resp.getSubnets()
      subnetAll.addAll(subnets)
      marker = subnets[subnets.size() - 1].getId()
    }
    return subnetAll
  }

  Subnet getSubnet(String subnetId) {
    def req = new ShowSubnetRequest().withSubnetId(subnetId)
    try {
      def resp = client.showSubnet(req)
      return resp.getSubnet()
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  Port getPort(String portId) {
    def req = new ShowPortRequest().withPortId(portId)
    try {
      def resp = client.showPort(req)
      return resp.getPort()
    } catch (ServiceResponseException e) {
      throw new HeCloudOperationException(e.getErrorMsg())
    }
  }

  List<Port> getAllPorts() {
    def marker = ""
    List<Port> portAll = []
    while (true) {
      def req = new ListPortsRequest().withLimit(DEFAULT_LIMIT)
      if (marker) {
        req.setMarker(marker)
      }
      def resp
      try {
        resp = client.listPorts(req)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listPorts (limit: {}, marker: {}, region: {})",
          DEFAULT_LIMIT,
          req.getMarker(),
          region,
          e
        )
      }
      if (resp == null || resp.getPorts() == null || resp.getPorts().size() == 0) {
        break
      }
      def ports = resp.getPorts()
      portAll.addAll(ports)
      marker = ports[ports.size() - 1].getId()
    }
    return portAll
  }
}
