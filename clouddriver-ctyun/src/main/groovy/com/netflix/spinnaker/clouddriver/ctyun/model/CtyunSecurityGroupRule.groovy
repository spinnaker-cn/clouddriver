package com.netflix.spinnaker.clouddriver.ctyun.model

class CtyunSecurityGroupRule {
  Integer index        //rule index
  String protocol      //TCP, UDP, ICMP, GRE, ALL
  String port          //all, 离散port,  range
  String cidrBlock
  String action        //ACCEPT or DROP
  String id            //主键id
  String ethertype     //IP类型:IPv4、IPv6
  String vpcId         //vpcId
}
