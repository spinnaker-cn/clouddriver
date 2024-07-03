package com.netflix.spinnaker.clouddriver.ecloud.client.openapi;

import com.ecloud.sdk.util.StringUtil;
import com.netflix.spinnaker.clouddriver.ecloud.exception.EcloudException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class EcloudSignatureHelper {
  private static final char[] HEX_CODE_TABLE =
      new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  public static String signPath(
      String httpMethod,
      String path,
      String accessKey,
      String secretKey,
      Map<String, String> queryParams) {
    SortedMap<String, String> parameters = new TreeMap();
    if (queryParams != null) {
      queryParams.entrySet().stream()
          .filter(
              (e) -> {
                return e.getKey() != null;
              })
          .forEach(
              (e) -> {
                parameters.put(e.getKey(), e.getValue());
              });
    }
    parameters.put("AccessKey", accessKey);
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    parameters.put("Timestamp", dateFormat.format(new Date()));
    parameters.put("SignatureMethod", "HmacSHA1");
    parameters.put("SignatureVersion", "V2.0");
    parameters.put("SignatureNonce", nonce());
    String queryString =
        parameters.entrySet().stream()
            .map((e) -> String.format("%s=%s", e.getKey(), e.getValue()))
            .collect(Collectors.joining("&"));
    String canonicalQueryString =
        parameters.entrySet().stream()
            .map(
                (e) ->
                    String.format(
                        "%s=%s",
                        percentEncode((String) e.getKey()), percentEncode((String) e.getValue())))
            .collect(Collectors.joining("&"));
    String hashString = convertToHexString(sha256Encode(canonicalQueryString));
    String stringToSign =
        httpMethod.toUpperCase() + "\n" + StringUtil.percentEncode(path) + "\n" + hashString;
    String signature = convertToHexString(hmacSha1(stringToSign, "BC_SIGNATURE&" + secretKey));
    String servletPath =
        new StringBuffer(path)
            .append("?")
            .append(queryString)
            .append("&")
            .append(String.format("%s=%s", "Signature", percentEncode(signature)))
            .toString();
    return servletPath;
  }

  private static byte[] hmacSha1(String strToSign, String secretKey) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
      SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA1");
      mac.init(secretKeySpec);
      return mac.doFinal(strToSign.getBytes(StandardCharsets.UTF_8));
    } catch (InvalidKeyException | NoSuchAlgorithmException var5) {
      throw new EcloudException(var5);
    }
  }

  private static String nonce() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private static byte[] sha256Encode(String text) {
    if (text == null) {
      return new byte[0];
    } else {
      MessageDigest md;
      try {
        md = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException var3) {
        throw new EcloudException(var3);
      }

      return md.digest(text.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static String convertToHexString(byte[] data) {
    if (data == null) {
      return "";
    } else {
      int length = data.length;
      char[] value = new char[length << 1];
      int pos = 0;
      byte[] var4 = data;
      int var5 = data.length;

      for (int var6 = 0; var6 < var5; ++var6) {
        byte datum = var4[var6];
        value[pos++] = HEX_CODE_TABLE[(240 & datum) >>> 4];
        value[pos++] = HEX_CODE_TABLE[15 & datum];
      }

      return new String(value);
    }
  }

  private static String percentEncode(String text) {
    if (text == null) {
      return null;
    } else {
      try {
        return URLEncoder.encode(text, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~");
      } catch (UnsupportedEncodingException var2) {
        throw new EcloudException(var2);
      }
    }
  }
}
