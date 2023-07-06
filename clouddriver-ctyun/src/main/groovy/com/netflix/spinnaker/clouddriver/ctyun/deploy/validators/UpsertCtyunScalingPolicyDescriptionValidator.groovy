package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunAlarmActionDescription
import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation

import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@CtyunOperation(AtomicOperations.UPSERT_SCALING_POLICY)
@Component("upsertCtyunScalingPolicyDescriptionValidator")
class UpsertCtyunScalingPolicyDescriptionValidator extends DescriptionValidator<UpsertCtyunAlarmActionDescription> {

  @Override
  void validate(List priorDescriptions, UpsertCtyunAlarmActionDescription description, Errors errors) {
    if (!description.regionID) {
      errors.rejectValue "region", "upsertScalingPolicyDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "upsertScalingPolicyDescription.serverGroupName.empty"
    }
  }
}
