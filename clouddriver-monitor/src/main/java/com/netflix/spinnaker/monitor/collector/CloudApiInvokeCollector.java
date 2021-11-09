package com.netflix.spinnaker.monitor.collector;

import com.netflix.spinnaker.monitor.model.CloudApiMetric;
import com.netflix.spinnaker.monitor.util.MonitorUtils;
import io.prometheus.client.Collector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author chen_muyi
 * @date 2021/11/5 16:03
 */
@Component
public class CloudApiInvokeCollector extends Collector {

  @Override
  public List<MetricFamilySamples> collect() {
    Map<String, CloudApiMetric> metricsMap = MonitorUtils.metricsMap;
    List<MetricFamilySamples> collect = metricsMap.values().stream().map(
      meter -> {
        String metricName = StringUtils.isEmpty(meter.getName()) ? "cloud_api_invoke" : meter.getName();
        MetricFamilySamples.Sample sample = new MetricFamilySamples.Sample(metricName, meter.getLables(), meter.getValues(), meter.getCount().get());
        List<MetricFamilySamples.Sample> samples = new ArrayList<>();
        samples.add(sample);
        return new MetricFamilySamples(metricName,
          Type.COUNTER,
          StringUtils.isEmpty(meter.getDescription()) ? "this is cloud api invoke metrics" : meter.getDescription(),
          samples);
      }
    ).collect(Collectors.toList());
    return collect;
  }
}
