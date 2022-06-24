package com.netflix.spinnaker.clouddriver.hecloud.constants

interface HeCloudConstants {
  final static String END_POINT_SUFFIX = "joint.cmecloud.cn"

  static class Region{
    //华北-北京2
    final static String HUABEI_BEIJING2 = "cidc-rp-11"

    //华东-无锡
    final static String HUADONG_WUXI = "cidc-rp-12"

    //华东-福州
    final static String HUADONG_FUZHOU = "cidc-rp-13"

    //东北-沈阳
    final static String DONGBEI_SHENYANG = "cidc-rp-19"

    //华东-宁波
    final static String HUADONG_NINGBO = "cidc-rp-2000"

    static String getIamEndPoint(String region){
      return "https://iam." + region + ".joint.cmecloud.cn"
    }

  }
}
