package com.netflix.spinnaker.clouddriver.ecloud.model;

import com.netflix.spinnaker.clouddriver.model.Image;
import lombok.Data;

@Data
public class EcloudImage implements Image {
  /** * 镜像所属区域 */
  private String region;
  /** * 镜像名称 */
  private String name;
  /** * 镜像类型 */
  private String type;
  /** * 镜像创建时间 */
  private String createdTime;
  /** * 镜像id */
  private String imageId;
  /** * 镜像操作系统名称 */
  private String osPlatform;
  /** * 镜像类型 */
  private String imageType;
  /** * 镜像快照ID */
  private String snapshotId;

  private int isPublic;

  public EcloudImage(
      String region,
      String name,
      String imageId,
      String imageType,
      String osPlatform,
      String createTime,
      String snapshotId,
      int isPublic) {
    this.region = region;
    this.name = name;
    this.imageId = imageId;
    this.imageType = imageType;
    this.osPlatform = osPlatform;
    this.createdTime = createTime;
    this.snapshotId = snapshotId;
    this.isPublic = isPublic;
  }

  @Override
  public String getId() {
    return imageId;
  }
}
