package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.ecloud.EcloudOperation;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.DeleteEcloudLoadBalancerDescription;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.ops.DeleteEcloudLoadBalancerAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-11
 */
@EcloudOperation(AtomicOperations.DELETE_LOAD_BALANCER)
@Component("deleteEcloudLoadBalancerDescription")
public class DeleteEcloudLoadBalancerAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {
  public AtomicOperation convertOperation(Map input) {
    return new DeleteEcloudLoadBalancerAtomicOperation(convertDescription(input));
  }

  public DeleteEcloudLoadBalancerDescription convertDescription(Map input) {
    return EcloudAtomicOperationConverterHelper.convertDescription(
        input, this, DeleteEcloudLoadBalancerDescription.class);
  }
}
