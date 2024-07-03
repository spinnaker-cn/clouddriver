package com.netflix.spinnaker.clouddriver.ecloud.deploy.validators;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/4/12 @Description
 */
@EcloudOperation(AtomicOperations.DISABLE_SERVER_GROUP)
@Component("disableEcloudServerGroupDescriptionValidator")
public class DisableEcloudServerGroupDescriptionValidator
    extends ControlEcloudServerGroupDescriptionValidator {}
