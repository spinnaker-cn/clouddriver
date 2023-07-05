package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunScheduledActionDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@Component("upsertCtyunScheduledActionsDescriptionValidator")
class UpsertCtyunScheduledActionsDescriptionValidator extends DescriptionValidator<UpsertCtyunScheduledActionDescription> {
  @Override
  void validate(List priorDescriptions, UpsertCtyunScheduledActionDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "upsertScheduledActionDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "upsertScheduledActionDescription.serverGroupName.empty"
    }
  }
}
