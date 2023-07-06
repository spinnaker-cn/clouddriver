package com.netflix.spinnaker.clouddriver.ctyun.names

import com.netflix.frigga.Names
import com.netflix.spinnaker.clouddriver.names.NamingStrategy
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunBasicResource
import com.netflix.spinnaker.moniker.Moniker
import org.springframework.stereotype.Component


@Component
class CtyunBasicResourceNamer implements NamingStrategy<CtyunBasicResource> {
  @Override
  String getName() {
    return "ctyunAnnotations"
  }

  void applyMoniker(CtyunBasicResource ctyunBasicResource, Moniker moniker) {
  }

  @Override
  Moniker deriveMoniker(CtyunBasicResource ctyunBasicResource) {
    String name = ctyunBasicResource.monikerName
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
