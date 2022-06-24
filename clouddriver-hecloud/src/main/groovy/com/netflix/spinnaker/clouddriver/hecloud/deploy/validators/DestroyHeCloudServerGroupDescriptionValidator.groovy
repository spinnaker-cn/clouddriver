package com.netflix.spinnaker.clouddriver.hecloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.DestroyHeCloudServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HeCloudOperation(AtomicOperations.DESTROY_SERVER_GROUP)
@Component("destroyHeCloudServerGroupDescriptionValidator")
class DestroyHeCloudServerGroupDescriptionValidator extends DescriptionValidator<DestroyHeCloudServerGroupDescription> {
  @Override
  void validate(List priorDescriptions, DestroyHeCloudServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "hecloudDestroyServerGroupDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "hecloudDestroyServerGroupDescription.serverGroupName.empty"
    }
  }
}
