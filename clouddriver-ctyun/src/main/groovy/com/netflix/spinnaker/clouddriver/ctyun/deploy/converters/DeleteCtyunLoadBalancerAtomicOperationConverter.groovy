package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.DeleteCtyunLoadBalancerAtomicOperation
import org.springframework.stereotype.Component


@CtyunOperation(AtomicOperations.DELETE_LOAD_BALANCER)
@Component("deleteCtyunLoadBalancerDescription")
class DeleteCtyunLoadBalancerAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport{
  AtomicOperation convertOperation(Map input) {
    new DeleteCtyunLoadBalancerAtomicOperation(convertDescription(input))
  }

  DeleteCtyunLoadBalancerDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, DeleteCtyunLoadBalancerDescription)
  }
}
