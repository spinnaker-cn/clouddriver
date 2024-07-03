package com.netflix.spinnaker.clouddriver.ecloud.deploy.description;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteEcloudSecurityGroupDescription extends AbstractEcloudCredentialsDescription {
  private String region;
  private String securityGroupId;
}
