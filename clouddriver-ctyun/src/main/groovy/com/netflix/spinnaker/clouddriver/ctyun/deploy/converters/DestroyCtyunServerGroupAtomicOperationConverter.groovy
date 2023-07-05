package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DestroyCtyunServerGroupDescription
import org.springframework.stereotype.Component
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.DestroyCtyunServerGroupAtomicOperation

@CtyunOperation(AtomicOperations.DESTROY_SERVER_GROUP)
@Component("destroyCtyunServerGroupDescription")
class DestroyCtyunServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  AtomicOperation convertOperation(Map input) {
    new DestroyCtyunServerGroupAtomicOperation(convertDescription(input))
  }

  DestroyCtyunServerGroupDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, DestroyCtyunServerGroupDescription)
  }
}
