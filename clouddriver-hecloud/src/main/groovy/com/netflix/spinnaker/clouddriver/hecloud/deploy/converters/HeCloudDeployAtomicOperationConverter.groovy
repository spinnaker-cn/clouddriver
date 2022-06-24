package com.netflix.spinnaker.clouddriver.hecloud.deploy.converters

import com.netflix.spinnaker.clouddriver.deploy.DeployAtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.HeCloudDeployDescription
import org.springframework.stereotype.Component

@HeCloudOperation(AtomicOperations.CREATE_SERVER_GROUP)
@Component("heCloudDeployDescription")
class HeCloudDeployAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport{
  AtomicOperation convertOperation(Map input) {
    new DeployAtomicOperation(convertDescription(input))
  }

  HeCloudDeployDescription convertDescription(Map input) {
    HeCloudAtomicOperationConverterHelper.convertDescription(input, this, HeCloudDeployDescription)
  }
}
