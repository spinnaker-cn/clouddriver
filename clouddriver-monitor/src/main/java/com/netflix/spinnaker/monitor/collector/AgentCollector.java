package com.netflix.spinnaker.monitor.collector;

import com.netflix.spinnaker.monitor.model.AgentMetric;
import com.netflix.spinnaker.monitor.model.CloudApiMetric;
import com.netflix.spinnaker.monitor.util.MonitorUtils;
import io.prometheus.client.Collector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author chen_muyi
 * @date 2021/11/5 16:03
 */
@Component
public class AgentCollector extends Collector {

  @Override
  public List<MetricFamilySamples> collect() {
    Map<String, AgentMetric> agentMetricsMap = MonitorUtils.agentMetricsMap;
    List<MetricFamilySamples> collect = agentMetricsMap.values().stream().map(
      meter -> {
        String metricName = StringUtils.isEmpty(meter.getMetricName()) ? "agent_loadData_elapsed" : meter.getMetricName();
        MetricFamilySamples.Sample sample = new MetricFamilySamples.Sample(metricName, meter.getLables(), meter.getValues(), meter.getCount());
        List<MetricFamilySamples.Sample> samples = new ArrayList<>();
        samples.add(sample);
        return new MetricFamilySamples(metricName,
          Type.COUNTER,
          StringUtils.isEmpty(meter.getDescription()) ? "this is agent loadData elapsed metrics" : meter.getDescription(),
          samples);
      }
    ).collect(Collectors.toList());
    return collect;
  }
}
