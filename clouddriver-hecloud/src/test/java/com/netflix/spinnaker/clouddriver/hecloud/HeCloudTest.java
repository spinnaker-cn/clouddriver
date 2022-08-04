package com.netflix.spinnaker.clouddriver.hecloud;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.http.HttpConfig;
import com.huaweicloud.sdk.core.region.Region;
import com.huaweicloud.sdk.vpc.v2.model.ListSubnetsRequest;
import com.huaweicloud.sdk.vpc.v3.VpcClient;
import com.huaweicloud.sdk.vpc.v3.model.ListSecurityGroupRulesRequest;
import com.huaweicloud.sdk.vpc.v3.model.ListSecurityGroupRulesResponse;

/**
 * @author 硝酸铜
 * @date 2022/4/26
 */
public class HeCloudTest {
  public static void main(String[] args) {
    String ak = "ak";
    String sk = "sk";
    Region regionId = new Region("ecs.cidc-rp-11", "https://ecs.cidc-rp-11.joint.cmecloud.cn");
    String projectId = "projectId";

    HttpConfig config = HttpConfig.getDefaultHttpConfig();
    config.withIgnoreSSLVerification(true);

    BasicCredentials auth = new BasicCredentials()
      .withAk(ak)
      .withSk(sk)
      .withProjectId(projectId);


    VpcClient client = VpcClient.newBuilder()
      .withHttpConfig(config)
      .withCredential(auth)
      .withRegion(regionId)
      .build();


    ListSubnetsRequest request = new ListSubnetsRequest();
    ListSecurityGroupRulesResponse response = client.listSecurityGroupRules(new ListSecurityGroupRulesRequest());


    System.out.println("success: " + response.getSecurityGroupRules().toString());
  }
}
