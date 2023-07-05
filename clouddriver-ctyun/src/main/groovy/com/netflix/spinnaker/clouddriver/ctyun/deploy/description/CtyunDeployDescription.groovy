package com.netflix.spinnaker.clouddriver.ctyun.deploy.description

import cn.ctyun.ctapi.scaling.groupcreate.MazInfo
import com.netflix.spinnaker.clouddriver.deploy.DeployDescription
import com.netflix.spinnaker.clouddriver.security.resources.ServerGroupsNameable
import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class CtyunDeployDescription extends AbstractCtyunCredentialsDescription implements DeployDescription {
  /*
  common
   */
  String application
  String accountName

  String region //regionID
  List<String> securityGroupIds //securityGroupIDList安全组ID列表，非多可用区资源池不使用该参数，其安全组参数在弹性伸缩配置中填写，不管怎么样都传
  Integer recoveryMode //本参数表示云主机回收模式。取值范围:1：释放模式。2：停机回收模式。

  //serverGroupName+stack+detail==name
  String serverGroupName
  String stack
  String detail

  Integer healthMode // 1：云服务器健康检查。2：弹性负载均衡健康检查
  List<Map<String,Object>> mazInfoList //mazInfo 多可用区资源池的实例可用区及子网信息。mazInfo和subnetIDList 2个参数互斥，如果资源池为多可用区时使用mazInfo则不传subnetIDList 参数
  //masterId 主网卡，子网可跨可用区
  //azName 云主机的可用区
  //optionId 扩展网卡列表
  //以上数据时MazInfo的数据，这个可能需要后台给个mazInfo列表才可以选择，如果给了，下面subnetIds就不让选了，这块要考虑天翼云后台创建部分

  List<String> subnetIds  //subnetIDList 非多可用区资源池的子网ID列表。mazInfo和subnetIDList 2个参数互斥，如果资源池为非多可用区时使用subnetIDList 则不传mazInfo参数

  List<Integer> terminationPolicies //moveOutStrategy 本参数表示移除策略。取值范围：1：较早创建的配置较早创建的云主机。2：较晚创建的配置较晚创建的云主机。3：较早创建的云主机。4：较晚创建的云主机。
  //useLb 后台根据forwardLoadBalancers内容自行判断取值
  String vpcId //vpcID
  Integer maxSize //minCount
  Integer minSize // maxCount
  Integer desiredCapacity //期望值，通过期望值初始化伸缩组实例个数
  Integer healthPeriod //healthPeriod
  List<Map<String, Object>> forwardLoadBalancers// lbList
  Integer projectId //projectID

  //configObj自行拼凑，一下为需要传的参数
  //name后台自己创建
  String instanceType //specName		规格名称
  String imageId //imageID	镜像ID

  //volumes	磁盘类型和大小列表，元素为Volume ,后端负责dataDisks和systemDisk组装成list，不用处理
  List<Map<String, Object>> dataDisks
  Map<String, Object> systemDisk
  //securityGroupIDList 上面已经传过了List<String> securityGroupIds	安全组ID列表，非多可用区资源池该参数为必填，后端判断怎么用

  //internetAccessible.publicIpAssigned 对应useFloatings	本参数表示是否使用弹性IP,就是公共ip。取值范围：1：不使用。 2：自动分配。
  //internetAccessible.internetChargeType 对应billingMode 本参数表示计费类型。取值范围：1：按带宽计费。2：按流量计费。useFloatings=2时必填
  //internetAccessible.internetMaxBandwidthOut 对应 bandWidth	否	Integer	带宽大小
  Map<String, Object> internetAccessible
  //loginSettings.loginMode loginMode	是	Integer	本参数表示登陆方式。取值范围：1：密码。2：秘钥对。
  //loginSettings.userName username	否	String	用户名，loginMode=1时必填
  //loginSettings.password  password	否	String	密码，loginMode=1时必填
  //loginSettings.keyIds keyPairID	否	String	秘钥对ID，loginMode=2时必填
  Map<String, Object> loginSettings
  String userData //userData	否	String	用户自定义数据,需要以Base64方式编码
  //configID创建没有选择的地方，暂时不需要
  //ruleList 暂时不维护

  //配置id，如果是没有页面的clone，用configid创建配置信息
  Integer configId

  //下面参数目前不知道干啥用的
  String instanceChargeType
  Map<String, Object> enhancedService
  Map<String, Object> instanceMarketOptionsRequest
  List<String> instanceTypes
  String instanceTypesCheckPolicy
  List<Map<String, String>> instanceTags
  String camRoleName
  /*
  auto scaling group part
   */



  Integer defaultCooldown
  List<String> loadBalancerIds

  List<String> zones
  String retryPolicy
  String zonesCheckPolicy


/*  String masterId
  String azName
  String optionId*/
  /*
  clone source
   */

  Source source = new Source()
  boolean copySourceScalingPoliciesAndActions = true


  @Canonical
  static class Source implements ServerGroupsNameable {
    String region
    String serverGroupName
    Boolean useSourceCapacity

    @Override
    Collection<String> getServerGroupNames() {
      return Collections.singletonList(serverGroupName)
    }
  }
}
