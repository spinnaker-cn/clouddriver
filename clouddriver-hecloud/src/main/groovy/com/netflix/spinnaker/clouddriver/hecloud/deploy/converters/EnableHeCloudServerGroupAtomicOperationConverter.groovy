package com.netflix.spinnaker.clouddriver.hecloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.hecloud.HeCloudOperation
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.EnableDisableHeCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.hecloud.deploy.ops.EnableHeCloudServerGroupAtomicOperation
import org.springframework.stereotype.Component

@HeCloudOperation(AtomicOperations.ENABLE_SERVER_GROUP)
@Component("enableHeCloudServerGroupDescription")
class EnableHeCloudServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  AtomicOperation convertOperation(Map input) {
    new EnableHeCloudServerGroupAtomicOperation(convertDescription(input))
  }

  EnableDisableHeCloudServerGroupDescription convertDescription(Map input) {
    HeCloudAtomicOperationConverterHelper.convertDescription(input, this, EnableDisableHeCloudServerGroupDescription)
  }
}
