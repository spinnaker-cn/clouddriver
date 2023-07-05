package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

import cn.ctyun.ctapi.scaling.ruleupdate.TriggerInfo


class UpsertCtyunScheduledActionDescription extends AbstractCtyunCredentialsDescription {
  String regionID
  String region
  Integer groupID
  String name
  Integer type
  Integer ruleType
  Integer operateUnit
  Integer operateCount
  Integer action
  Integer cycle
  List<Integer> day
  String effectiveFrom
  String effectiveTill
  String executionTime
  Integer cooldown
  //该字段为了接收警告策略修改信息
  TriggerInfo triggerInfo
  Integer status//1启动2停用

  Integer ruleID
  String accountName
  OperationType operationType
  String serverGroupName





  enum OperationType {
    CREATE, MODIFY
  }
}
