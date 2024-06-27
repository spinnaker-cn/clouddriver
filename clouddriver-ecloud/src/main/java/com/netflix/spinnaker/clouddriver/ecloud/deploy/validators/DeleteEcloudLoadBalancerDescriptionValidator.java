package com.netflix.spinnaker.clouddriver.ecloud.deploy.validators;

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DeleteEcloudLoadBalancerDescription;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-11
 */
@EcloudOperation(AtomicOperations.DELETE_LOAD_BALANCER)
@Component("deleteEcloudLoadBalancerDescriptionValidator")
public class DeleteEcloudLoadBalancerDescriptionValidator
    extends DescriptionValidator<DeleteEcloudLoadBalancerDescription> {
  @Override
  public void validate(
      List priorDescriptions, DeleteEcloudLoadBalancerDescription description, Errors errors) {

    if (description.getApplication() == null || description.getApplication().isEmpty()) {
      errors.rejectValue("application", "DeleteEcloudLoadBalancerDescription.application.empty");
    }

    if (description.getAccountName() == null || description.getAccountName().isEmpty()) {
      errors.rejectValue("accountName", "DeleteEcloudLoadBalancerDescription.accountName.empty");
    }

    if (description.getRegion() == null || description.getRegion().isEmpty()) {
      errors.rejectValue("region", "DeleteEcloudLoadBalancerDescription.region.empty");
    }

    if (description.getLoadBalancerId() == null || description.getLoadBalancerId().isEmpty()) {
      errors.rejectValue(
          "loadBalancerId", "DeleteEcloudLoadBalancerDescription.loadBalancerId.empty");
    }
  }
}
