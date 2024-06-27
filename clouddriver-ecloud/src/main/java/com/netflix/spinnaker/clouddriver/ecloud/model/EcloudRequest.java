package com.netflix.spinnaker.clouddriver.ecloud.model;

import java.util.List;
import java.util.Map;

public class EcloudRequest {
  private String method;
  private String region;
  private String path;
  private String version;
  private String accessKey;
  private String secretKey;
  private Map<String, List<String>> headers;
  private Map<String, String> queryParams;
  private Map<String, Object> bodyParams;

  public EcloudRequest(
      String method, String region, String path, String accessKey, String secretKey) {
    this.method = method;
    this.region = region;
    this.path = path;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, List<String>> headers) {
    this.headers = headers;
  }

  public Map<String, String> getQueryParams() {
    return queryParams;
  }

  public void setQueryParams(Map<String, String> queryParams) {
    this.queryParams = queryParams;
  }

  public Map<String, Object> getBodyParams() {
    return bodyParams;
  }

  public void setBodyParams(Map<String, Object> bodyParams) {
    this.bodyParams = bodyParams;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }
}
