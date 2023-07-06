package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.RebootCtyunInstancesDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.RebootCtyunInstancesAtomicOperation
import org.springframework.stereotype.Component

@CtyunOperation(AtomicOperations.REBOOT_INSTANCES)
@Component("rebootCtyunInstancesDescription")
class RebootCtyunInstancesAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  AtomicOperation convertOperation(Map input) {
    new RebootCtyunInstancesAtomicOperation(convertDescription(input))
  }

  @Override
  RebootCtyunInstancesDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, RebootCtyunInstancesDescription)
  }
}
