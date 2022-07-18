package com.netflix.spinnaker.monitor.util;

import com.google.gson.Gson;
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum;
import com.netflix.spinnaker.monitor.enums.CloudTypeEnum;
import com.netflix.spinnaker.monitor.model.AgentMetric;
import com.netflix.spinnaker.monitor.model.MonitorModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


//import io.micrometer.core.instrument.MeterRegistry;
import com.netflix.spinnaker.monitor.model.CloudApiMetric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/**
 * @author chen_muyi
 * @date 2021/9/24 15:29
 */
@Slf4j
@Component
public class MonitorUtils {
  public static Map<String, CloudApiMetric> metricsMap = new ConcurrentHashMap<>();
  public static Map<String, AgentMetric> agentMetricsMap = new ConcurrentHashMap<>();
  private static final Gson GSON = new Gson();

  private static final String CODE = "code";
  private static final String AGENT = "agent";
  private static final String LOG_ID = "logId";
  private static final String CLOUD_TYPE = "cloudType";
  private static final String API_CODE = "apiCode";
  private static final String MESSAGE = "message";
  private static final String ERROR = "error";

  public static void registerAgentElapsedMetric(String  metricName,String agentName, long elapsedMs) {
    String key = String.join(metricName, agentName);
    AgentMetric agentCache = agentMetricsMap.get(key);
    if (agentCache == null) {
      AgentMetric agentMetric = new AgentMetric();
      agentMetric.setMetricName(metricName);
      agentMetric.setAgentName(agentName);
      List<String> lables = new ArrayList<>();
      lables.add(AGENT);
      List<String> values = new ArrayList<>();
      values.add(agentMetric.getAgentName());
      agentMetric.setLables(lables);
      agentMetric.setValues(values);
      agentMetric.setCount(elapsedMs);
      agentMetricsMap.put(key, agentMetric);
    } else {
      agentCache.setCount(agentCache.getCount()+elapsedMs);
    }
  }

  public static void registerMetric(MonitorModel model, Throwable e) {
    String key = DigestUtils.md5DigestAsHex(String.join(",", model.getMonitorName(), model.getCode().name(), model.getCloudType().name(), model.getApiCode()).getBytes());
    CloudApiMetric cloudApiMetric = metricsMap.get(key);
    if (cloudApiMetric == null) {
      cloudApiMetric = new CloudApiMetric();
      metricsMap.put(key, cloudApiMetric);
      cloudApiMetric.setName(model.getMonitorName());
      List<String> lables = new ArrayList<>();
      lables.add(CODE);
      lables.add(CLOUD_TYPE);
      lables.add(API_CODE);
      List<String> values = new ArrayList<>();
      values.add(model.getCode().name());
      values.add(model.getCloudType().name());
      values.add(model.getApiCode());
      cloudApiMetric.setLables(lables);
      cloudApiMetric.setValues(values);
      cloudApiMetric.setCount(1);
    } else {
      cloudApiMetric.setCount(cloudApiMetric.getCount()+1);
    }
    Map<String, Object> map = new HashMap<>();
    map.put(LOG_ID, model.getLogId());
    map.put(ERROR, e);
    log.error(GSON.toJson(map));
  }
}
