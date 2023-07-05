package com.netflix.spinnaker.clouddriver.ctyun.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.ctyun.CtyunOperation
import com.netflix.spinnaker.clouddriver.ctyun.deploy.description.DeleteCtyunSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.ctyun.deploy.ops.DeleteCtyunSecurityGroupAtomicOperation
import org.springframework.stereotype.Component


@CtyunOperation(AtomicOperations.DELETE_SECURITY_GROUP)
@Component("deleteCtyunSecurityGroupDescription")
class DeleteCtyunSecurityGroupAtomicOperationConverter  extends AbstractAtomicOperationsCredentialsSupport{
  AtomicOperation convertOperation(Map input) {
    new DeleteCtyunSecurityGroupAtomicOperation(convertDescription(input))
  }

  DeleteCtyunSecurityGroupDescription convertDescription(Map input) {
    CtyunAtomicOperationConverterHelper.convertDescription(input, this, DeleteCtyunSecurityGroupDescription);
  }
}
