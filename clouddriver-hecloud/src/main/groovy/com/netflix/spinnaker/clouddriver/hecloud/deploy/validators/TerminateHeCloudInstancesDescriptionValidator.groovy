package com.netflix.spinnaker.clouddriver.hecloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.TerminateHeCloudInstancesDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HeCloudOperation(AtomicOperations.TERMINATE_INSTANCES)
@Component("terminateHeCloudInstancesDescriptionValidator")
class TerminateHeCloudInstancesDescriptionValidator extends DescriptionValidator<TerminateHeCloudInstancesDescription> {
  @Override
  void validate(List priorDescriptions, TerminateHeCloudInstancesDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "TerminateHeCloudInstancesDescription.region.empty"
    }

    if (!description.instanceIds) {
      errors.rejectValue "instanceIds", "TerminateHeCloudInstancesDescription.instanceIds.empty"
    }
  }
}
