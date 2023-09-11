package com.netflix.spinnaker.clouddriver.ctyun.exception;

import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum;
import com.netflix.spinnaker.monitor.enums.CloudTypeEnum;
import com.netflix.spinnaker.monitor.model.BaseMsgCode;
import com.netflix.spinnaker.monitor.model.MonitorModel;
import com.netflix.spinnaker.monitor.util.MonitorUtils;
import java.util.UUID;

/**
 * @author chen_muyi
 * @date 2021/10/19 14:19
 */
public class ExceptionUtils {
  private ExceptionUtils() {}

  public static void registerMetric(Exception e, AlarmLevelEnum alarmLevel) {
    MonitorModel monitorModel = new MonitorModel();
    monitorModel.setCode(alarmLevel);
    monitorModel.setCloudType(CloudTypeEnum.CTYUN);
    if (e instanceof CtyunOperationException) {
      monitorModel.setMessage(e.getMessage());
    } else {
      monitorModel.setApiCode(String.valueOf(BaseMsgCode.OTHER_ERROR.getCode()));
      monitorModel.setMessage(e.getMessage());
    }
    monitorModel.setLogId(UUID.randomUUID().toString().replace("-", ""));
    MonitorUtils.registerMetric(monitorModel, e);
  }

  public static void registerMetric(Exception e, AlarmLevelEnum alarmLevel, String code) {
    MonitorModel monitorModel = new MonitorModel();
    monitorModel.setCode(alarmLevel);
    monitorModel.setCloudType(CloudTypeEnum.CTYUN);
    monitorModel.setApiCode(code);
    monitorModel.setLogId(UUID.randomUUID().toString().replace("-", ""));
    MonitorUtils.registerMetric(monitorModel, e);
  }

  public static void registerMetric(Exception e, AlarmLevelEnum alarmLevel, Class<?> clazz) {
    MonitorModel monitorModel = new MonitorModel();
    monitorModel.setCode(alarmLevel);
    monitorModel.setCloudType(CloudTypeEnum.CTYUN);
    monitorModel.setApiCode(clazz.getSimpleName());
    monitorModel.setLogId(UUID.randomUUID().toString().replace("-", ""));
    MonitorUtils.registerMetric(monitorModel, e);
  }
}