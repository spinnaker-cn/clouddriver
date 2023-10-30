package com.netflix.spinnaker.clouddriver.ctyun.client

import cn.ctyun.ctapi.Credential
import com.netflix.spinnaker.clouddriver.ctyun.exception.CtyunOperationException
import com.tencentcloudapi.common.AbstractModel
import groovy.util.logging.Slf4j

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

@Slf4j
abstract class AbstractCtyunServiceClient {
  final int MAX_QUERY_TIME = 1000
  final int DEFAULT_LIMIT = 50

  abstract String getEndPoint()
  Credential cred

  AbstractCtyunServiceClient(String accessKey, String securityKey) {
    //cred = new Credential(accessKey, securityKey)
    cred = new Credential().withAk(accessKey).withSk(securityKey);
  }

  def iterQuery(maxItemNum=0, closure) {
    List<AbstractModel> models = []
    int query_index = 0
    int pageNum = 0
    int pageSize = DEFAULT_LIMIT
    try {
      while (query_index++ < MAX_QUERY_TIME) {
        def result = closure(pageNum, pageSize) as List
        if (result) {
          if (null == result || result.size()==0 || (maxItemNum && models.size() + result.size() > maxItemNum)) {
            break
          }

          models.addAll result
          pageNum++
        } else {
          break
        }
        sleep(500)
      }
      models
    } catch (Exception e) {
      throw new CtyunOperationException(e.toString())
    }
  }

  static Date ConvertIsoDateTime(String isoDateTime) {
    try {
      DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME
      TemporalAccessor accessor = timeFormatter.parse(isoDateTime)
      Date date = Date.from(Instant.from(accessor))
      return date
    } catch (Exception e) {
      log.warn "convert time error ${e.toString()}"
      return null
    }
  }

}
