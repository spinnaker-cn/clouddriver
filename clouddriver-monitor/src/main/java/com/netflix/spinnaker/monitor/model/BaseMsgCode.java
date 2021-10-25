package com.netflix.spinnaker.monitor.model;

import lombok.Getter;

/**
 * @author chen_muyi
 * @date 2021/5/6 9:20
 */
public enum BaseMsgCode implements MsgCode {
  OTHER_ERROR(999, "other error");
  ;

  BaseMsgCode(Integer code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  @Getter private Integer code;
  @Getter private String msg;

  @Override
  public Integer code() {
    return code;
  }

  @Override
  public String msg() {
    return msg;
  }
}
