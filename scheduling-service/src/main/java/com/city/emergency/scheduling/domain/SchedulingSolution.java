package com.city.emergency.scheduling.domain;

import lombok.Builder;
import lombok.Data;
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
public class SchedulingSolution implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private LocalDateTime createTime;
    
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

    public SchedulingSolution() {
    }

    public SchedulingSolution(Long id, String name, LocalDateTime createTime, 
                              List<Resource> resources, List<Assignment> assignments,
                              HardSoftScore score, Double totalDistance, 
                              Double estimatedTotalTime, Integer unassignedOrders, 
                              String solutionDetails) {
        this.id = id;
        this.name = name;
        this.createTime = createTime;
        this.resources = resources;
        this.assignments = assignments;
        this.score = score;
        this.totalDistance = totalDistance;
        this.estimatedTotalTime = estimatedTotalTime;
        this.unassignedOrders = unassignedOrders;
        this.solutionDetails = solutionDetails;
    }
}
