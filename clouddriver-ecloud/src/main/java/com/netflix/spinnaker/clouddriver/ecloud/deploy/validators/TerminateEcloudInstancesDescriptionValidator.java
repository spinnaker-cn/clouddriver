package com.netflix.spinnaker.clouddriver.ecloud.deploy.validators;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/4/12 @Description
 */
@EcloudOperation(AtomicOperations.TERMINATE_INSTANCES)
@Component("stopEcloudInstancesDescriptionValidator")
public class TerminateEcloudInstancesDescriptionValidator
    extends ControlEcloudServerGroupInstancesDescriptionValidator {}