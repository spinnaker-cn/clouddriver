package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunLoadBalancerDescription
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@Slf4j
@CtyunOperation(AtomicOperations.UPSERT_LOAD_BALANCER)
@Component("upsertCtyunLoadBalancerDescriptionValidator")
class UpsertCtyunLoadBalancerDescriptionValidator extends DescriptionValidator<UpsertCtyunLoadBalancerDescription> {
  @Override
  void validate(List priorDescriptions, UpsertCtyunLoadBalancerDescription description, Errors errors) {
    log.info("Enter ctyun validate ${description.properties}")
    if (!description.application) {
      errors.rejectValue "application", "UpsertCtyunLoadBalancerDescription.application.empty"
    }

    if (!description.accountName) {
      errors.rejectValue "accountName", "UpsertCtyunLoadBalancerDescription.accountName.empty"
    }

    if (!description.loadBalancerName) {
      errors.rejectValue "loadBalancerName", "UpsertCtyunLoadBalancerDescription.loadBalancerName.empty"
    }

    if (!description.region) {
      errors.rejectValue "region", "UpsertCtyunLoadBalancerDescription.region.empty"
    }

    if (!description.loadBalancerType) {
      errors.rejectValue "loadBalancerType", "UpsertCtyunLoadBalancerDescription.loadBalancerType.empty"
      //OPEN check
    }

    //listener check

    //rule check
  }
}
