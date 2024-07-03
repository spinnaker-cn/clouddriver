package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.RebootEcloudInstancesDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.RebootEcloudInstancesAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/4/11 @Description
 */
@EcloudOperation(AtomicOperations.REBOOT_INSTANCES)
@Component("rebootEcloudInstancesDescription")
public class RebootEcloudInstancesAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  @Nullable
  @Override
  public RebootEcloudInstancesAtomicOperation convertOperation(Map input) {
    return new RebootEcloudInstancesAtomicOperation(convertDescription(input));
  }

  @Override
  public RebootEcloudInstancesDescription convertDescription(Map input) {
    return EcloudDescriptionConverterHelper.convertDescription(
        input, this, RebootEcloudInstancesDescription.class);
  }
}
