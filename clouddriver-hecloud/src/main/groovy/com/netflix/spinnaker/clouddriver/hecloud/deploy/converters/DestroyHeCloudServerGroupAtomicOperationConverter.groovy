package com.netflix.spinnaker.clouddriver.hecloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.DestroyHeCloudServerGroupDescription
import org.springframework.stereotype.Component
import com.netflix.spinnaker.clouddriver.hecloud.deploy.ops.DestroyHeCloudServerGroupAtomicOperation

@HeCloudOperation(AtomicOperations.DESTROY_SERVER_GROUP)
@Component("destroyHeCloudServerGroupDescription")
class DestroyHeCloudServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  AtomicOperation convertOperation(Map input) {
    new DestroyHeCloudServerGroupAtomicOperation(convertDescription(input))
  }

  DestroyHeCloudServerGroupDescription convertDescription(Map input) {
    HeCloudAtomicOperationConverterHelper.convertDescription(input, this, DestroyHeCloudServerGroupDescription)
  }
}
