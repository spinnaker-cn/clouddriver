package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.EnableEcloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.EnableEcloudServerGroupAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/4/11 @Description
 */
@EcloudOperation(AtomicOperations.ENABLE_SERVER_GROUP)
@Component("enableEcloudServerGroupDescription")
public class EnableEcloudServerGroupAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  @Nullable
  @Override
  public EnableEcloudServerGroupAtomicOperation convertOperation(Map input) {
    return new EnableEcloudServerGroupAtomicOperation(convertDescription(input));
  }

  @Override
  public EnableEcloudServerGroupDescription convertDescription(Map input) {
    return EcloudDescriptionConverterHelper.convertDescription(
        input, this, EnableEcloudServerGroupDescription.class);
  }
}
