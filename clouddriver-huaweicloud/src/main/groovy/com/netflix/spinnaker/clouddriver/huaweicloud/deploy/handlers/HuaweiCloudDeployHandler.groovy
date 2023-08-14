package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.handlers

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.cloud.apigateway.sdk.utils.Client
import com.cloud.apigateway.sdk.utils.Request
import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.deploy.DeployDescription
import com.netflix.spinnaker.clouddriver.deploy.DeployHandler
import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.HuaweiCloudServerGroupNameResolver
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.HuaweiCloudDeployDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.exception.HuaweiCloudOperationException
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.HuaweiCloudClusterProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiAutoScalingClient
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiCloudEyeClient
import com.netflix.spinnaker.clouddriver.huaweicloud.util.HuaweiConfigUtil
import groovy.util.logging.Slf4j
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.stereotype.Component

/*
curl -X POST \
  http://localhost:7002/huaweicloud/ops \
  -H 'Content-Type: application/json' \
  -H 'Postman-Token: 16583564-d31a-442f-bb17-0a308ee2c529' \
  -H 'cache-control: no-cache' \
  -d '[{"createServerGroup":{"application":"myapp","stack":"dev","accountName":"test","imageId":"img-oikl1tzv","instanceType":"S2.SMALL2","zones":["ap-guangzhou-2"],"credentials":"my-account-name","maxSize":0,"minSize":0,"desiredCapacity":0,"vpcId":"","region":"ap-guangzhou","dataDisks":[{"diskType":"CLOUD_PREMIUM","diskSize":50}],"systemDisk":{"diskType":"CLOUD_PREMIUM","diskSize":50}}}]'
*/


@Component
@Slf4j
class HuaweiCloudDeployHandler implements DeployHandler<HuaweiCloudDeployDescription> {
  private static final String BASE_PHASE = "DEPLOY"

  @Autowired
  private HuaweiCloudClusterProvider huaweicloudClusterProvider

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }

  @Override
  boolean handles(DeployDescription description) {
    description instanceof HuaweiCloudDeployDescription
  }

  @Override
  DeploymentResult handle(HuaweiCloudDeployDescription description, List priorOutputs) {
    task.updateStatus BASE_PHASE, "Initializing deployment to ${description.zones}"

    def accountName = description.accountName
    def region = description.region
    def serverGroupNameResolver = new HuaweiCloudServerGroupNameResolver(
      accountName, region, huaweicloudClusterProvider, description.credentials)

    task.updateStatus BASE_PHASE, "Looking up next sequence..."

    def serverGroupName = serverGroupNameResolver.resolveNextServerGroupName(description.application, description.stack, description.detail, false)

    task.updateStatus BASE_PHASE, "Produced server group name: $serverGroupName"

    description.serverGroupName = serverGroupName

    HuaweiAutoScalingClient autoScalingClient = new HuaweiAutoScalingClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      region
    )

    if (description?.source?.useSourceCapacity) {
      log.info('copy source server group capacity')
      String sourceServerGroupName = description?.source?.serverGroupName
      String sourceRegion = description?.source?.region
      def sourceServerGroup = huaweicloudClusterProvider.getServerGroup(accountName, sourceRegion, sourceServerGroupName)
      if (!sourceServerGroup) {
        log.warn("source server group $sourceServerGroupName is not found")
      } else {
        // use source current num as desired capacity
        def sourceAsg = autoScalingClient.getAutoScalingGroupsByName(sourceServerGroupName)[0]
        description.desiredCapacity = sourceAsg.getCurrentInstanceNumber() as Integer
        description.maxSize = sourceServerGroup.asg.maxSize as Integer
        description.minSize = sourceServerGroup.asg.minSize as Integer
        // use source halth periodic parameters
        description.healthAuditMethod = sourceServerGroup.asg.healthAuditMethod as String
        description.healthPeriodicTime = sourceServerGroup.asg.healthPeriodicTime as Integer
        description.healthGracePeriod = sourceServerGroup.asg.healthGracePeriod as Integer
      }
    }

    task.updateStatus BASE_PHASE, "Composing server group $serverGroupName..."

    autoScalingClient.deploy(description)

    task.updateStatus BASE_PHASE, "Done creating server group $serverGroupName in $region."

    DeploymentResult deploymentResult = new DeploymentResult()
    deploymentResult.serverGroupNames = ["$region:$serverGroupName".toString()]
    deploymentResult.serverGroupNameByRegion[region] = serverGroupName

    if (description.copySourceScalingPoliciesAndActions) {
      copyScalingPolicy(description, deploymentResult)
      copyNotification(description, deploymentResult)  // copy notification by the way
      copyLifeCycleHook(description, deploymentResult)
      copySheduledTasks(description, deploymentResult)
    }
    return deploymentResult
  }

  private def copyNotification(HuaweiCloudDeployDescription description, DeploymentResult deployResult) {
    task.updateStatus BASE_PHASE, "Enter copyNotification."
    String sourceServerGroupName = description?.source?.serverGroupName
    String sourceRegion = description?.source?.region
    String accountName = description?.accountName
    def sourceServerGroup = huaweicloudClusterProvider.getServerGroup(accountName, sourceRegion, sourceServerGroupName)

    if (!sourceServerGroup) {
      log.warn("source server group not found, account $accountName, region $sourceRegion, source sg name $sourceServerGroupName")
      return
    }

    String sourceAsgId = sourceServerGroup.asg.autoScalingGroupId

    task.updateStatus BASE_PHASE, "Initializing copy notification from $sourceAsgId."

    HuaweiAutoScalingClient autoScalingClient = new HuaweiAutoScalingClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      sourceRegion
    )

    String newServerGroupName = deployResult.serverGroupNameByRegion[sourceRegion]
    def newAsg = autoScalingClient.getAutoScalingGroupsByName(newServerGroupName)[0]
    String newAsgId = newAsg.getScalingGroupId()

    def notifications = autoScalingClient.getNotification(sourceAsgId)
    for (notification in notifications) {
      try {
        autoScalingClient.createNotification(newAsgId, notification)
      } catch (HuaweiCloudOperationException e) {
        // something bad happened during creation, log the error and continue
        log.warn "create notification error $e"
      }
    }
  }

  private def copyLifeCycleHook(HuaweiCloudDeployDescription description, DeploymentResult deployResult) {
    task.updateStatus BASE_PHASE, "Enter copyLifeCycleHook."
    String sourceServerGroupName = description?.source?.serverGroupName
    String sourceRegion = description?.source?.region
    String accountName = description?.accountName
    def sourceServerGroup = huaweicloudClusterProvider.getServerGroup(accountName, sourceRegion, sourceServerGroupName)

    if (!sourceServerGroup) {
      log.warn("source server group not found, account $accountName, region $sourceRegion, source sg name $sourceServerGroupName")
      return
    }

    String sourceAsgId = sourceServerGroup.asg.autoScalingGroupId

    task.updateStatus BASE_PHASE, "Initializing copy lifecyclehook from $sourceAsgId."

    HuaweiAutoScalingClient autoScalingClient = new HuaweiAutoScalingClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      sourceRegion
    )

    String newServerGroupName = deployResult.serverGroupNameByRegion[sourceRegion]
    def newAsg = autoScalingClient.getAutoScalingGroupsByName(newServerGroupName)[0]
    String newAsgId = newAsg.getScalingGroupId()

    def hooks = autoScalingClient.getLifeCycleHook(sourceAsgId)

    for (hook in hooks) {
      try {
        autoScalingClient.createLifeCycleHook(newAsgId, hook)
      } catch (HuaweiCloudOperationException e) {
        // something bad happened during creation, log the error and continue
        log.warn "create hook error $e"
      }
    }
  }

  private def copyScalingPolicy(HuaweiCloudDeployDescription description, DeploymentResult deployResult) {
    task.updateStatus BASE_PHASE, "Enter copyScalingPolicy."

    String sourceServerGroupName = description?.source?.serverGroupName
    String sourceRegion = description?.source?.region
    String accountName = description?.accountName
    def sourceServerGroup = huaweicloudClusterProvider.getServerGroup(accountName, sourceRegion, sourceServerGroupName)

    if (!sourceServerGroup) {
      log.warn("description is $description")
      log.warn("source server group not found, account $accountName, region $sourceRegion, source sg name $sourceServerGroupName")
      return
    }

    String sourceAsgId = sourceServerGroup.asg.autoScalingGroupId

    task.updateStatus BASE_PHASE, "Initializing copy scaling policy from $sourceAsgId."

    HuaweiAutoScalingClient autoScalingClient = new HuaweiAutoScalingClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      sourceRegion
    )

    HuaweiCloudEyeClient cloudEyeClient = new HuaweiCloudEyeClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      sourceRegion
    )

    String newServerGroupName = deployResult.serverGroupNameByRegion[sourceRegion]
    def newAsg = autoScalingClient.getAutoScalingGroupsByName(newServerGroupName)[0]
    String newAsgId = newAsg.getScalingGroupId()

    // copy all scaling policies
    def scalingPolicies = autoScalingClient.getScalingPolicies(sourceAsgId)
    for (scalingPolicy in scalingPolicies) {
      try {
        def alarmId = scalingPolicy.getAlarmId()
        if (alarmId) {
          def alarm = cloudEyeClient.getAlarm(alarmId)
          def newAlarm = cloudEyeClient.createAlarm(alarm, newAsgId)
          alarmId = newAlarm.getAlarmId()
        }
        autoScalingClient.createScalingPolicy(newAsgId, newServerGroupName, scalingPolicy, alarmId)
      } catch (HuaweiCloudOperationException e) {
        // something bad happened during creation, log the error and continue
        log.warn "create scaling policy error $e"
      }
    }
  }

  private def copySheduledTasks(HuaweiCloudDeployDescription description, DeploymentResult deployResult) {
    task.updateStatus BASE_PHASE, "Enter copySheduledTasks."

    String sourceServerGroupName = description?.source?.serverGroupName
    String sourceRegion = description?.source?.region
    String accountName = description?.accountName
    def sourceServerGroup = huaweicloudClusterProvider.getServerGroup(accountName, sourceRegion, sourceServerGroupName)

    if (!sourceServerGroup) {
      log.warn("description is $description")
      log.warn("source server group not found, account $accountName, region $sourceRegion, source sg name $sourceServerGroupName")
      return
    }

    String sourceAsgId = sourceServerGroup.asg.autoScalingGroupId

    task.updateStatus BASE_PHASE, "Initializing copy sheduled tasks from $sourceAsgId."
    CloseableHttpClient client = HttpClients.custom().build();
    try {
      Request request = new Request();
      request.setKey(description.credentials.credentials.accessKeyId);
      request.setSecret(description.credentials.credentials.accessSecretKey);
      request.setMethod("GET");
      def projectId = HuaweiConfigUtil.getProjectId(description.credentials.credentials.accessKeyId, sourceRegion)
      def queryAsgUrl = "https://as.cn-north-4.myhuaweicloud.com/autoscaling-api/v1/$projectId/scaling-groups/$sourceAsgId/scheduled-tasks";
      task.updateStatus BASE_PHASE, "Query task url: $queryAsgUrl"
      request.setUrl(queryAsgUrl);
      request.addHeader("Content-Type", "application/json");
      HttpRequestBase sign = Client.sign(request);
      CloseableHttpResponse response = client.execute(sign);
      HttpEntity entity = response.getEntity();
      String content = EntityUtils.toString(entity, "UTF-8");

      def jsonObject = JSONObject.parseObject(content);
      def scheduleds = (JSONArray) jsonObject.get("scheduled_tasks");
      HuaweiAutoScalingClient autoScalingClient = new HuaweiAutoScalingClient(
        description.credentials.credentials.accessKeyId,
        description.credentials.credentials.accessSecretKey,
        sourceRegion
      )

      String newServerGroupName = deployResult.serverGroupNameByRegion[sourceRegion]
      def newAsg = autoScalingClient.getAutoScalingGroupsByName(newServerGroupName)[0]
      String newAsgId = newAsg.getScalingGroupId()

      request.setMethod("POST");
      String postUrl = "https://as.cn-north-4.myhuaweicloud.com/autoscaling-api/v1/$projectId/scaling-groups/$newAsgId/scheduled-tasks";
      task.updateStatus BASE_PHASE, "Create task url: $postUrl"
      scheduleds.forEach({ scheduled ->
        request.setBody(String.valueOf(scheduled));
        try {
          request.setUrl(postUrl);
          HttpRequestBase postSign = Client.sign(request);
          CloseableHttpResponse execute = client.execute(postSign);
          String s = EntityUtils.toString(execute.getEntity());
        } catch (Exception e) {
          log.error("create scheduled task error asgId: $newAsg")
          e.printStackTrace();
        }
      });
    } catch (Exception e) {
      log.error("create scheduled task error: $sourceAsgId")
      e.printStackTrace();
    } finally {
      client.close()
    }
  }
}
