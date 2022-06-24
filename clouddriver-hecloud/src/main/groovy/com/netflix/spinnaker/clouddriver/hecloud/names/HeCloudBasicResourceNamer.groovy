package com.netflix.spinnaker.clouddriver.hecloud.names

import com.netflix.frigga.Names
import com.netflix.spinnaker.clouddriver.names.NamingStrategy
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudBasicResource
import com.netflix.spinnaker.moniker.Moniker
import org.springframework.stereotype.Component


@Component
class HeCloudBasicResourceNamer implements NamingStrategy<HeCloudBasicResource> {
  @Override
  String getName() {
    return "hecloudAnnotations"
  }

  void applyMoniker(HeCloudBasicResource hecloudBasicResource, Moniker moniker) {
  }

  @Override
  Moniker deriveMoniker(HeCloudBasicResource hecloudBasicResource) {
    String name = hecloudBasicResource.monikerName
    Names parsed = Names.parseName(name)

    Moniker moniker = Moniker.builder()
      .app(parsed.getApp())
      .cluster(parsed.getCluster())
      .detail(parsed.getDetail())
      .stack(parsed.getStack())
      .detail(parsed.getDetail())
      .sequence(parsed.getSequence())
      .build()

    return moniker
  }
}
