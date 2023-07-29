package com.netflix.spinnaker.clouddriver.ctyun.deploy

import com.netflix.frigga.Names
import com.netflix.spinnaker.clouddriver.helpers.AbstractServerGroupNameResolver
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.ctyun.CtyunCloudProvider
import com.netflix.spinnaker.clouddriver.ctyun.client.CtyunAutoScalingClient
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunBasicResource
import com.netflix.spinnaker.clouddriver.ctyun.provider.view.CtyunClusterProvider
import com.netflix.spinnaker.clouddriver.ctyun.security.CtyunNamedAccountCredentials
import com.netflix.spinnaker.moniker.Namer


class CtyunServerGroupNameResolver extends AbstractServerGroupNameResolver{
  private static final String CTYUN_PHASE = "CTYUN_DEPLOY"
  private final String accountName
  private final String region
  private final CtyunClusterProvider ctyunClusterProvider
  private final CtyunAutoScalingClient ctyunAutoScalingClient
  private final Namer namer

  CtyunServerGroupNameResolver(
    String accountName,
    String region,
    CtyunClusterProvider ctyunClusterProvider,
    CtyunNamedAccountCredentials credentials
  ) {
    this.accountName = accountName
    this.region = region
    this.ctyunClusterProvider = ctyunClusterProvider
    this.namer = NamerRegistry.lookup()
      .withProvider(CtyunCloudProvider.ID)
      .withAccount(accountName)
      .withResource(CtyunBasicResource)
    this.ctyunAutoScalingClient = new CtyunAutoScalingClient(
      credentials.credentials.accessKey,
      credentials.credentials.securityKey,
      region
    )
  }

  @Override
  String getPhase() {
    return CTYUN_PHASE
  }

  @Override
  String getRegion() {
    return region
  }

  @Override
  List<AbstractServerGroupNameResolver.TakenSlot> getTakenSlots(String clusterName) {
    def applicationName = Names.parseName(clusterName).app
    def cluster = ctyunClusterProvider.getCluster(applicationName, accountName, clusterName)
    if (!cluster) {
      []
    }
    else {
      def autoScalingGroups = ctyunAutoScalingClient.getAllAutoScalingGroups2()
      def serverGroupsInCluster = autoScalingGroups.findAll {
        Names.parseName(it.name).cluster == clusterName
      }

      return serverGroupsInCluster.collect {
        def name = it.name
        def date = CtyunAutoScalingClient.ConvertIsoDateTime(it.createDate)
        new AbstractServerGroupNameResolver.TakenSlot(
            serverGroupName: name,
            sequence: Names.parseName(name).sequence,
            createdTime: date
          )
      }
    }
  }
}
