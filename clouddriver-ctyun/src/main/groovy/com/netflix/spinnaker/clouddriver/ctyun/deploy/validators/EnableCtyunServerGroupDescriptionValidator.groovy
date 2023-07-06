package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.EnableDisableCtyunServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@CtyunOperation(AtomicOperations.ENABLE_SERVER_GROUP)
@Component("enableCtyunServerGroupDescriptionValidator")
class EnableCtyunServerGroupDescriptionValidator extends DescriptionValidator<EnableDisableCtyunServerGroupDescription> {
  @Override
  void validate(List priorDescriptions, EnableDisableCtyunServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "enableCtyunServerGroupDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "enableCtyunServerGroupDescription.serverGroupName.empty"
    }
  }
}
