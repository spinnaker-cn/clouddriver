package com.netflix.spinnaker.monitor;

import com.netflix.spinnaker.cats.agent.Agent;
import com.netflix.spinnaker.cats.agent.ExecutionInstrumentation;
import com.netflix.spinnaker.monitor.model.AgentMetric;
import com.netflix.spinnaker.monitor.util.MonitorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author chen_muyi
 * @date 2022/7/15 15:11
 */
@Slf4j
@Component
public class AgentExecutionInstrumentation implements ExecutionInstrumentation {

  @Override
  public void executionStarted(Agent agent) {
     log.info("{} loadData start",agent.getAgentType());
  }

  @Override
  public void executionCompleted(Agent agent, long elapsedMs) {
    log.info("{} loadData end,elapsed:{}ms ",agent.getAgentType(),elapsedMs);
    MonitorUtils.registerAgentElapsedMetric("agent_loadData_elapsed",agent.getAgentType(),elapsedMs);
  }

  @Override
  public void executionFailed(Agent agent, Throwable cause) {

  }
}
