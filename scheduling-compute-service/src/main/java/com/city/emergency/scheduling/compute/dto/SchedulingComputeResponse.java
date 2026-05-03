package com.city.emergency.scheduling.compute.dto;

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
public class SchedulingComputeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId;
    private String status;
    private String errorMessage;
    
    private Integer hardScore;
    private Integer softScore;
    private Double totalDistance;
    private Double estimatedTotalTime;
    private Integer unassignedOrders;
    private String solutionDetails;
    
    private List<AssignmentResult> assignments;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentResult implements Serializable {
        private Long orderId;
        private String orderNo;
        private Long resourceId;
        private String resourceName;
        
        private Double distance;
        private Double estimatedTravelTime;
        private Double waitingTime;
        
        private Double score;
        private String scoreDetails;
        
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
