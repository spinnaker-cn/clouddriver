package com.netflix.spinnaker.clouddriver.ctyun.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunSecurityGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@CtyunOperation(AtomicOperations.DELETE_SECURITY_GROUP)
@Component("deleteCtyunSecurityGroupDescriptionValidator")
class DeleteCtyunSecurityGroupDescriptionValidator extends DescriptionValidator<DeleteCtyunSecurityGroupDescription> {

  @Override
  void validate(List priorDescriptions, DeleteCtyunSecurityGroupDescription description, Errors errors) {
    if (!description.securityGroupId) {
      errors.rejectValue "securityGroupId", "DeleteCtyunSecurityGroupDescription.securityGroupId.empty"
    }

    if (!description.accountName) {
      errors.rejectValue "accountName", "DeleteCtyunSecurityGroupDescription.accountName.empty"
    }

    if (!description.region) {
      errors.rejectValue "region", "DeleteCtyunSecurityGroupDescription.region.empty"
    }
  }
}
