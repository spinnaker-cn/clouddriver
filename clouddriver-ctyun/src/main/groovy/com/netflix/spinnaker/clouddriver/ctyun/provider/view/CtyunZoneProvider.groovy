package com.netflix.spinnaker.clouddriver.ctyun.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunKeyPair
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunZone
import com.netflix.spinnaker.clouddriver.model.KeyPairProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.ZONES

@Slf4j
@Component
class CtyunZoneProvider {

  @Autowired
  Cache cacheView

  private final ObjectMapper objectMapper

  @Autowired
  CtyunZoneProvider(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper
  }

  Set<CtyunZone> getAll(String account,String region) {
    cacheView.getAll(
      ZONES.ns,
      cacheView.filterIdentifiers(
        ZONES.ns,
        Keys.getZonesKey('*',account,region)
      ),
      RelationshipCacheFilter.none()).collect {
      objectMapper.convertValue it.attributes.zone, CtyunZone
    }
  }
}
