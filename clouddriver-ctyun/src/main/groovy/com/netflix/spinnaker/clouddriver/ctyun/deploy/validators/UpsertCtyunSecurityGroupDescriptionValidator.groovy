package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunSecurityGroupDescription
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@Slf4j
@CtyunOperation(AtomicOperations.UPSERT_SECURITY_GROUP)
@Component("upsertCtyunSecurityGroupDescriptionValidator")
class UpsertCtyunSecurityGroupDescriptionValidator extends DescriptionValidator<UpsertCtyunSecurityGroupDescription> {

  @Override
  void validate(List priorDescriptions, UpsertCtyunSecurityGroupDescription description, Errors errors) {
    log.info("Validate ctyun security group description ${description}")
    if (!description.securityGroupName) {
      errors.rejectValue "securityGroupName", "UpsertCtyunSecurityGroupDescription.securityGroupName.empty"
    }

    if (!description.accountName) {
      errors.rejectValue "accountName", "UpsertCtyunSecurityGroupDescription.accountName.empty"
    }

    if (!description.region) {
      errors.rejectValue "region", "UpsertCtyunSecurityGroupDescription.region.empty"
    }
  }
}
