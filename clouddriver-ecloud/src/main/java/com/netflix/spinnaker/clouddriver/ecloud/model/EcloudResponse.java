package com.netflix.spinnaker.clouddriver.ecloud.model;

public class EcloudResponse {
  private String requestId;
  private String errorCode;
  private String errorMessage;
  private String state;
  private Object body;
  private String httpCode;

  public EcloudResponse(String httpCode) {
    this.httpCode = httpCode;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public Object getBody() {
    return body;
  }

  public void setBody(Object body) {
    this.body = body;
  }

  public String getHttpCode() {
    return httpCode;
  }

  public void setHttpCode(String httpCode) {
    this.httpCode = httpCode;
  }

  public boolean isHttpSuccess() {
    return httpCode != null && httpCode.startsWith("20");
  }
}
