package com.city.emergency.location.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LocationSourceEnum {

    GPS(1, "GPS定位", 0.1),
    BEIDOU(2, "北斗定位", 0.1),
    WIFI(3, "WiFi定位", 0.3),
    CELL(4, "基站定位", 0.5),
    INERTIAL(5, "惯性导航", 0.2);

    private final Integer code;
    private final String desc;
    private final Double defaultAccuracy;

    public static LocationSourceEnum getByCode(Integer code) {
        for (LocationSourceEnum source : values()) {
            if (source.getCode().equals(code)) {
                return source;
            }
        }
        return null;
    }
}
