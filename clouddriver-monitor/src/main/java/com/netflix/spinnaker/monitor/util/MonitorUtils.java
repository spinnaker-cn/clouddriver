package com.netflix.spinnaker.monitor.util;

import com.google.gson.Gson;
import com.netflix.spinnaker.monitor.model.MonitorModel;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author chen_muyi
 * @date 2021/9/24 15:29
 */
@Slf4j
@Component
public class MonitorUtils {
  private static final Gson GSON = new Gson();

  @Autowired private MeterRegistry meterRegistry;

  private static MeterRegistry meterRegistryStatic;

  @PostConstruct
  public void init() {
    meterRegistryStatic = meterRegistry;
  }

  private static final String CODE = "code";
  private static final String LOG_ID = "logId";
  private static final String CLOUD_TYPE = "cloudType";
  private static final String API_CODE = "apiCode";
  private static final String MESSAGE = "message";
  private static final String ERROR = "error";

  public static void registerMetric(MonitorModel model, Throwable e) {
    meterRegistryStatic
        .counter(
            "invoke api error",
            new String[] {
              CODE,
              String.valueOf(model.getCode()),
              CLOUD_TYPE,
              model.getCloudType().name(),
              API_CODE,
              String.valueOf(model.getApiCode())
            })
        .increment();
    Map<String, Object> map = new HashMap<>();
    map.put(LOG_ID, model.getLogId());
    map.put(ERROR, e);
    log.error(GSON.toJson(map));
  }
}
