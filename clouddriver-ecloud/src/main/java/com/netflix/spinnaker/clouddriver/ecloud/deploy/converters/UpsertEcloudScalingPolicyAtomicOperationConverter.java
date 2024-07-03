package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.UpsertEcloudScalingPolicyDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.UpsertEcloudScalingPolicyAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import org.springframework.stereotype.Component;

@EcloudOperation(AtomicOperations.UPSERT_SCALING_POLICY)
@Component("upsertEcloudScalingPolicyDescription")
public class UpsertEcloudScalingPolicyAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  @Override
  public AtomicOperation convertOperation(Map input) {
    return new UpsertEcloudScalingPolicyAtomicOperation(convertDescription(input));
  }

  @Override
  public UpsertEcloudScalingPolicyDescription convertDescription(Map input) {
    return EcloudDescriptionConverterHelper.convertDescription(
        input, this, UpsertEcloudScalingPolicyDescription.class);
  }
}
