package com.netflix.spinnaker.clouddriver.hecloud.model

import java.text.SimpleDateFormat

class HeCloudModelUtil {

  // 时间格式支持：yyyy-MM-ddThh:mm:ssZ或者yyyy-MM-dd hh:mm:ss
  private static final dateFormats = ["yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd HH:mm:ss"]
    .collect { new SimpleDateFormat(it) }

  static Long translateTime(String time) {
    for (SimpleDateFormat dateFormat: dateFormats) {
      try {
        return dateFormat.parse(time).getTime()
      } catch (e) { }
    }

    null
  }
}
