package com.netflix.spinnaker.clouddriver.ecloud.deploy.validators;

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DeleteEcloudSecurityGroupDescription;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import io.micrometer.core.instrument.util.StringUtils;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@EcloudOperation(AtomicOperations.DELETE_SECURITY_GROUP)
@Component("deleteEcloudSecurityGroupDescriptionValidator")
public class DeleteEcloudSecurityGroupDescriptionValidator
    extends DescriptionValidator<DeleteEcloudSecurityGroupDescription> {

  @Override
  public void validate(
      List priorDescriptions, DeleteEcloudSecurityGroupDescription description, Errors errors) {
    if (StringUtils.isBlank(description.getSecurityGroupId())) {
      errors.rejectValue(
          "securityGroupId", "DeleteEcloudSecurityGroupDescription.securityGroupId.empty");
    }

    if (StringUtils.isBlank(description.getAccount())) {
      errors.rejectValue("accountName", "DeleteEcloudSecurityGroupDescription.accountName.empty");
    }

    if (StringUtils.isBlank(description.getRegion())) {
      errors.rejectValue("region", "DeleteEcloudSecurityGroupDescription.region.empty");
    }
  }
}
