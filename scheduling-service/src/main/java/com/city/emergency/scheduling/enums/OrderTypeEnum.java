package com.city.emergency.scheduling.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderTypeEnum {

    ROAD_RESCUE(1, "道路救援", 100, true),
    ACCIDENT_RESCUE(2, "事故救援", 150, true),
    MEDICAL_RESCUE(3, "医疗救援", 200, true),
    FIRE_RESCUE(4, "消防救援", 200, true),
    
    FREIGHT_SMALL(5, "小件货运", 30, false),
    FREIGHT_LARGE(6, "大件货运", 50, false),
    FREIGHT_URGENT(7, "紧急货运", 80, false),
    MOVING(8, "搬家服务", 60, false),
    EQUIPMENT_TRANSPORT(9, "设备运输", 70, false);

    private final Integer code;
    private final String desc;
    private final Integer basePriority;
    private final Boolean isEmergency;

    public static OrderTypeEnum getByCode(Integer code) {
        for (OrderTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public boolean isEmergencyType() {
        return this.isEmergency;
    }
    
    public boolean isFreightType() {
        return !this.isEmergency;
    }
}
