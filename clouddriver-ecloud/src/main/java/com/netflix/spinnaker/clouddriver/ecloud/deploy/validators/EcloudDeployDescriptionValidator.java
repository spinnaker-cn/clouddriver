package com.netflix.spinnaker.clouddriver.ecloud.deploy.validators;

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.EcloudDeployDescription;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@EcloudOperation(AtomicOperations.CREATE_SERVER_GROUP)
@Component("ecloudDeployDescriptionValidator")
public class EcloudDeployDescriptionValidator
    extends DescriptionValidator<EcloudDeployDescription> {
  @Override
  public void validate(List priorDescriptions, EcloudDeployDescription description, Errors errors) {

    if (description.getApplication() == null || description.getApplication().isEmpty()) {
      errors.rejectValue("application", "ecloudDeployDescription.application.empty");
    }

    if (description.getImageId() == null || description.getImageId().isEmpty()) {
      errors.rejectValue("imageId", "ecloudDeployDescription.imageId.empty");
    }

    if (description.getInstanceTypeRelas() == null
        || description.getInstanceTypeRelas().isEmpty()) {
      errors.rejectValue("instanceType", "ecloudDeployDescription.instanceType.empty");
    }

    if (description.getSubnets() == null || description.getSubnets().isEmpty()) {
      errors.rejectValue("subnets", "ecloudDeployDescription.subnets.not.supplied");
    }

    if (description.getTerminationPolicy() == null) {
      errors.rejectValue(
          "terminationPolicies", "ecloudDeployDescription.terminationPolicies.not.supplied");
    }

    if (description.getMaxSize() == null) {
      errors.rejectValue("maxSize", "ecloudDeployDescription.maxSize.empty");
    }

    if (description.getMinSize() == null) {
      errors.rejectValue("minSize", "ecloudDeployDescription.minSize.empty");
    }

    if (description.getDesiredCapacity() == null) {
      errors.rejectValue("desiredCapacity", "ecloudDeployDescription.desiredCapacity.empty");
    }

    if (description.getForwardLoadBalancers() != null
        && description.getForwardLoadBalancers().size() > 0) {
      Set<String> existedZones = new HashSet<>();
      for (EcloudDeployDescription.SubnetRela subnet : description.getSubnets()) {
        if (existedZones.contains(subnet.getZone())) {
          errors.rejectValue("subnets", "ecloudDeployDescription.subnet.zone.duplicated");
        } else {
          existedZones.add(subnet.getZone());
        }
      }
    }
  }
}
