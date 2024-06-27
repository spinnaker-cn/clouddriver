package com.netflix.spinnaker.clouddriver.ecloud.deploy.converters;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-11
 */
public class EcloudAtomicOperationConverterHelper {
  static <T> T convertDescription(
      Map input,
      AbstractAtomicOperationsCredentialsSupport credentialsSupport,
      Class<T> description) {
    if (!input.containsKey("accountName") || input.get("accountName") == null) {
      input.put("accountName", input.get("credentials"));
    }
    if (input.get("accountName") != null) {
      input.put(
          "credentials",
          credentialsSupport.getCredentialsObject((String) input.get("accountName")));
    }
    // Save these to re-assign after ObjectMapper does its work.
    Object credentials = input.remove("credentials");
    ObjectMapper objectMapper = credentialsSupport.getObjectMapper().copy();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    T converted = objectMapper.convertValue(input, description);

    // Re-assign the credentials.
    try {
      description.getMethod("setCredentials", Object.class).invoke(converted, credentials);
    } catch (Exception e) {
      throw new RuntimeException("Failed to re-assign credentials", e);
    }
    return converted;
  }
}
