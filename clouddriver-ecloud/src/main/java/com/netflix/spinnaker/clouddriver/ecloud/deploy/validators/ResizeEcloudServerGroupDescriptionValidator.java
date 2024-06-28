package com.netflix.spinnaker.clouddriver.ecloud.deploy.validators;

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.ResizeEcloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@EcloudOperation(AtomicOperations.RESIZE_SERVER_GROUP)
@Component("resizeEcloudServerGroupDescriptionValidator")
public class ResizeEcloudServerGroupDescriptionValidator
    extends DescriptionValidator<ResizeEcloudServerGroupDescription> {

  @Override
  public void validate(
      List priorDescriptions, ResizeEcloudServerGroupDescription description, Errors errors) {
    if (description.getRegion() == null || description.getRegion().isEmpty()) {
      errors.rejectValue("region", "ControlEcloudServerGroupDescription.region.empty");
    }

    if (description.getServerGroupName() == null || description.getServerGroupName().isEmpty()) {
      errors.rejectValue(
          "serverGroupName", "ControlEcloudServerGroupDescription.serverGroupName.empty");
    }
  }
}
