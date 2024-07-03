package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.ResizeEcloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.ResizeEcloudServerGroupAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import org.springframework.stereotype.Component;

@EcloudOperation(AtomicOperations.RESIZE_SERVER_GROUP)
@Component("resizeEcloudServerGroupDescription")
public class ResizeEcloudServerGroupAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  @Override
  public AtomicOperation convertOperation(Map input) {
    return new ResizeEcloudServerGroupAtomicOperation(convertDescription(input));
  }

  @Override
  public ResizeEcloudServerGroupDescription convertDescription(Map input) {
    return EcloudDescriptionConverterHelper.convertDescription(
        input, this, ResizeEcloudServerGroupDescription.class);
  }
}
