package com.netflix.spinnaker.clouddriver.hecloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.TerminateAndDecrementHeCloudServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors


@HeCloudOperation(AtomicOperations.TERMINATE_INSTANCE_AND_DECREMENT)
@Component("terminateAndDecrementHeCloudServerGroupDescriptionValidator")
class TerminateAndDecrementHeCloudServerGroupDescriptionValidator extends DescriptionValidator<TerminateAndDecrementHeCloudServerGroupDescription> {
  @Override
  void validate(List priorDescriptions, TerminateAndDecrementHeCloudServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "TerminateAndDecrementHeCloudServerGroupDescription.region.empty"
    }

    if (!description.instance) {
      errors.rejectValue "instance", "TerminateAndDecrementHeCloudServerGroupDescription.instance.empty"
    }
  }
}
