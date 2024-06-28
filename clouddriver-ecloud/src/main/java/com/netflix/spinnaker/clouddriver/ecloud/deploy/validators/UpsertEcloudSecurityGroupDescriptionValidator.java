package com.netflix.spinnaker.clouddriver.ecloud.deploy.validators;

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.UpsertEcloudSecurityGroupDescription;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import io.micrometer.core.instrument.util.StringUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Slf4j
@EcloudOperation(AtomicOperations.UPSERT_SECURITY_GROUP)
@Component("upsertEcloudSecurityGroupDescriptionValidator")
public class UpsertEcloudSecurityGroupDescriptionValidator
    extends DescriptionValidator<UpsertEcloudSecurityGroupDescription> {

  @Override
  public void validate(
      List priorDescriptions, UpsertEcloudSecurityGroupDescription description, Errors errors) {
    log.info("Validate ecloud security group description {}", description);
    if (description.getSecurityGroupName() == null
        || description.getSecurityGroupName().isEmpty()) {
      errors.rejectValue(
          "securityGroupName", "UpsertEcloudSecurityGroupDescription.securityGroupName.empty");
    }

    if (StringUtils.isBlank(description.getAccount())) {
      errors.rejectValue("accountName", "UpsertEcloudSecurityGroupDescription.accountName.empty");
    }

    if (StringUtils.isBlank(description.getRegion())) {
      errors.rejectValue("region", "UpsertEcloudSecurityGroupDescription.region.empty");
    }
  }
}
