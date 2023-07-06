package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunScheduledActionDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.UpsertCtyunScheduledActionAtomicOperation
import org.springframework.stereotype.Component

@Component("upsertCtyunScheduledActionsDescription")
class UpsertCtyunScheduledActionAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  AtomicOperation convertOperation(Map input) {
    new UpsertCtyunScheduledActionAtomicOperation(convertDescription(input))
  }

  @Override
  UpsertCtyunScheduledActionDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, UpsertCtyunScheduledActionDescription)
  }
}
