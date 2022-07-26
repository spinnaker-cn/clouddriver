package com.netflix.spinnaker.clouddriver.hecloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.EnableDisableHeCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.hecloud.deploy.ops.DisableHeCloudServerGroupAtomicOperation
import org.springframework.stereotype.Component

@HeCloudOperation(AtomicOperations.DISABLE_SERVER_GROUP)
@Component("disableHeCloudServerGroupDescription")
class DisableHeCloudServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {

  AtomicOperation convertOperation(Map input) {
    new DisableHeCloudServerGroupAtomicOperation(convertDescription(input))
  }

  EnableDisableHeCloudServerGroupDescription convertDescription(Map input) {
    HeCloudAtomicOperationConverterHelper.convertDescription(input, this, EnableDisableHeCloudServerGroupDescription)
  }
}
