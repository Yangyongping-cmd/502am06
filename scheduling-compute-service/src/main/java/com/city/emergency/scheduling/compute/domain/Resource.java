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
public class Resource implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String resourceType;
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
