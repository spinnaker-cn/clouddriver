package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.CtyunDeployDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.CloneCtyunServerGroupAtomicOperation
import org.springframework.stereotype.Component

@CtyunOperation(AtomicOperations.CLONE_SERVER_GROUP)
@Component("cloneCtyunServerGroupDescription")
class CloneCtyunServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {

  AtomicOperation convertOperation(Map input) {
    new CloneCtyunServerGroupAtomicOperation(convertDescription(input))
  }

  CtyunDeployDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, CtyunDeployDescription)
  }
}
