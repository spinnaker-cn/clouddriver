package com.netflix.spinnaker.clouddriver.tencent.exception;

import com.netflix.spinnaker.cats.agent.Agent;
import com.netflix.spinnaker.cats.agent.ExecutionInstrumentation;
import com.netflix.spinnaker.clouddriver.tencent.exception.ExceptionUtils;
import com.netflix.spinnaker.monitor.enums.AlarmLevelEnum;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author chen_muyi
 * @date 2021/12/2 9:22
 */
@Component
public class TencentMetricsExecutionInstrumentation implements ExecutionInstrumentation {

  @Override
  public void executionStarted(Agent agent) {

  }

  @Override
  public void executionCompleted(Agent agent, long elapsedMs) {

  }

  @Override
  public void executionFailed(Agent agent, Throwable cause) {
    String providerName = agent.getProviderName();
    if (!providerName.contains("com.netflix.spinnaker.clouddriver.tencent")){
      return;
    }
    String className = providerName.substring(providerName.lastIndexOf(".") + 1);
    if (cause instanceof RuntimeException){
      ExceptionUtils.registerMetric((RuntimeException)cause, AlarmLevelEnum.LEVEL_2,className);
    }else {
      ExceptionUtils.registerMetric(new RuntimeException("unKnown exception"), AlarmLevelEnum.LEVEL_2,className);
    }
  }
}