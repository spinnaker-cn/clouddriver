package com.netflix.spinnaker.clouddriver.ecloud.exception;

import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum;
import com.netflix.spinnaker.monitor.enums.CloudTypeEnum;
import com.netflix.spinnaker.monitor.model.BaseMsgCode;
import com.netflix.spinnaker.monitor.model.MonitorModel;
import com.netflix.spinnaker.monitor.util.MonitorUtils;
import java.util.UUID;

public class ExceptionUtils {
  private ExceptionUtils() {}

  public static void registerMetric(Exception e, AlarmLevelEnum alarmLevel) {
    MonitorModel monitorModel = new MonitorModel();
    monitorModel.setCode(alarmLevel);
    monitorModel.setCloudType(CloudTypeEnum.ECLOUD);
    if (e instanceof EcloudException) {
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
    monitorModel.setCloudType(CloudTypeEnum.ECLOUD);
    monitorModel.setApiCode(code);
    monitorModel.setLogId(UUID.randomUUID().toString().replace("-", ""));
    MonitorUtils.registerMetric(monitorModel, e);
  }

  public static void registerMetric(Exception e, AlarmLevelEnum alarmLevel, Class<?> clazz) {
    MonitorModel monitorModel = new MonitorModel();
    monitorModel.setCode(alarmLevel);
    monitorModel.setCloudType(CloudTypeEnum.ECLOUD);
    monitorModel.setApiCode(clazz.getSimpleName());
    monitorModel.setLogId(UUID.randomUUID().toString().replace("-", ""));
    MonitorUtils.registerMetric(monitorModel, e);
  }
}
