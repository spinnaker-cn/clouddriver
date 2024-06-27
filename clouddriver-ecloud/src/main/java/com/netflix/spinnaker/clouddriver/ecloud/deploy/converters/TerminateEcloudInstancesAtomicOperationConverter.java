package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.TerminateEcloudInstancesDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.TerminateEcloudInstancesAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/4/11 @Description
 */
@EcloudOperation(AtomicOperations.TERMINATE_INSTANCES)
@Component("terminateEcloudInstancesDescription")
public class TerminateEcloudInstancesAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  @Nullable
  @Override
  public TerminateEcloudInstancesAtomicOperation convertOperation(Map input) {
    return new TerminateEcloudInstancesAtomicOperation(convertDescription(input));
  }

  @Override
  public TerminateEcloudInstancesDescription convertDescription(Map input) {
    return EcloudDescriptionConverterHelper.convertDescription(
        input, this, TerminateEcloudInstancesDescription.class);
  }
}
