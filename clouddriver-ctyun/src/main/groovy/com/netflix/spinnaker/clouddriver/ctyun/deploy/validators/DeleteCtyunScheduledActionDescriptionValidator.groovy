package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunScheduledActionDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@Component("deleteCtyunScheduledActionDescriptionValidator")
class DeleteCtyunScheduledActionDescriptionValidator extends DescriptionValidator<DeleteCtyunScheduledActionDescription> {
  @Override
  void validate(List priorDescriptions, DeleteCtyunScheduledActionDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "deleteScheduledActionDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "deleteScheduledActionDescription.serverGroupName.empty"
    }

    if (!description.scheduledActionId) {
      errors.rejectValue "scheduledActionId", "deleteScheduledActionDescription.scalingPolicyId.empty"
    }
  }
}
