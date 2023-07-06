package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunScalingAlarmPolicyDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.DeleteCtyunScalingPolicyAtomicOperation
import org.springframework.stereotype.Component

@CtyunOperation(AtomicOperations.DELETE_SCALING_POLICY)
@Component("deleteCtyunScalingPolicyDescription")
class DeleteCtyunScalingPolicyAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  DeleteCtyunScalingAlarmPolicyDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, DeleteCtyunScalingAlarmPolicyDescription)
  }

  @Override
  AtomicOperation convertOperation(Map input) {
    new DeleteCtyunScalingPolicyAtomicOperation(convertDescription(input))
  }
}
