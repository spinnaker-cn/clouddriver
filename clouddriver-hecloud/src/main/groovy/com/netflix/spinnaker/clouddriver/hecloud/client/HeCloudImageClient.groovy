package com.netflix.spinnaker.clouddriver.hecloud.client

import com.netflix.spinnaker.clouddriver.hecloud.constants.HeCloudConstants
import com.netflix.spinnaker.clouddriver.hecloud.exception.HeCloudOperationException

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import com.huaweicloud.sdk.ims.v2.ImsClient
import com.huaweicloud.sdk.ims.v2.model.ImageInfo
import com.huaweicloud.sdk.ims.v2.model.ListImagesRequest
import groovy.util.logging.Slf4j

@Slf4j
class HeCloudImageClient {
  private final DEFAULT_LIMIT = 100
  String region
  ImsClient client

  HeCloudImageClient(String accessKeyId, String accessSecretKey, String region) {
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey).withIamEndpoint(HeCloudConstants.Region.getIamEndPoint(region))
    def regionId = new Region(region, "https://ims." + region + "." + HeCloudConstants.END_POINT_SUFFIX)
    def config = HttpConfig.getDefaultHttpConfig()
    this.region = region
    client = ImsClient.newBuilder()
      .withHttpConfig(config)
      .withCredential(auth)
      .withRegion(regionId)
      .build()
  }

  def getImages() {
    def marker = ""
    List<ImageInfo> imageAll = []
    while (true) {
      def request = new ListImagesRequest()
      request.setLimit(DEFAULT_LIMIT)
      request.setImagetype(ListImagesRequest.ImagetypeEnum.PRIVATE)
      request.setDiskFormat(ListImagesRequest.DiskFormatEnum.ZVHD2)
      if (marker) {
        request.setMarker(marker)
      }
      def resp
      try {
        resp = client.listImages(request)
      } catch (ServiceResponseException e) {
        log.error(
          "Unable to listImages (limit: {}, region: {})",
          DEFAULT_LIMIT,
          region,
          e
        )
      }
      if (resp == null || resp.getImages() == null || resp.getImages().size() == 0) {
        break
      }
      def images = resp.getImages()
      imageAll.addAll(images)
      marker = images[images.size() - 1].getId()

    }
    return imageAll
  }

}
