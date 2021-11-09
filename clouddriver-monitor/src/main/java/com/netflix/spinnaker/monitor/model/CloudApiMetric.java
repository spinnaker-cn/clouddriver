package com.netflix.spinnaker.monitor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chen_muyi
 * @date 2021/11/8 10:27
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CloudApiMetric {
  private String name;
  private List<String> lables;
  private List<String> values;
  private Integer count;
  private String description;
}
