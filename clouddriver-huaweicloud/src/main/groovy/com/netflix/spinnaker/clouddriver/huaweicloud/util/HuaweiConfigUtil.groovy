package com.netflix.spinnaker.clouddriver.huaweicloud.util

import com.cloud.apigateway.sdk.utils.Client
import com.cloud.apigateway.sdk.utils.Request
import com.netflix.spinnaker.clouddriver.huaweicloud.config.HuaweiCloudExtProperties
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml

import java.util.stream.Collectors

/**
 * @author chen_muyi* @date 2023/8/10 17:05
 */
@Component
class HuaweiConfigUtil {

  private static List<HuaweiCloudExtProperties> staticProperties

  @Value('${extConfigUrl}')
  public void setProperties(String url) {
    Yaml yaml = new Yaml();
    def stream = new FileInputStream(new File(url))
    def configMap = yaml.load(stream)
    def huaweiExtConfig = configMap?.getAt("huaweiExtConfig") as ArrayList
    staticProperties = huaweiExtConfig.collect {
      it ->
        def staticProperties = new HuaweiCloudExtProperties()
        staticProperties.account = String.valueOf(it?.getAt("account"))
        staticProperties.accessKeyId = String.valueOf(it?.getAt("accessKeyId"))
        staticProperties.region = String.valueOf(it?.getAt("region"))
        staticProperties.projectId = String.valueOf(it?.getAt("projectId"))
        staticProperties.serverLessUrl = String.valueOf(it?.getAt("serverLessUrl"))
        staticProperties
    } as List
  }

  public static String getAsServerLessUrl(String ak, String region) {
    def filterProperties = staticProperties.findAll { it ->
      return Objects.equals(it.getAccessKeyId(), ak) && Objects.equals(it.getRegion(), region)
    }
    if (filterProperties.isEmpty()) {
      return null
    } else {
      return filterProperties.first().serverLessUrl
    }
  }

  public static String getProjectId(String ak, String region) {
    def filterProperties = staticProperties.findAll { it ->
      return Objects.equals(it.getAccessKeyId(), ak) && Objects.equals(it.getRegion(), region)
    }
    if (filterProperties.isEmpty()) {
      return null
    } else {
      return filterProperties.first().projectId
    }
  }
}
