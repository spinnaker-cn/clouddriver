package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.EnableDisableCtyunServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@CtyunOperation(AtomicOperations.DISABLE_SERVER_GROUP)
@Component("disableCtyunServerGroupDescriptionValidator")
class DisableCtyunServerGroupDescriptionValidator extends DescriptionValidator<EnableDisableCtyunServerGroupDescription> {
  @Override
  void validate(List priorDescriptions, EnableDisableCtyunServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "disableCtyunServerGroupDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "disableCtyunServerGroupDescription.serverGroupName.empty"
    }
  }
}
