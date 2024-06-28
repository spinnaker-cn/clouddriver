package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DisableEcloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.DisableEcloudServerGroupAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/4/11 @Description
 */
@EcloudOperation(AtomicOperations.DISABLE_SERVER_GROUP)
@Component("disableEcloudServerGroupDescription")
public class DisableEcloudServerGroupAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  @Nullable
  @Override
  public DisableEcloudServerGroupAtomicOperation convertOperation(Map input) {
    return new DisableEcloudServerGroupAtomicOperation(convertDescription(input));
  }

  @Override
  public DisableEcloudServerGroupDescription convertDescription(Map input) {
    return EcloudDescriptionConverterHelper.convertDescription(
        input, this, DisableEcloudServerGroupDescription.class);
  }
}
