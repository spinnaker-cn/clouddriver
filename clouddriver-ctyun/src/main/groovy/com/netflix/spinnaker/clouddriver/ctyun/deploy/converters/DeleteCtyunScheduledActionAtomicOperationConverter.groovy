package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunScheduledActionDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.DeleteCtyunScheduledActionAtomicOperation
import org.springframework.stereotype.Component

@Component("deleteCtyunScheduledActionDescription")
class DeleteCtyunScheduledActionAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  AtomicOperation convertOperation(Map input) {
    new DeleteCtyunScheduledActionAtomicOperation(convertDescription(input))
  }

  @Override
  DeleteCtyunScheduledActionDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, DeleteCtyunScheduledActionDescription)
  }
}
