package com.city.emergency.scheduling.compute.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@PlanningSolution
public class SchedulingProblem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Boolean isEmergency;
    
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "resourceRange")
    private List<Resource> resources;
    
    @PlanningEntityCollectionProperty
    private List<Assignment> assignments;
    
    @PlanningScore
    private HardSoftScore score;
    
    private Double totalDistance;
    private Double estimatedTotalTime;
    private Integer unassignedOrders;
    private String solutionDetails;

    public SchedulingProblem() {
    }

    public SchedulingProblem(Long id, String name, Boolean isEmergency,
                              List<Resource> resources, List<Assignment> assignments,
                              HardSoftScore score, Double totalDistance, 
                              Double estimatedTotalTime, Integer unassignedOrders, 
                              String solutionDetails) {
        this.id = id;
        this.name = name;
        this.isEmergency = isEmergency;
        this.resources = resources;
        this.assignments = assignments;
        this.score = score;
        this.totalDistance = totalDistance;
        this.estimatedTotalTime = estimatedTotalTime;
        this.unassignedOrders = unassignedOrders;
        this.solutionDetails = solutionDetails;
    }
}
