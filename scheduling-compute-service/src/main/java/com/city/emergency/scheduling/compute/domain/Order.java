package com.city.emergency.scheduling.compute.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Double longitude;
    private Double latitude;
    private String address;
    private List<String> requiredSkills;
    private String requiredVehicleType;
    private Double requiredCapacity;
    private Boolean isEmergency;
    private Integer urgencyLevel;
    private Integer priority;
}
