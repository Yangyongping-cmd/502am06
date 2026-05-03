package com.city.emergency.scheduling.domain;

import com.city.emergency.scheduling.enums.OrderTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private OrderTypeEnum orderType;
    private String orderTypeName;
    private Integer priority;
    
    private Double longitude;
    private Double latitude;
    private String address;
    
    private List<String> requiredSkills;
    private String requiredVehicleType;
    private Double requiredCapacity;
    
    private LocalDateTime createTime;
    private LocalDateTime expectedStartTime;
    private LocalDateTime latestStartTime;
    private Integer expectedDuration;
    
    private String customerName;
    private String customerPhone;
    private String description;
    
    private Boolean isEmergency;
    private Boolean isReuseMode;
    private Integer urgencyLevel;
}
