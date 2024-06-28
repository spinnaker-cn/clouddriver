package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DestroyEcloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.DestroyEcloudServerGroupAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/4/11 @Description
 */
@EcloudOperation(AtomicOperations.DESTROY_SERVER_GROUP)
@Component("destroyEcloudServerGroupDecription")
public class DestroyEcloudServerGroupAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  @Nullable
  @Override
  public DestroyEcloudServerGroupAtomicOperation convertOperation(Map input) {
    return new DestroyEcloudServerGroupAtomicOperation(convertDescription(input));
  }

  @Override
  public DestroyEcloudServerGroupDescription convertDescription(Map input) {
    return EcloudDescriptionConverterHelper.convertDescription(
        input, this, DestroyEcloudServerGroupDescription.class);
  }
}
