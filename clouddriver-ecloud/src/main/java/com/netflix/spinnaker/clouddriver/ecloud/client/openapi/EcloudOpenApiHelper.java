package com.netflix.spinnaker.clouddriver.ecloud.client.openapi;

import com.alibaba.fastjson2.JSONObject;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudRequest;
import com.netflix.spinnaker.clouddriver.ecloud.model.EcloudResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * @author xu.dangling
 * @date 2024/4/15 @Description
 */
@Slf4j
public class EcloudOpenApiHelper {

  private static final Map<String, String> ALL_REGIONS = initRegions();

  private static RestTemplate restTemplate;

  static {
    ClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    ((SimpleClientHttpRequestFactory) requestFactory).setConnectTimeout(30000);
    ((SimpleClientHttpRequestFactory) requestFactory).setReadTimeout(30000);
    restTemplate = new RestTemplate();
    restTemplate.setRequestFactory(requestFactory);
  }

  public static EcloudResponse execute(EcloudRequest request) {
    // body
    String jsonBody = JSONObject.toJSONString(request.getBodyParams());
    // headers
    HttpHeaders httpHeaders = new HttpHeaders();
    if (request.getHeaders() != null) {
      httpHeaders.putAll(request.getHeaders());
    }
    httpHeaders.put("Content-type", Collections.singletonList("application/json"));
    httpHeaders.put("Content-Length", Collections.singletonList("" + jsonBody.length()));
    // uri Params
    Map<String, String> uriParams = new HashMap<>();
    if (request.getQueryParams() != null) {
      uriParams.putAll(request.getQueryParams());
    }
    if (request.getVersion() != null) {
      uriParams.put("Version", request.getVersion());
    }
    // full url
    String signedUri =
        EcloudSignatureHelper.signPath(
            request.getMethod(),
            request.getPath(),
            request.getAccessKey(),
            request.getSecretKey(),
            uriParams);
    String fullUrl =
        new StringBuffer()
            .append(ALL_REGIONS.get(request.getRegion()))
            .append(signedUri)
            .toString();
    HttpEntity<String> entity = new HttpEntity<String>(jsonBody, httpHeaders);
    ResponseEntity<Map> rsp = null;
    String statusCode = null;
    log.info("OpenApi url is: " + fullUrl);
    try {
      rsp =
          restTemplate.exchange(
              fullUrl, HttpMethod.valueOf(request.getMethod()), entity, Map.class);
      statusCode = rsp.getStatusCode().toString();
    } catch (RestClientResponseException e) {
      log.error(e.getMessage(), e);
      statusCode = e.getRawStatusCode() + "";
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      statusCode = "500";
    }
    EcloudResponse eRsp = new EcloudResponse(statusCode);
    if (rsp != null && rsp.getBody() != null) {
      eRsp.setRequestId((String) rsp.getBody().get("requestId"));
      eRsp.setErrorCode((String) rsp.getBody().get("errorCode"));
      eRsp.setErrorMessage((String) rsp.getBody().get("errorMessage"));
      eRsp.setBody(rsp.getBody().get("body"));
    }
    return eRsp;
  }

  private static Map<String, String> initRegions() {
    Map<String, String> map = new HashMap();
    map.put("CIDC-RP-04", "https://console-yunnan-1.cmecloud.cn:8443");
    map.put("CIDC-RP-16", "https://console-qinghai-1.cmecloud.cn:8443");
    map.put("CIDC-RP-25", "https://console-wuxi-1.cmecloud.cn:8443");
    map.put("CIDC-RP-26", "https://console-dongguan-1.cmecloud.cn:8443");
    map.put("CIDC-RP-27", "https://console-yaan-1.cmecloud.cn:8443");
    map.put("CIDC-RP-28", "https://console-zhengzhou-1.cmecloud.cn:8443");
    map.put("CIDC-RP-29", "https://console-beijing-2.cmecloud.cn:8443");
    map.put("CIDC-RP-30", "https://console-zhuzhou-1.cmecloud.cn:8443");
    map.put("CIDC-RP-31", "https://console-jinan-1.cmecloud.cn:8443");
    map.put("CIDC-RP-32", "https://console-xian-1.cmecloud.cn:8443");
    map.put("CIDC-RP-33", "https://console-shanghai-1.cmecloud.cn:8443");
    map.put("CIDC-RP-34", "https://console-chongqing-1.cmecloud.cn:8443");
    map.put("CIDC-RP-35", "https://console-ningbo-1.cmecloud.cn:8443");
    map.put("CIDC-RP-36", "https://console-tianjin-1.cmecloud.cn:8443");
    map.put("CIDC-RP-37", "https://console-jilin-1.cmecloud.cn:8443");
    map.put("CIDC-RP-38", "https://console-hubei-1.cmecloud.cn:8443");
    map.put("CIDC-RP-39", "https://console-jiangxi-1.cmecloud.cn:8443");
    map.put("CIDC-RP-40", "https://console-gansu-1.cmecloud.cn:8443");
    map.put("CIDC-RP-41", "https://console-shanxi-1.cmecloud.cn:8443");
    map.put("CIDC-RP-42", "https://console-liaoning-1.cmecloud.cn:8443");
    map.put("CIDC-RP-43", "https://console-yunnan-2.cmecloud.cn:8443");
    map.put("CIDC-RP-44", "https://console-hebei-1.cmecloud.cn:8443");
    map.put("CIDC-RP-45", "https://console-fujian-1.cmecloud.cn:8443");
    map.put("CIDC-RP-46", "https://console-guangxi-1.cmecloud.cn:8443");
    map.put("CIDC-RP-47", "https://console-anhui-1.cmecloud.cn:8443");
    map.put("CIDC-RP-48", "https://console-huhehaote-1.cmecloud.cn:8443");
    map.put("CIDC-RP-49", "https://console-guiyang-1.cmecloud.cn:8443");
    map.put("CIDC-CORE-00", "https://ecloud.10086.cn");
    map.put("CIDC-RP-53", "https://console-hainan-1.cmecloud.cn:8443");
    map.put("CIDC-RP-54", "https://console-xinjiang-1.cmecloud.cn:8443");
    map.put("CIDC-RP-55", "http://console-heilongjiang-1.cmecloud.cn:18080");
    map.put("CIDC-BRP-25", "");
    map.put("CIDC-RP-60", "");
    map.put("CIDC-RP-61", "");
    map.put("CIDC-RP-62", "");
    return Collections.unmodifiableMap(map);
  }
}
