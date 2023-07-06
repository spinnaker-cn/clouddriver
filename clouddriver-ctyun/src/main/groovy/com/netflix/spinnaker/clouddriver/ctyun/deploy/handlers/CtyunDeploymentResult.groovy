package com.netflix.spinnaker.clouddriver.ctyun.deploy.handlers

import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult

class CtyunDeploymentResult extends DeploymentResult {
  List<Integer> serverGroupIds = []
  Map<String, Integer> serverGroupIdByRegion = [:]
}
