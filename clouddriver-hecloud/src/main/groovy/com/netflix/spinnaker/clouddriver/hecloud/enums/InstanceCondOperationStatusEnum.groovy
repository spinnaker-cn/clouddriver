package com.netflix.spinnaker.clouddriver.hecloud.enums

/**
 * ECS 规格状态，是否售罄
 * @author 硝酸铜
 * @date 2022/5/11
 */
enum InstanceCondOperationStatusEnum {
  //正常商用
  NORMAL,

  //下线（即不显示）
  ABANDON,

  //售罄
  SELLOUT,

  //公测
  OBT,

  //公测售罄
  OBT_SELLOUT,

  //推荐(等同normal，也是商用)
  PROMOTION

}
