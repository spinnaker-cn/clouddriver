package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

import cn.ctyun.ctapi.scaling.rulecreatealarm.AlarmTriggerInfo

class UpsertCtyunAlarmActionDescription extends AbstractCtyunCredentialsDescription {

  String regionID
  Integer groupID
  String name
  Integer type
  Integer operateUnit //本参数表示操作的单位。取值范围： 1：个数。2：百分比。
  Integer operateCount  //执行次数
  Integer action //本参数表示执行动作。取值范围：1：增加。2：减少。 3：设置为N。
  Integer cycle
  List<Integer> day
  String effectiveFrom
  String effectiveTill
  String executionTime
  Integer cooldown //冷却时间，告警策略时必填，单位：秒
  AlarmTriggerInfo triggerObj //新建告警规则传规则内容

  Integer status//1启动2停用

  Integer ruleID
  String accountName
  OperationType operationType
  String serverGroupName
  enum OperationType {
    CREATE, MODIFY
  }
}
