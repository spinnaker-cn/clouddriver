package com.netflix.spinnaker.clouddriver.ecloud.exception;

public class EcloudException extends RuntimeException {

  public EcloudException(String message) {
    super(message);
  }

  public EcloudException(Throwable cause) {
    super(cause);
  }

  public EcloudException(String message, Throwable cause) {
    super(message, cause);
  }
}
