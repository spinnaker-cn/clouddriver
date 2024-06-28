package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.RemoveEcloudServerGroupInstancesDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.RemoveEcloudServerGroupInstancesAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/4/11 @Description
 */
@EcloudOperation(AtomicOperations.TERMINATE_INSTANCE_AND_DECREMENT)
@Component("removeEcloudServerGroupInstancesDescription")
public class RemoveEcloudServerGroupInstancesAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  @Nullable
  @Override
  public RemoveEcloudServerGroupInstancesAtomicOperation convertOperation(Map input) {
    return new RemoveEcloudServerGroupInstancesAtomicOperation(convertDescription(input));
  }

  @Override
  public RemoveEcloudServerGroupInstancesDescription convertDescription(Map input) {
    return EcloudDescriptionConverterHelper.convertDescription(
        input, this, RemoveEcloudServerGroupInstancesDescription.class);
  }
}
