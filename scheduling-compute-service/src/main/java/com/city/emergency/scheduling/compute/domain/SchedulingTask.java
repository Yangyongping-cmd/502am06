package com.city.emergency.scheduling.compute.domain;

import com.city.emergency.scheduling.compute.dto.SchedulingComputeRequest;
import com.city.emergency.scheduling.compute.dto.SchedulingComputeResponse;
import com.city.emergency.scheduling.compute.enums.SchedulingTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulingTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId;
    private SchedulingComputeRequest request;
    private SchedulingComputeResponse result;
    
    private SchedulingTaskStatus status;
    
    private LocalDateTime submitTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    
    private String errorMessage;
}
