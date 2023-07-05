package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunScalingAlarmPolicyDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@CtyunOperation(AtomicOperations.DELETE_SCALING_POLICY)
@Component("deleteCtyunScalingPolicyDescriptionValidator")
class DeleteCtyunScalingPolicyDescriptionValidator extends DescriptionValidator<DeleteCtyunScalingAlarmPolicyDescription> {
  @Override
  void validate(List priorDescriptions, DeleteCtyunScalingAlarmPolicyDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "deleteScalingPolicyDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "deleteScalingPolicyDescription.serverGroupName.empty"
    }

    if (!description.scalingPolicyId) {
      errors.rejectValue "scalingPolicyId", "deleteScalingPolicyDescription.scalingPolicyId.empty"
    }
  }
}
