package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.UpsertEcloudSecurityGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.UpsertEcloudSecurityGroupAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import org.springframework.stereotype.Component;

@EcloudOperation(AtomicOperations.UPSERT_SECURITY_GROUP)
@Component("upsertEcloudSecurityGroupDescription")
public class UpsertEcloudSecurityGroupAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {
  public AtomicOperation convertOperation(Map input) {
    return new UpsertEcloudSecurityGroupAtomicOperation(convertDescription(input));
  }

  public UpsertEcloudSecurityGroupDescription convertDescription(Map input) {
    return EcloudAtomicOperationConverterHelper.convertDescription(
        input, this, UpsertEcloudSecurityGroupDescription.class);
  }
}
