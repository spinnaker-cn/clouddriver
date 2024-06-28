package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.UpsertEcloudLoadBalancerDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.UpsertEcloudLoadBalancerAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-17
 */
@EcloudOperation(AtomicOperations.UPSERT_LOAD_BALANCER)
@Component("upsertEcloudLoadBalancerDescription")
public class UpsertEcloudLoadBalancerAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {
  public AtomicOperation convertOperation(Map input) {
    return new UpsertEcloudLoadBalancerAtomicOperation(convertDescription(input));
  }

  public UpsertEcloudLoadBalancerDescription convertDescription(Map input) {
    return EcloudAtomicOperationConverterHelper.convertDescription(
        input, this, UpsertEcloudLoadBalancerDescription.class);
  }
}
