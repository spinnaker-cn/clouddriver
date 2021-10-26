package com.netflix.spinnaker.monitor.enums;

import lombok.Getter;

/**
 * @author chen_muyi
 * @date 2021/10/18 15:31
 */
public enum AlarmLevelEnum {
  LEVEL_1(001), // always
  LEVEL_2(002),
  LEVEL_3(003),
  LEVEL_4(004);
  @Getter private Integer code;

  AlarmLevelEnum(Integer code) {
    this.code = code;
  }
}
