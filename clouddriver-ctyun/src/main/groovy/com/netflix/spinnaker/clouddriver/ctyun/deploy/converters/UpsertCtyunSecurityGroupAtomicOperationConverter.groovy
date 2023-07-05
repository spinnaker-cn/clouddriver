package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.UpsertCtyunSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.UpsertCtyunSecurityGroupAtomicOperation
import org.springframework.stereotype.Component


@CtyunOperation(AtomicOperations.UPSERT_SECURITY_GROUP)
@Component("upsertCtyunSecurityGroupDescription")
class UpsertCtyunSecurityGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport{
  AtomicOperation convertOperation(Map input) {
    new UpsertCtyunSecurityGroupAtomicOperation(convertDescription(input))
  }

  UpsertCtyunSecurityGroupDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, UpsertCtyunSecurityGroupDescription)
  }
}
