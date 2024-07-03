package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.CloneEcloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.CloneEcloudServerGroupAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import org.springframework.stereotype.Component;

@EcloudOperation(AtomicOperations.CLONE_SERVER_GROUP)
@Component("cloneEcloudServerGroupDescription")
public class CloneEcloudServerGroupAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  @Override
  public AtomicOperation convertOperation(Map input) {
    return new CloneEcloudServerGroupAtomicOperation(convertDescription(input));
  }

  @Override
  public CloneEcloudServerGroupDescription convertDescription(Map input) {
    return EcloudDescriptionConverterHelper.convertDescription(
        input, this, CloneEcloudServerGroupDescription.class);
  }
}
