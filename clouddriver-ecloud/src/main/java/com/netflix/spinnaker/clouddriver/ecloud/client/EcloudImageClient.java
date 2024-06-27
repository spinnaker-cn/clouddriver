package com.netflix.spinnaker.clouddriver.ecloud.client;

import com.ecloud.sdk.config.Config;
import com.ecloud.sdk.ims.v1.Client;
import com.ecloud.sdk.ims.v1.model.ListImageRespV2Query;
import com.ecloud.sdk.ims.v1.model.ListImageRespV2Request;
import com.ecloud.sdk.ims.v1.model.ListImageRespV2Response;
import com.ecloud.sdk.ims.v1.model.ListImageRespV2ResponseContent;
import com.ecloud.sdk.ims.v1.model.ListShareImageV2Query;
import com.ecloud.sdk.ims.v1.model.ListShareImageV2Request;
import com.ecloud.sdk.ims.v1.model.ListShareImageV2Response;
import com.ecloud.sdk.ims.v1.model.ListShareImageV2ResponseContent;
import com.netflix.spinnaker.clouddriver.ecloud.exception.EcloudException;
import com.netflix.spinnaker.clouddriver.ecloud.security.EcloudCredentials;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class EcloudImageClient {

  protected EcloudCredentials account;
  protected String region;
  protected Client client;

  public EcloudImageClient(EcloudCredentials account, String region) {
    this.account = account;
    this.region = region;
    Config config = new Config();
    config.setAccessKey(account.getAccessKey());
    config.setSecretKey(account.getSecretKey());
    config.setPoolId(region);
    client = new Client(config);
  }

  public List<ListImageRespV2ResponseContent> getImages() {
    int page = 1;
    int size = 50;
    int count = 0;
    ListImageRespV2Request request = new ListImageRespV2Request();
    ListImageRespV2Query listRespV2Query = new ListImageRespV2Query();

    List<ListImageRespV2ResponseContent> imageAll = new ArrayList<>();
    ListImageRespV2Response resp;

    while (true) {
      listRespV2Query.setPage(page);
      listRespV2Query.setPageSize(size);
      request.setListImageRespV2Query(listRespV2Query);
      try {
        resp = client.listImageRespV2(request);
        if (resp == null || resp.getBody() == null) {
          break;
        }
        List<ListImageRespV2ResponseContent> images = resp.getBody().getContent();
        if (!CollectionUtils.isEmpty(images)) {
          count += images.size();
          imageAll.addAll(images);
          if (count < resp.getBody().getTotal()) {
            page++;
          } else {
            break;
          }
        }
      } catch (EcloudException e) {
        log.error(
            "Unable to list self Images (limit: {}, region: {}, account: {})",
            size,
            region,
            account,
            e);
        break;
      }
    }
    return imageAll;
  }

  public List<ListShareImageV2ResponseContent> getShareImages() {
    int page = 1;
    int size = 50;
    int count = 0;
    ListShareImageV2Request request = ListShareImageV2Request.builder().build();
    ListShareImageV2Query query = new ListShareImageV2Query();
    List<ListShareImageV2ResponseContent> imageAll = new ArrayList<>();
    ListShareImageV2Response resp;

    while (true) {
      query.setPage(page);
      query.setPageSize(size);
      request.setListShareImageV2Query(query);
      try {
        resp = client.listShareImageV2(request);
        if (resp == null || resp.getBody() == null) {
          break;
        }
        List<ListShareImageV2ResponseContent> images = resp.getBody().getContent();
        if (!CollectionUtils.isEmpty(images)) {
          count += images.size();
          imageAll.addAll(images);
          if (count < resp.getBody().getTotal()) {
            page++;
          }
        }
      } catch (EcloudException e) {
        log.error(
            "Unable to list share Images (limit: {}, region: {}, account: {})",
            size,
            region,
            account,
            e);
        break;
      }
    }
    return imageAll;
  }
}
