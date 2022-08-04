package com.netflix.spinnaker.clouddriver.hecloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.HeCloudDeployDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HeCloudOperation(AtomicOperations.CREATE_SERVER_GROUP)
@Component("heCloudDeployDescriptionValidator")
class HeCloudDeployDescriptionValidator extends DescriptionValidator<HeCloudDeployDescription> {
  @Override
  void validate(List priorDescriptions, HeCloudDeployDescription description, Errors errors) {

    if (!description.application) {
      errors.rejectValue "application", "heCloudDeployDescription.application.empty"
    }

    if (!description.imageId) {
      errors.rejectValue "imageId", "heCloudDeployDescription.imageId.empty"
    }

    if (!description.instanceType) {
      errors.rejectValue "instanceType", "heCloudDeployDescription.instanceType.empty"
    }

    if (!description.subnetIds) {
      errors.rejectValue "subnetIds", "heCloudDeployDescription.subnetIds.empty"
    }

    if (description.maxSize == null) {
      errors.rejectValue "maxSize", "heCloudDeployDescription.maxSize.empty"
    }

    if (description.minSize == null) {
      errors.rejectValue "minSize", "heCloudDeployDescription.minSize.empty"
    }

    if (description.desiredCapacity == null) {
      errors.rejectValue "desiredCapacity", "heCloudDeployDescription.desiredCapacity.empty"
    }

  }
}
