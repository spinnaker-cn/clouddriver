package com.netflix.spinnaker.clouddriver.ctyun.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.KeyPairProvider
import com.netflix.spinnaker.clouddriver.ctyun.cache.Keys
import com.netflix.spinnaker.clouddriver.ctyun.model.CtyunKeyPair
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.ctyun.cache.Keys.Namespace.KEY_PAIRS

@Slf4j
@Component
class CtyunKeyPairProvider implements KeyPairProvider<CtyunKeyPair> {

  @Autowired
  Cache cacheView

  private final ObjectMapper objectMapper

  @Autowired
  CtyunKeyPairProvider(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper
  }

  @Override
  Set<CtyunKeyPair> getAll() {
    cacheView.getAll(
      KEY_PAIRS.ns,
      cacheView.filterIdentifiers(
        KEY_PAIRS.ns,
        Keys.getKeyPairKey('*','*','*')
      ),
      RelationshipCacheFilter.none()).collect {
      objectMapper.convertValue it.attributes.keyPair, CtyunKeyPair
    }
  }
}
