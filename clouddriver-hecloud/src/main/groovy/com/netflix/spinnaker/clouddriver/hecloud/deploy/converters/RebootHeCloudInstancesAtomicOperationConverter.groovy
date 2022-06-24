package com.netflix.spinnaker.clouddriver.hecloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.RebootHeCloudInstancesDescription
import com.netflix.spinnaker.clouddriver.hecloud.deploy.ops.RebootHeCloudInstancesAtomicOperation
import org.springframework.stereotype.Component

@HeCloudOperation(AtomicOperations.REBOOT_INSTANCES)
@Component("rebootHeCloudInstancesDescription")
class RebootHeCloudInstancesAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  AtomicOperation convertOperation(Map input) {
    new RebootHeCloudInstancesAtomicOperation(convertDescription(input))
  }

  @Override
  RebootHeCloudInstancesDescription convertDescription(Map input) {
    HeCloudAtomicOperationConverterHelper.convertDescription(input, this, RebootHeCloudInstancesDescription)
  }
}
