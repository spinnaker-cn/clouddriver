package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.TerminateCtyunInstancesDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.TerminateCtyunInstancesAtomicOperation
import org.springframework.stereotype.Component


@CtyunOperation(AtomicOperations.TERMINATE_INSTANCES)
@Component("terminateCtyunInstancesDescription")
class TerminateCtyunInstancesAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  TerminateCtyunInstancesDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, TerminateCtyunInstancesDescription)
  }

  @Override
  AtomicOperation convertOperation(Map input) {
    new TerminateCtyunInstancesAtomicOperation(convertDescription(input))
  }
}
