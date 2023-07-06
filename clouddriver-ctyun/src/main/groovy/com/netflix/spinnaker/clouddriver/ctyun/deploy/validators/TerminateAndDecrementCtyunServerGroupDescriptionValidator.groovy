package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.TerminateAndDecrementCtyunServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors


@CtyunOperation(AtomicOperations.TERMINATE_INSTANCE_AND_DECREMENT)
@Component("terminateAndDecrementCtyunServerGroupDescriptionValidator")
class TerminateAndDecrementCtyunServerGroupDescriptionValidator extends DescriptionValidator<TerminateAndDecrementCtyunServerGroupDescription> {
  @Override
  void validate(List priorDescriptions, TerminateAndDecrementCtyunServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "TerminateAndDecrementCtyunServerGroupDescription.region.empty"
    }

    if (!description.instance) {
      errors.rejectValue "instance", "TerminateAndDecrementCtyunServerGroupDescription.instance.empty"
    }
  }
}
