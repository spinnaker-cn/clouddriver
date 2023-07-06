package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.ResizeCtyunServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@CtyunOperation(AtomicOperations.RESIZE_SERVER_GROUP)
@Component("resizeCtyunServerGroupDescriptionValidator")
class ResizeCtyunServerGroupDescriptionValidator extends DescriptionValidator<ResizeCtyunServerGroupDescription> {
  @Override
  void validate(
    List priorDescriptions, ResizeCtyunServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "ResizeCtyunServerGroupDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "ResizeCtyunServerGroupDescription.serverGroupName.empty"
    }

    if (description.capacity == null) {
      errors.rejectValue "capacity", "ResizeCtyunServerGroupDescription.capacity.empty"
    }
  }
}
