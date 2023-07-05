package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.TerminateCtyunInstancesDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@CtyunOperation(AtomicOperations.TERMINATE_INSTANCES)
@Component("terminateCtyunInstancesDescriptionValidator")
class TerminateCtyunInstancesDescriptionValidator extends DescriptionValidator<TerminateCtyunInstancesDescription> {
  @Override
  void validate(List priorDescriptions, TerminateCtyunInstancesDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "TerminateCtyunInstancesDescription.region.empty"
    }

    if (!description.instanceIds) {
      errors.rejectValue "instanceIds", "TerminateCtyunInstancesDescription.instanceIds.empty"
    }
  }
}
