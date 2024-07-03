package com.netflix.spinnaker.clouddriver.ecloud.deploy.validators;

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.ControlEcloudServerGroupDescription;
import java.util.List;
import org.springframework.validation.Errors;

class ControlEcloudServerGroupDescriptionValidator
    extends DescriptionValidator<ControlEcloudServerGroupDescription> {

  @Override
  public void validate(
      List priorDescriptions, ControlEcloudServerGroupDescription description, Errors errors) {
    if (description.getRegion() == null || description.getRegion().isEmpty()) {
      errors.rejectValue("region", "ControlEcloudServerGroupDescription.region.empty");
    }

    if (description.getServerGroupName() == null || description.getServerGroupName().isEmpty()) {
      errors.rejectValue(
          "serverGroupName", "ControlEcloudServerGroupDescription.serverGroupName.empty");
    }
  }
}
