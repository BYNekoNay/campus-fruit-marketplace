package com.campusfruit.order.enums;

/**
 * 操作者类型枚举。
 */
public enum OperatorType {

    /** 系统自动 */
    SYSTEM("系统"),

    /** 普通用户 */
    USER("用户"),

    /** 门店员工 */
    STORE_STAFF("门店员工"),

    /** 管理员 */
    ADMIN("管理员");

    private final String label;

    OperatorType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
