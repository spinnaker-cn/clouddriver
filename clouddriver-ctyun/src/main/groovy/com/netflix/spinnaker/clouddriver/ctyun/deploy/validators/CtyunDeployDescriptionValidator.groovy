package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.CtyunDeployDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@CtyunOperation(AtomicOperations.CREATE_SERVER_GROUP)
@Component("ctyunDeployDescriptionValidator")
class CtyunDeployDescriptionValidator extends DescriptionValidator<CtyunDeployDescription> {
  @Override
  void validate(List priorDescriptions, CtyunDeployDescription description, Errors errors) {

    if (!description.application) {
      errors.rejectValue "application", "CtyunDeployDescription.application.empty"
    }

    if (!description.imageId) {
      errors.rejectValue "imageId", "ctyunDeployDescription.imageId.empty"
    }

    if (!description.instanceType) {
      errors.rejectValue "instanceType", "ctyunDeployDescription.instanceType.empty"
    }

    if (!description.mazInfoList && !description.subnetIds) {
      errors.rejectValue "mazInfoList or subnetIds", "ctyunDeployDescription.subnetIds.or.mazInfoList.not.supplied"
    }

    if (description.maxSize == null) {
      errors.rejectValue "maxSize", "ctyunDeployDescription.maxSize.empty"
    }

    if (description.minSize == null) {
      errors.rejectValue "minSize", "ctyunDeployDescription.minSize.empty"
    }

    if (description.desiredCapacity == null) {
      errors.rejectValue "desiredCapacity", "ctyunDeployDescription.desiredCapacity.empty"
    }

  }
}
