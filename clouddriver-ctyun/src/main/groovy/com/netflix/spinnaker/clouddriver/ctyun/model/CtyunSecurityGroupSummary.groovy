package com.netflix.spinnaker.clouddriver.ctyun.model

import com.netflix.spinnaker.clouddriver.model.SecurityGroupSummary
import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable

@Immutable
@EqualsAndHashCode(includes = ['id'], cache = true)
class CtyunSecurityGroupSummary implements SecurityGroupSummary{
  String name
  String id
  String vpcId
}
