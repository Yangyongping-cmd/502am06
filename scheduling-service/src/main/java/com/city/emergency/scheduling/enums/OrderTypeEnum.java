package com.city.emergency.scheduling.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderTypeEnum {

    ROAD_RESCUE(1, "道路救援", 100),
    ACCIDENT_RESCUE(2, "事故救援", 150),
    MEDICAL_RESCUE(3, "医疗救援", 200),
    FIRE_RESCUE(4, "消防救援", 200),
    MOVING(5, "同城货运", 30),
    HOUSEKEEPING(6, "家政服务", 20),
    MAINTENANCE(7, "维修服务", 25),
    DELIVERY(8, "配送服务", 15);

    private final Integer code;
    private final String desc;
    private final Integer basePriority;

    public static OrderTypeEnum getByCode(Integer code) {
        for (OrderTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public boolean isEmergencyType() {
        return this.code <= 4;
    }
}
