package com.netflix.spinnaker.clouddriver.hecloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.RebootHeCloudInstancesDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HeCloudOperation(AtomicOperations.REBOOT_INSTANCES)
@Component("rebootHeCloudInstancesDescriptionValidator")
class RebootHeCloudInstancesDescriptionValidator extends DescriptionValidator<RebootHeCloudInstancesDescription> {
  @Override
  void validate(List priorDescriptions, RebootHeCloudInstancesDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "RebootHeCloudInstancesDescription.region.empty"
    }

    if (!description.instanceIds) {
      errors.rejectValue "instanceIds", "RebootHeCloudInstancesDescription.instanceIds.empty"
    }
  }
}
