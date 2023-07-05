package com.netflix.spinnaker.clouddriver.ctyun

import com.netflix.spinnaker.clouddriver.core.CloudProvider
import org.springframework.stereotype.Component

import java.lang.annotation.Annotation

@Component
class CtyunCloudProvider implements CloudProvider {
  public static final String ID = "ctyun"
  final String id = ID
  final String displayName = "Ctyun"
  final Class<Annotation> operationAnnotationType = CtyunOperation
}
