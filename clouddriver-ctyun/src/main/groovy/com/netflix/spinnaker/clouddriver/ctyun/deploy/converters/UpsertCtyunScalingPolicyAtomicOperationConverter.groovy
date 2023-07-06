package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunAlarmActionDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation

import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.UpsertCtyunScalingAlarmPolicyAtomicOperation
import org.springframework.stereotype.Component

@CtyunOperation(AtomicOperations.UPSERT_SCALING_POLICY)
@Component("upsertCtyunScalingPolicyDescription")
class UpsertCtyunScalingPolicyAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {

  AtomicOperation convertOperation(Map input) {
    new UpsertCtyunScalingAlarmPolicyAtomicOperation(convertDescription(input))
  }

  UpsertCtyunAlarmActionDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, UpsertCtyunAlarmActionDescription)
  }
}
