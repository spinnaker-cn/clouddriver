package com.netflix.spinnaker.clouddriver.ctyun.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.netflix.spinnaker.clouddriver.model.Image

class CtyunImage implements Image {
  String name
  String region
  String type
  String createdTime
  String imageId
  String osPlatform
  List<Map<String, Object>> snapshotSet

  @Override
  @JsonIgnore
  String getId() {
    return imageId
  }
}
