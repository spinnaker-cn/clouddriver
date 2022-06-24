package com.netflix.spinnaker.clouddriver.hecloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.InstanceTypeProvider
import com.netflix.spinnaker.clouddriver.hecloud.cache.Keys
import com.netflix.spinnaker.clouddriver.hecloud.model.HeCloudInstanceType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.hecloud.cache.Keys.Namespace.*

@Component
class HeCloudInstanceTypeProvider implements InstanceTypeProvider<HeCloudInstanceType> {
  @Autowired
  Cache cacheView

  private final ObjectMapper objectMapper

  @Autowired
  HeCloudInstanceTypeProvider(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper
  }

  @Override
  Set<HeCloudInstanceType> getAll() {
    def instance_type_set = cacheView.getAll(
      INSTANCE_TYPES.ns,
      cacheView.filterIdentifiers(
        INSTANCE_TYPES.ns,
        Keys.getInstanceTypeKey('*','*','*')
      ),
      RelationshipCacheFilter.none()).collect {
      objectMapper.convertValue(it.attributes.instanceType, HeCloudInstanceType)
    }


    def orderByFamilyAndCpuAndMem = new OrderBy([{it.instanceFamily}, { it.cpu }, { it.mem }])
    instance_type_set.sort(orderByFamilyAndCpuAndMem)
    return instance_type_set
  }
}
