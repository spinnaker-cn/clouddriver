package com.netflix.spinnaker.clouddriver.ctyun.exception;

import com.netflix.spinnaker.monitor.model.MsgCode;
import lombok.Getter;

/**
 * @author chen_muyi
 * @date 2021/12/2 14:14
 */
public enum CtyunCodeEnum implements MsgCode {
  ;
  @Getter private int code;
  @Getter private String message;

  @Override
  public Integer code() {
    return code;
  }

  @Override
  public String msg() {
    return message;
  }
}
