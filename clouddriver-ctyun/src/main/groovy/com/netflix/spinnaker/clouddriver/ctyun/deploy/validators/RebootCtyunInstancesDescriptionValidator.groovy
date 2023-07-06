package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.RebootCtyunInstancesDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@CtyunOperation(AtomicOperations.REBOOT_INSTANCES)
@Component("rebootCtyunInstancesDescriptionValidator")
class RebootCtyunInstancesDescriptionValidator extends DescriptionValidator<RebootCtyunInstancesDescription> {
  @Override
  void validate(List priorDescriptions, RebootCtyunInstancesDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "RebootCtyunInstancesDescription.region.empty"
    }

    if (!description.instanceIds) {
      errors.rejectValue "instanceIds", "RebootCtyunInstancesDescription.instanceIds.empty"
    }
  }
}
