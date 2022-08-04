package com.netflix.spinnaker.clouddriver.hecloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.TerminateHeCloudInstancesDescription
import com.netflix.spinnaker.clouddriver.hecloud.deploy.ops.TerminateHeCloudInstancesAtomicOperation
import org.springframework.stereotype.Component


@HeCloudOperation(AtomicOperations.TERMINATE_INSTANCES)
@Component("terminateHeCloudInstancesDescription")
class TerminateHeCloudInstancesAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  TerminateHeCloudInstancesDescription convertDescription(Map input) {
    HeCloudAtomicOperationConverterHelper.convertDescription(input, this, TerminateHeCloudInstancesDescription)
  }

  @Override
  AtomicOperation convertOperation(Map input) {
    new TerminateHeCloudInstancesAtomicOperation(convertDescription(input))
  }
}
