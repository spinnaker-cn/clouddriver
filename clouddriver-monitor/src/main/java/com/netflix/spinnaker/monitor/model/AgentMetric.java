package com.netflix.spinnaker.monitor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author chen_muyi
 * @date 2022/7/15 15:36
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AgentMetric {
  private String metricName;
  private String agentName;
  private List<String> lables;
  private List<String> values;
  private Long count;
  private String description;
}
