package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.netflix.spinnaker.clouddriver.ecloud.deploy.description.AbstractEcloudCredentialsDescription;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xu.dangling
 * @date 2024/4/12 @Description
 */
@Slf4j
public class EcloudDescriptionConverterHelper {

  public static <T extends AbstractEcloudCredentialsDescription> T convertDescription(
      Map<String, Object> input,
      AbstractAtomicOperationsCredentialsSupport credentialsSupport,
      Class<T> descriptionClass) {

    log.info(descriptionClass.getSimpleName() + " input:" + JSONObject.toJSONString(input));

    // 检查accountName是否存在，如果不存在则使用credentials作为accountName（第一次接收input)
    if (input.get("accountName") == null) {
      input.put("accountName", input.get("credentials"));
    }
    // 第一次处理后，之后都会有accountName字段
    if (input.get("accountName") != null) {
      input.put(
          "credentials",
          credentialsSupport.getCredentialsObject((String) input.get("accountName")));
    }

    // 移除credentials并保存，以便在ObjectMapper处理之后重新赋值
    EcloudCredentials credentials = (EcloudCredentials) input.remove("credentials");

    T converted =
        credentialsSupport
            .getObjectMapper()
            .copy()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .convertValue(input, descriptionClass);
    converted.setCredentials(credentials);

    return converted;
  }
}
