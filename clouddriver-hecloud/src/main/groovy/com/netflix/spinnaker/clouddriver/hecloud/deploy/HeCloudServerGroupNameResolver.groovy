package com.netflix.spinnaker.clouddriver.hecloud.deploy

import com.netflix.frigga.Names
import com.netflix.spinnaker.clouddriver.helpers.AbstractServerGroupNameResolver
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudProvider
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudAutoScalingClient
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudBasicResource
import com.netflix.spinnaker.clouddriver.hecloud.provider.view.HeCloudClusterProvider
import com.netflix.spinnaker.clouddriver.hecloud.security.HeCloudNamedAccountCredentials
import com.netflix.spinnaker.moniker.Namer


class HeCloudServerGroupNameResolver extends AbstractServerGroupNameResolver{
  private static final String HECLOUD_PHASE = "HECLOUD_DEPLOY"
  private final String accountName
  private final String region
  private final HeCloudClusterProvider heCloudClusterProvider
  private final HeCloudAutoScalingClient autoScalingClient
  private final Namer namer

  HeCloudServerGroupNameResolver(
    String accountName,
    String region,
    HeCloudClusterProvider heCloudClusterProvider,
    HeCloudNamedAccountCredentials credentials
  ) {
    this.accountName = accountName
    this.region = region
    this.heCloudClusterProvider = heCloudClusterProvider
    this.namer = NamerRegistry.lookup()
      .withProvider(HeCloudProvider.ID)
      .withAccount(accountName)
      .withResource(HeCloudBasicResource)
    this.autoScalingClient = new HeCloudAutoScalingClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )
  }

  @Override
  String getPhase() {
    return HECLOUD_PHASE
  }

  @Override
  String getRegion() {
    return region
  }

  @Override
  List<AbstractServerGroupNameResolver.TakenSlot> getTakenSlots(String clusterName) {
    def applicationName = Names.parseName(clusterName).app
    def cluster = heCloudClusterProvider.getCluster(applicationName, accountName, clusterName)
    if (!cluster) {
      []
    }
    else {
      def autoScalingGroups = autoScalingClient.getAllAutoScalingGroups()
      def serverGroupsInCluster = autoScalingGroups.findAll {
        Names.parseName(it.getScalingGroupName()).cluster == clusterName
      }

      return serverGroupsInCluster.collect {
        def name = it.getScalingGroupName()
        def date = HeCloudElasticCloudServerClient.ConvertIsoDateTime(it.getCreateTime().toString())
        new AbstractServerGroupNameResolver.TakenSlot(
            serverGroupName: name,
            sequence: Names.parseName(name).sequence,
            createdTime: date
          )
      }
    }
  }
}
