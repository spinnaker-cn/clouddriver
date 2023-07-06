package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.TerminateAndDecrementCtyunServerGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.TerminateAndDecrementCtyunServerGroupAtomicOperation
import org.springframework.stereotype.Component

@CtyunOperation(AtomicOperations.TERMINATE_INSTANCE_AND_DECREMENT)
@Component("terminateAndDecrementCtyunServerGroupDescription")
class TerminateAndDecrementCtyunServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  TerminateAndDecrementCtyunServerGroupDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, TerminateAndDecrementCtyunServerGroupDescription)
  }

  @Override
  AtomicOperation convertOperation(Map input) {
    new TerminateAndDecrementCtyunServerGroupAtomicOperation(convertDescription(input))
  }
}
