package com.city.emergency.scheduling.compute.domain;

import lombok.Builder;
import lombok.Data;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@PlanningEntity
public class Assignment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Order order;
    
    @PlanningVariable(valueRangeProviderRefs = "resourceRange", nullable = true)
    private Resource resource;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private Double distance;
    private Double estimatedTravelTime;
    private Double waitingTime;
    
    private Double score;
    private String scoreDetails;

    public Assignment() {
    }

    public Assignment(Long id, Order order, Resource resource, LocalDateTime startTime, 
                      LocalDateTime endTime, Double distance, Double estimatedTravelTime, 
                      Double waitingTime, Double score, String scoreDetails) {
        this.id = id;
        this.order = order;
        this.resource = resource;
        this.startTime = startTime;
        this.endTime = endTime;
        this.distance = distance;
        this.estimatedTravelTime = estimatedTravelTime;
        this.waitingTime = waitingTime;
        this.score = score;
        this.scoreDetails = scoreDetails;
    }
}
