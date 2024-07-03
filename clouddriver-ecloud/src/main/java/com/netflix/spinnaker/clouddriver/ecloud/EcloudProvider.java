package com.netflix.spinnaker.clouddriver.ecloud;

import com.netflix.spinnaker.clouddriver.core.CloudProvider;
import java.lang.annotation.Annotation;
import org.springframework.stereotype.Component;

@Component
public class EcloudProvider implements CloudProvider {
  public static final String PROVIDER_NAME = EcloudProvider.class.getName();

  public static final String ID = "ecloud";

  final String id = ID;
  final String displayName = "Ecloud";
  final Class<? extends Annotation> operationAnnotationType = EcloudOperation.class;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public Class<? extends Annotation> getOperationAnnotationType() {
    return operationAnnotationType;
  }
}
