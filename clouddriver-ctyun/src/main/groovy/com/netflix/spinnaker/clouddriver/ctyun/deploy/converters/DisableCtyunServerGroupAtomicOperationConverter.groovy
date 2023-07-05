package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.EnableDisableCtyunServerGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.DisableCtyunServerGroupAtomicOperation
import org.springframework.stereotype.Component

@CtyunOperation(AtomicOperations.DISABLE_SERVER_GROUP)
@Component("disableCtyunServerGroupDescription")
class DisableCtyunServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {

  AtomicOperation convertOperation(Map input) {
    new DisableCtyunServerGroupAtomicOperation(convertDescription(input))
  }

  EnableDisableCtyunServerGroupDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, EnableDisableCtyunServerGroupDescription)
  }
}
