package com.netflix.spinnaker.clouddriver.hecloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.ResizeHeCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.hecloud.deploy.ops.ResizeHeCloudServerGroupAtomicOperation
import org.springframework.stereotype.Component

@HeCloudOperation(AtomicOperations.RESIZE_SERVER_GROUP)
@Component("resizeHeCloudServerGroupDescription")
class ResizeHeCloudServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  AtomicOperation convertOperation(Map input) {
    new ResizeHeCloudServerGroupAtomicOperation(convertDescription(input))
  }

  ResizeHeCloudServerGroupDescription convertDescription(Map input) {
    HeCloudAtomicOperationConverterHelper.convertDescription(input, this, ResizeHeCloudServerGroupDescription)
  }
}
