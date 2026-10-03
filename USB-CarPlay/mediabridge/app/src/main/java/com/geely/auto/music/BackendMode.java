package com.geely.auto.music;

public enum BackendMode {
    LEGACY("legacy", "MediaBridge 标准"),
    F25("f25", "F25 兼容"),
    PHONE_DEBUG("phone-debug", "手机调试台");

    private final String value;
    private final String label;

    BackendMode(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    public static BackendMode from(String value) {
        for (BackendMode mode : values()) {
            if (mode.value.equals(value)) return mode;
        }
        return LEGACY;
    }
}
