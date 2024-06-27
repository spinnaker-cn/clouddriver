package com.netflix.spinnaker.clouddriver.ecloud.enums;

/**
 * @author han.pengfei
 * @description
 * @date 2024-04-19
 */
public enum LbSpecEnum {
  ONE(1, "普通型"),
  TWO(2, "优享型I"),

  THREE(3, "优享型II"),

  FOUTH(4, "高端型I"),

  FIVE(5, "高端型II"),

  SIX(6, "旗舰型"),
  TEN(10, "简约型");

  private final Integer code;

  private final String desc;

  LbSpecEnum(Integer code, String desc) {
    this.code = code;
    this.desc = desc;
  }

  public Integer getCode() {
    return code;
  }

  public String getDesc() {
    return desc;
  }

  public static String getDescByCode(Integer code) {
    for (LbSpecEnum value : LbSpecEnum.values()) {
      if (value.getCode().equals(code)) {
        return value.getDesc();
      }
    }
    return null;
  }
}
