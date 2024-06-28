package com.netflix.spinnaker.clouddriver.ecloud.deploy.validators;

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.UpsertEcloudLoadBalancerDescription;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-11
 */
@Slf4j
@EcloudOperation(AtomicOperations.UPSERT_LOAD_BALANCER)
@Component("upsertEcloudLoadBalancerDescriptionValidator")
public class UpsertEcloudLoadBalancerDescriptionValidator
    extends DescriptionValidator<UpsertEcloudLoadBalancerDescription> {
  @Override
  public void validate(
      List priorDescriptions, UpsertEcloudLoadBalancerDescription description, Errors errors) {
    if (description.getApplication() == null || description.getApplication().isEmpty()) {
      errors.rejectValue("application", "UpsertEcloudLoadBalancerDescription.application.empty");
    }

    if (description.getAccountName() == null || description.getAccountName().isEmpty()) {
      errors.rejectValue("accountName", "UpsertEcloudLoadBalancerDescription.accountName.empty");
    }

    if (description.getLoadBalancerName() == null || description.getLoadBalancerName().isEmpty()) {
      errors.rejectValue(
          "loadBalancerName", "UpsertEcloudLoadBalancerDescription.loadBalancerName.empty");
    }

    if (description.getRegion() == null || description.getRegion().isEmpty()) {
      errors.rejectValue("region", "UpsertEcloudLoadBalancerDescription.region.empty");
    }

    if (description.getLoadBalancerType() == null || description.getLoadBalancerType().isEmpty()) {
      errors.rejectValue(
          "loadBalancerType", "UpsertEcloudLoadBalancerDescription.loadBalancerType.empty");
      // OPEN check
    }

    // listener check

    // rule check
  }
}
