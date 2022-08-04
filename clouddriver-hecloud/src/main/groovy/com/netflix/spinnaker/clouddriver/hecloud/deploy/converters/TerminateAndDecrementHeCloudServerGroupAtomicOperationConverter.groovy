package com.netflix.spinnaker.clouddriver.hecloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.TerminateAndDecrementHeCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.hecloud.deploy.ops.TerminateAndDecrementHeCloudServerGroupAtomicOperation
import org.springframework.stereotype.Component

@HeCloudOperation(AtomicOperations.TERMINATE_INSTANCE_AND_DECREMENT)
@Component("terminateAndDecrementHeCloudServerGroupDescription")
class TerminateAndDecrementHeCloudServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  TerminateAndDecrementHeCloudServerGroupDescription convertDescription(Map input) {
    HeCloudAtomicOperationConverterHelper.convertDescription(input, this, TerminateAndDecrementHeCloudServerGroupDescription)
  }

  @Override
  AtomicOperation convertOperation(Map input) {
    new TerminateAndDecrementHeCloudServerGroupAtomicOperation(convertDescription(input))
  }
}
