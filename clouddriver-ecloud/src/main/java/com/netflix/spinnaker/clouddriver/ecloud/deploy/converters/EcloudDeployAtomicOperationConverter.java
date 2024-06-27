package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.deploy.DeployAtomicOperation;
import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.EcloudDeployDescription;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author xu.dangling
 * @date 2024/5/13 @Description
 */
@EcloudOperation(AtomicOperations.CREATE_SERVER_GROUP)
@Component("ecloudDeployDescription")
public class EcloudDeployAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {
  @Nullable
  @Override
  public AtomicOperation convertOperation(Map input) {
    return new DeployAtomicOperation(convertDescription(input));
  }

  @Override
  public EcloudDeployDescription convertDescription(Map input) {
    return EcloudDescriptionConverterHelper.convertDescription(
        input, this, EcloudDeployDescription.class);
  }
}
