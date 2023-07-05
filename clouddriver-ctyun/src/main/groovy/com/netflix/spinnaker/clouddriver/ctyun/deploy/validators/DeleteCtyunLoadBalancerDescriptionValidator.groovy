package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunLoadBalancerDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@CtyunOperation(AtomicOperations.DELETE_LOAD_BALANCER)
@Component("deleteCtyunLoadBalancerDescriptionValidator")
class DeleteCtyunLoadBalancerDescriptionValidator extends DescriptionValidator<DeleteCtyunLoadBalancerDescription> {
  @Override
  void validate(List priorDescriptions, DeleteCtyunLoadBalancerDescription description, Errors errors) {

    if (!description.application) {
      errors.rejectValue "application", "DeleteCtyunLoadBalancerDescription.application.empty"
    }

    if (!description.accountName) {
      errors.rejectValue "accountName", "DeleteCtyunLoadBalancerDescription.accountName.empty"
    }

    if (!description.region) {
      errors.rejectValue "region", "UpsertCtyunLoadBalancerDescription.region.empty"
    }

    if (!description.loadBalancerId) {
      errors.rejectValue "loadBalancerId", "DeleteCtyunLoadBalancerDescription.loadBalancerId.empty"
    }
  }
}
