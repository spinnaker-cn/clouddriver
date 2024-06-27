package com.netflix.spinnaker.clouddriver.ecloud.deploy.validators;

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.UpsertEcloudScalingPolicyDescription;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@EcloudOperation(AtomicOperations.UPSERT_SCALING_POLICY)
@Component("UpsertEcloudScalingPolicyDescriptionValidator")
public class UpsertEcloudScalingPolicyDescriptionValidator
    extends DescriptionValidator<UpsertEcloudScalingPolicyDescription> {

  @Override
  public void validate(
      List priorDescriptions, UpsertEcloudScalingPolicyDescription description, Errors errors) {
    if (description.getRegion() == null || description.getRegion().isEmpty()) {
      errors.rejectValue("region", "UpsertEcloudScalingPolicyDescription.region.empty");
    }

    if (description.getServerGroupName() == null || description.getServerGroupName().isEmpty()) {
      errors.rejectValue(
          "serverGroupName", "UpsertEcloudScalingPolicyDescription.serverGroupName.empty");
    }

    if (description.getPolicyName() == null || description.getPolicyName().isEmpty()) {
      errors.rejectValue("policyName", "UpsertEcloudScalingPolicyDescription.adjustmentType.empty");
    }

    if (description.getAdjustmentType() == null || description.getAdjustmentType().isEmpty()) {
      errors.rejectValue(
          "adjustmentType", "UpsertEcloudScalingPolicyDescription.adjustmentType.empty");
    }

    if (description.getAdjustmentValue() == null) {
      errors.rejectValue(
          "adjustmentValue", "UpsertEcloudScalingPolicyDescription.adjustmentValue.empty");
    }

    if (description.getCooldown() == null) {
      errors.rejectValue("cooldown", "UpsertEcloudScalingPolicyDescription.cooldown.empty");
    }
  }
}
