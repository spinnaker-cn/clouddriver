package com.netflix.spinnaker.clouddriver.hecloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.EnableDisableHeCloudServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HeCloudOperation(AtomicOperations.ENABLE_SERVER_GROUP)
@Component("enableHeCloudServerGroupDescriptionValidator")
class EnableHeCloudServerGroupDescriptionValidator extends DescriptionValidator<EnableDisableHeCloudServerGroupDescription> {
  @Override
  void validate(List priorDescriptions, EnableDisableHeCloudServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "enableHeCloudServerGroupDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "enableHeCloudServerGroupDescription.serverGroupName.empty"
    }
  }
}
