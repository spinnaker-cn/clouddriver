package com.netflix.spinnaker.clouddriver.ecloud.client;

import com.ecloud.sdk.config.Config;
import com.ecloud.sdk.util.StringUtil;
import com.ecloud.sdk.vpc.v1.Client;
import com.ecloud.sdk.vpc.v1.model.CreateSecurityGroupBody;
import com.ecloud.sdk.vpc.v1.model.CreateSecurityGroupRequest;
import com.ecloud.sdk.vpc.v1.model.CreateSecurityGroupResponse;
import com.ecloud.sdk.vpc.v1.model.CreateSecurityGroupRuleBody;
import com.ecloud.sdk.vpc.v1.model.CreateSecurityGroupRuleRequest;
import com.ecloud.sdk.vpc.v1.model.CreateSecurityGroupRuleResponse;
import com.ecloud.sdk.vpc.v1.model.DeleteSecGroupRulePath;
import com.ecloud.sdk.vpc.v1.model.DeleteSecGroupRuleRequest;
import com.ecloud.sdk.vpc.v1.model.DeleteSecurityGroupPath;
import com.ecloud.sdk.vpc.v1.model.DeleteSecurityGroupRequest;
import com.ecloud.sdk.vpc.v1.model.ListSecGroupQuery;
import com.ecloud.sdk.vpc.v1.model.ListSecGroupRequest;
import com.ecloud.sdk.vpc.v1.model.ListSecGroupResponse;
import com.ecloud.sdk.vpc.v1.model.ListSecGroupResponseContent;
import com.ecloud.sdk.vpc.v1.model.ListSecurityGroupRuleQuery;
import com.ecloud.sdk.vpc.v1.model.ListSecurityGroupRuleRequest;
import com.ecloud.sdk.vpc.v1.model.ListSecurityGroupRuleResponse;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.UpsertEcloudSecurityGroupDescription;
import com.netflix.spinnaker.clouddriver.ecloud.exception.EcloudException;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudSecurityGroupRule;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class EcloudVirtualPrivateCloudClient {
  protected EcloudCredentials account;
  protected String region;
  protected Client client;

  public EcloudVirtualPrivateCloudClient(EcloudCredentials account, String region) {
    this.account = account;
    this.region = region;
    Config config = new Config();
    config.setAccessKey(account.getAccessKey());
    config.setSecretKey(account.getSecretKey());
    config.setPoolId(region);
    client = new Client(config);
  }

  public ListSecGroupResponse getSecurityGroupById(String securityGroupId) throws EcloudException {
    try {
      ListSecGroupRequest request = ListSecGroupRequest.builder().build();
      request.setListSecGroupQuery(
          ListSecGroupQuery.builder()
              .securityGroupIds(Collections.singletonList(securityGroupId))
              .build());
      return client.listSecGroup(request);
    } catch (Exception e) {
      throw new EcloudException(e.getMessage(), e);
    }
  }

  public ListSecurityGroupRuleResponse getSecurityGroupPolicies(String securityGroupId) {
    try {
      ListSecurityGroupRuleRequest request = ListSecurityGroupRuleRequest.builder().build();
      request.setListSecurityGroupRuleQuery(
          ListSecurityGroupRuleQuery.builder().securityGroupId(securityGroupId).build());
      return client.listSecurityGroupRule(request);
    } catch (Exception e) {
      throw new EcloudException(e.getMessage(), e);
    }
  }

  public List<ListSecGroupResponseContent> getSecurityGroupsAll() {
    int page = 1;
    int size = 50;
    int count = 0;
    ListSecGroupRequest request = ListSecGroupRequest.builder().build();
    List<ListSecGroupResponseContent> securityGroupAll = new ArrayList<>();
    ListSecGroupResponse resp;
    ListSecGroupQuery query = ListSecGroupQuery.builder().build();

    while (true) {
      query.setPage(page);
      query.setPageSize(size);
      request.setListSecGroupQuery(query);
      try {
        resp = client.listSecGroup(request);
        if (resp == null || resp.getBody() == null) {
          break;
        }
        List<ListSecGroupResponseContent> securityGroups = resp.getBody().getContent();
        if (!CollectionUtils.isEmpty(securityGroups)) {
          count += securityGroups.size();
          securityGroupAll.addAll(securityGroups);
          if (count < resp.getBody().getTotal()) {
            page++;
          } else {
            break;
          }
        }
      } catch (EcloudException e) {
        log.error(
            "Unable to list security groups (limit: {}, region: {}, account: {})",
            size,
            region,
            account,
            e);
        break;
      }
    }
    return securityGroupAll;
  }

  public void deleteSecurityGroup(String securityGroupId) {
    try {
      DeleteSecurityGroupRequest request = DeleteSecurityGroupRequest.builder().build();
      DeleteSecurityGroupPath deleteSecurityGroupPath =
          DeleteSecurityGroupPath.builder().securityGroupId(securityGroupId).build();
      request.setDeleteSecurityGroupPath(deleteSecurityGroupPath);
      client.deleteSecurityGroup(request);
    } catch (Exception e) {
      throw new EcloudException(e.toString());
    }
  }

  public String createSecurityGroup(
      UpsertEcloudSecurityGroupDescription upsertEcloudSecurityGroupDescription) {
    try {
      CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder().build();
      CreateSecurityGroupBody createSecurityGroupBody =
          CreateSecurityGroupBody.builder()
              .description(
                  upsertEcloudSecurityGroupDescription.getSecurityGroupDesc() == null
                      ? "spinnaker create"
                      : upsertEcloudSecurityGroupDescription.getSecurityGroupDesc())
              .name(upsertEcloudSecurityGroupDescription.getSecurityGroupName())
              .region(upsertEcloudSecurityGroupDescription.getRegion())
              .stateful(upsertEcloudSecurityGroupDescription.getStateful())
              .type(
                  CreateSecurityGroupBody.TypeEnum.fromValue(
                      upsertEcloudSecurityGroupDescription.getType()))
              .build();
      request.setCreateSecurityGroupBody(createSecurityGroupBody);
      CreateSecurityGroupResponse result = client.createSecurityGroup(request);
      return result.getBody();
    } catch (EcloudException e) {
      throw new EcloudException(e.toString());
    }
  }

  /**
   * * todo:腾讯云只对inRule操作
   *
   * @param groupId
   * @param inRules
   * @param outRules
   * @return
   */
  public String createSecurityGroupRules(
      String groupId,
      List<EcloudSecurityGroupRule> inRules,
      List<EcloudSecurityGroupRule> outRules) {
    try {
      CreateSecurityGroupRuleRequest request = CreateSecurityGroupRuleRequest.builder().build();
      CreateSecurityGroupRuleBody securityGroupRuleBody =
          CreateSecurityGroupRuleBody.builder().build();
      securityGroupRuleBody.setSecurityGroupId(groupId);
      for (EcloudSecurityGroupRule rule : inRules) {
        if (StringUtil.isBlank(rule.getCidrBlock())) {
          securityGroupRuleBody.setRemoteType(
              CreateSecurityGroupRuleBody.RemoteTypeEnum.SECURITY_GROUP);
        } else {
          securityGroupRuleBody.setRemoteType(CreateSecurityGroupRuleBody.RemoteTypeEnum.CIDR);
          securityGroupRuleBody.setRemoteIpPrefix(rule.getCidrBlock());
        }
        securityGroupRuleBody.setDescription(rule.getDirection());
        securityGroupRuleBody.setProtocol(
            CreateSecurityGroupRuleBody.ProtocolEnum.fromValue(rule.getProtocol()));
        securityGroupRuleBody.setMinPortRange(rule.getMinPortRange());
        securityGroupRuleBody.setMaxPortRange(rule.getMaxPortRange());
        request.setCreateSecurityGroupRuleBody(securityGroupRuleBody);
        CreateSecurityGroupRuleResponse result = client.createSecurityGroupRule(request);
        log.info("Created security result: {}", result);
      }
    } catch (Exception e) {
      throw new EcloudException(e.toString());
    }
    return "";
  }

  public String deleteSecurityGroupInRules(List<EcloudSecurityGroupRule> inRules) {
    try {
      DeleteSecGroupRuleRequest request = DeleteSecGroupRuleRequest.builder().build();

      DeleteSecGroupRulePath deleteSecGroupRulePath = DeleteSecGroupRulePath.builder().build();
      for (EcloudSecurityGroupRule rule : inRules) {
        deleteSecGroupRulePath.setSecurityGroupRuleId(rule.getId());
        client.deleteSecGroupRule(request);
      }
    } catch (EcloudException e) {
      throw new EcloudException(e.toString());
    }
    return "";
  }
}
