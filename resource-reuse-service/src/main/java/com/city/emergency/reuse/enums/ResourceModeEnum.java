package com.city.emergency.reuse.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResourceModeEnum {

    RESCUE_ONLY(1, "仅救援模式", "仅可执行救援任务"),
    REUSE_ALLOWED(2, "可复用模式", "可执行救援和家政任务"),
    REUSE_ACTIVE(3, "复用激活模式", "当前正执行家政任务"),
    SWITCHING(4, "切换中", "正在切换模式");

    private final Integer code;
    private final String desc;
    private final String detail;

    public static ResourceModeEnum getByCode(Integer code) {
        for (ResourceModeEnum mode : values()) {
            if (mode.getCode().equals(code)) {
                return mode;
            }
        }
        return null;
    }
}
