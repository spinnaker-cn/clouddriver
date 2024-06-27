package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DeleteEcloudSecurityGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.DeleteEcloudSecurityGroupAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component("deleteEcloudSecurityGroupDescription")
@EcloudOperation(AtomicOperations.DELETE_SECURITY_GROUP)
public class DeleteEcloudSecurityGroupAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  @Override
  public AtomicOperation convertOperation(Map input) {
    return new DeleteEcloudSecurityGroupAtomicOperation(convertDescription(input));
  }

  @Override
  public DeleteEcloudSecurityGroupDescription convertDescription(Map input) {
    return EcloudAtomicOperationConverterHelper.convertDescription(
        input, this, DeleteEcloudSecurityGroupDescription.class);
  }
}
