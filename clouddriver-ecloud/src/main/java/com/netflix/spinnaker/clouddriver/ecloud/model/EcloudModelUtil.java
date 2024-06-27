package com.netflix.spinnaker.clouddriver.ecloud.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EcloudModelUtil {
  private static final List<SimpleDateFormat> dateFormats =
      Arrays.asList(
          new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
          new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"));

  public static Long translateTime(String time) {
    for (SimpleDateFormat dateFormat : dateFormats) {
      try {
        return dateFormat.parse(time).getTime();
      } catch (ParseException e) {
        log.error(e.getMessage(), e);
      }
    }
    return null;
  }
}
