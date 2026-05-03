package com.city.emergency.scheduling.compute.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SchedulingTaskStatus {

    PENDING(1, "等待执行"),
    RUNNING(2, "执行中"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    TIMEOUT(5, "超时"),
    FAILED(6, "失败");

    private final Integer code;
    private final String desc;

    public static SchedulingTaskStatus getByCode(Integer code) {
        for (SchedulingTaskStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
