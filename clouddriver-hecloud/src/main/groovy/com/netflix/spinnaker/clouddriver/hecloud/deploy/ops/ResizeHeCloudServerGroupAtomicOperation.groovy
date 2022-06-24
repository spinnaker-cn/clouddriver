package com.netflix.spinnaker.clouddriver.hecloud.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.hecloud.client.HeCloudAutoScalingClient
import com.netflix.spinnaker.clouddriver.hecloud.deploy.description.ResizeHeCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.hecloud.provider.view.HeCloudClusterProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class ResizeHeCloudServerGroupAtomicOperation implements AtomicOperation<Void> {
  private static final String BASE_PHASE = "RESIZE_SERVER_GROUP"

  @Autowired
  HeCloudClusterProvider heCloudClusterProvider

  private final ResizeHeCloudServerGroupDescription description

  ResizeHeCloudServerGroupAtomicOperation(ResizeHeCloudServerGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    task.updateStatus BASE_PHASE, "Initializing resize of server group $description.serverGroupName in " +
      "$description.region..."
    def accountName = description.accountName
    //def credentials = description.credentials
    def region = description.region
    def serverGroupName = description.serverGroupName
    def asgId = heCloudClusterProvider.getServerGroupAsgId(serverGroupName, accountName, region)

    def client = new HeCloudAutoScalingClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      region
    )
    client.resizeAutoScalingGroup(asgId, description.capacity)

    /*for (int i = 0 ; i < 3 ; i ++){

      sleep(2000)
      def asList = client.getAutoScalingGroupsByName(serverGroupName)
      def scalingGroup = asList.get(0)
      if(scalingGroup.getMaxInstanceNumber() == description.capacity.max &&
        scalingGroup.getMinInstanceNumber() == description.capacity.min &&
        scalingGroup.getDesireInstanceNumber() == description.capacity.desired
      ){
        log.info("Resize refresh is ready.")
        break;
      }
      if( i == 2){
        log.error("Resize refresh is not ready")
      }
    }*/

    //等待定时任务刷新redis数据
    sleep(30000)

    task.updateStatus BASE_PHASE, "Complete resize of server group $description.serverGroupName in " +
      "$description.region."
    null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
