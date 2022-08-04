package com.netflix.spinnaker.clouddriver.hecloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.ResizeHeCloudServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HeCloudOperation(AtomicOperations.RESIZE_SERVER_GROUP)
@Component("resizeHeCloudServerGroupDescriptionValidator")
class ResizeHeCloudServerGroupDescriptionValidator extends DescriptionValidator<ResizeHeCloudServerGroupDescription> {
  @Override
  void validate(
    List priorDescriptions, ResizeHeCloudServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "ResizeHeCloudServerGroupDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "ResizeHeCloudServerGroupDescription.serverGroupName.empty"
    }

    if (description.capacity == null) {
      errors.rejectValue "capacity", "ResizeHeCloudServerGroupDescription.capacity.empty"
    }
  }
}
