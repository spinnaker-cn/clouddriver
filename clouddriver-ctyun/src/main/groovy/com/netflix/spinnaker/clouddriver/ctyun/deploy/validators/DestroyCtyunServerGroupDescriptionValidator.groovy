package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DestroyCtyunServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@CtyunOperation(AtomicOperations.DESTROY_SERVER_GROUP)
@Component("destroyCtyunServerGroupDescriptionValidator")
class DestroyCtyunServerGroupDescriptionValidator extends DescriptionValidator<DestroyCtyunServerGroupDescription> {
  @Override
  void validate(List priorDescriptions, DestroyCtyunServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "ctyunDestroyServerGroupDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "ctyunDestroyServerGroupDescription.serverGroupName.empty"
    }
  }
}
