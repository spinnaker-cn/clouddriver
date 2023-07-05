package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.ResizeCtyunServerGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.ResizeCtyunServerGroupAtomicOperation
import org.springframework.stereotype.Component

@CtyunOperation(AtomicOperations.RESIZE_SERVER_GROUP)
@Component("resizeCtyunServerGroupDescription")
class ResizeCtyunServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  AtomicOperation convertOperation(Map input) {
    new ResizeCtyunServerGroupAtomicOperation(convertDescription(input))
  }

  ResizeCtyunServerGroupDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, ResizeCtyunServerGroupDescription)
  }
}
