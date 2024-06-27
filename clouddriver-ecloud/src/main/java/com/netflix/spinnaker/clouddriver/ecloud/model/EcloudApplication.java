package com.netflix.spinnaker.clouddriver.ecloud.model;

import com.netflix.spinnaker.clouddriver.model.Application;
import java.util.Map;
import java.util.Set;

/**
 * @author xu.dangling
 * @date 2024/4/17 @Description
 */
public class EcloudApplication implements Application {

  private String name;

  private Map<String, Set<String>> clusterNames;

  private Map<String, String> attributes;

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setClusterNames(Map<String, Set<String>> clusterNames) {
    this.clusterNames = clusterNames;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<String, String> getAttributes() {
    return this.attributes;
  }

  @Override
  public Map<String, Set<String>> getClusterNames() {
    return clusterNames;
  }
}
