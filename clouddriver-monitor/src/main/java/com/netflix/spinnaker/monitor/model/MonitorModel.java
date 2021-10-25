package com.netflix.spinnaker.monitor.model;

import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum;
import com.netflix.spinnaker.monitor.enums.CloudTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author chen_muyi
 * @date 2021/10/18 15:51
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MonitorModel {
  private String logId;
  private String monitorName;
  private AlarmLevelEnum code;
  private CloudTypeEnum cloudType;
  private String apiCode;
  private String message;
}
