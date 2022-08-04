package com.netflix.spinnaker.clouddriver.hecloud

import com.netflix.spinnaker.clouddriver.core.CloudProvider
import org.springframework.stereotype.Component

import java.lang.annotation.Annotation

@Component
class HeCloudProvider implements CloudProvider {
  public static final String ID = "hecloud"
  final String id = ID
  final String displayName = "HeCloud"
  final Class<Annotation> operationAnnotationType = HeCloudOperation
}
