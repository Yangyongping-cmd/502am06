package com.city.emergency.scheduling.compute.dto;

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
public class SchedulingComputeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId;
    private Boolean isEmergency;
    private Long timeoutSeconds;
    
    private List<ResourceInfo> resources;
    private List<OrderInfo> orders;
    
    private String environment;
    private String callbackUrl;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceInfo implements Serializable {
        private Long id;
        private String name;
        private String type;
        private Double longitude;
        private Double latitude;
        private List<String> skills;
        private List<String> serviceAreas;
        private Integer status;
        private Integer currentLoad;
        private Integer maxLoad;
        private Double historicalScore;
        private Boolean isAvailable;
        private Boolean allowReuse;
        private String vehicleType;
        private Double vehicleCapacity;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderInfo implements Serializable {
        private Long id;
        private String orderNo;
        private Integer orderType;
        private String orderTypeName;
        private Integer priority;
        private Double longitude;
        private Double latitude;
        private String address;
        private List<String> requiredSkills;
        private String requiredVehicleType;
        private Double requiredCapacity;
        private Boolean isEmergency;
        private Integer urgencyLevel;
    }
}
