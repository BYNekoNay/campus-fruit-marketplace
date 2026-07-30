package com.campusfruit.identity.enums;

/**
 * 申诉状态枚举。
 */
public enum AppealStatus {
    PENDING_REVIEW("待处理"),
    ACCEPTED("已受理"),
    UPHELD_FREEZE("维持冻结"),
    RESTORED("恢复账号");

    private final String label;

    AppealStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
