package com.netflix.spinnaker.clouddriver.ecloud.deploy.validators;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/5/24 @Description
 */
@EcloudOperation(AtomicOperations.CLONE_SERVER_GROUP)
@Component("cloneEcloudServerGroupDescriptionValidator")
public class CloneEcloudServerGroupDescriptionValidator extends EcloudDeployDescriptionValidator {}
