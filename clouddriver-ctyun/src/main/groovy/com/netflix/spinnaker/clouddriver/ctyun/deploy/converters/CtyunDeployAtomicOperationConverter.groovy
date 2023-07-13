package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.deploy.DeployAtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.CtyunDeployDescription
import org.springframework.stereotype.Component

@CtyunOperation(AtomicOperations.CREATE_SERVER_GROUP)
@Component("ctyunDeployDescription")
class CtyunDeployAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport{
  AtomicOperation convertOperation(Map input) {
    new DeployAtomicOperation(convertDescription(input))
  }

  CtyunDeployDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, CtyunDeployDescription)
  }
}
