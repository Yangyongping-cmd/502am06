package com.city.emergency.scheduling.service;

import com.city.emergency.scheduling.domain.Assignment;
import com.city.emergency.scheduling.domain.Order;
import com.city.emergency.scheduling.domain.Resource;
import com.city.emergency.scheduling.domain.SchedulingSolution;
import com.city.emergency.scheduling.solver.SchedulingScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final SchedulingScoreCalculator scoreCalculator;
    private final ExecutorService schedulerExecutor = Executors.newFixedThreadPool(4);

    private static final long DEFAULT_SCHEDULING_TIMEOUT_SECONDS = 30;
    private static final long EMERGENCY_SCHEDULING_TIMEOUT_SECONDS = 5;

    public SchedulingSolution solveSchedulingProblem(List<Resource> resources, List<Order> orders) {
        return solveSchedulingProblem(resources, orders, false);
    }

    public SchedulingSolution solveSchedulingProblem(List<Resource> resources, List<Order> orders, 
                                                      boolean isEmergency) {
        log.info("开始执行调度优化, 资源数: {}, 订单数: {}, 是否紧急: {}", 
                 resources.size(), orders.size(), isEmergency);

        List<Assignment> assignments = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            Assignment assignment = Assignment.builder()
                    .id((long) i)
                    .order(order)
                    .build();
            assignments.add(assignment);
        }

        SchedulingSolution problem = SchedulingSolution.builder()
                .id(UUID.randomUUID().getMostSignificantBits())
                .name("Scheduling-" + LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .resources(resources)
                .assignments(assignments)
                .build();

        configureEnvironment(orders);

        Future<SchedulingSolution> future = schedulerExecutor.submit(() -> {
            SolverFactory<SchedulingSolution> solverFactory = createSolverFactory(isEmergency);
            Solver<SchedulingSolution> solver = solverFactory.buildSolver();
            
            long timeoutSeconds = isEmergency ? EMERGENCY_SCHEDULING_TIMEOUT_SECONDS : 
                                                       DEFAULT_SCHEDULING_TIMEOUT_SECONDS;
            solver.addEventListener(event -> {
            });

            return solver.solve(problem);
        });

        try {
            long timeoutSeconds = isEmergency ? EMERGENCY_SCHEDULING_TIMEOUT_SECONDS : 
                                                       DEFAULT_SCHEDULING_TIMEOUT_SECONDS;
            SchedulingSolution solution = future.get(timeoutSeconds, TimeUnit.SECONDS);
            
            calculateSolutionSummary(solution);
            
            log.info("调度优化完成, 硬分数: {}, 软分数: {}, 未分配订单数: {}",
                     solution.getScore().hardScore(), 
                     solution.getScore().softScore(),
                     solution.getUnassignedOrders());

            return solution;
        } catch (TimeoutException e) {
            log.warn("调度优化超时, 返回当前找到的最佳方案");
            future.cancel(true);
            return problem;
        } catch (Exception e) {
            log.error("调度优化执行失败", e);
            return problem;
        }
    }

    private void configureEnvironment(List<Order> orders) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        boolean isPeakHour = (hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19);
        scoreCalculator.setPeakHour(isPeakHour);
    }

    private SolverFactory<SchedulingSolution> createSolverFactory(boolean isEmergency) {
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(SchedulingSolution.class)
                .withEntityClasses(Assignment.class)
                .withEasyScoreCalculatorClass(SchedulingScoreCalculator.class);

        if (isEmergency) {
            solverConfig.withTerminationSpentLimit(Duration.ofSeconds(EMERGENCY_SCHEDULING_TIMEOUT_SECONDS));
        } else {
            solverConfig.withTerminationSpentLimit(Duration.ofSeconds(DEFAULT_SCHEDULING_TIMEOUT_SECONDS))
                       .withTerminationUnimprovedSecondsSpentLimit(5);
        }

        return SolverFactory.create(solverConfig);
    }

    private void calculateSolutionSummary(SchedulingSolution solution) {
        double totalDistance = 0.0;
        double totalTime = 0.0;
        int unassignedCount = 0;

        for (Assignment assignment : solution.getAssignments()) {
            if (assignment.getResource() == null) {
                unassignedCount++;
            } else {
                if (assignment.getDistance() != null) {
                    totalDistance += assignment.getDistance();
                }
                if (assignment.getEstimatedTravelTime() != null) {
                    totalTime += assignment.getEstimatedTravelTime();
                }
            }
        }

        solution.setTotalDistance(totalDistance);
        solution.setEstimatedTotalTime(totalTime);
        solution.setUnassignedOrders(unassignedCount);
        
        StringBuilder details = new StringBuilder();
        details.append("总距离: ").append(String.format("%.2f", totalDistance)).append("km, ");
        details.append("预计总时间: ").append(String.format("%.1f", totalTime)).append("分钟, ");
        details.append("未分配订单: ").append(unassignedCount);
        solution.setSolutionDetails(details.toString());
    }

    public List<Assignment> getQuickAssignment(List<Resource> resources, Order order) {
        List<Order> orders = new ArrayList<>();
        orders.add(order);
        
        boolean isEmergency = order.getIsEmergency() != null && order.getIsEmergency();
        
        SchedulingSolution solution = solveSchedulingProblem(resources, orders, isEmergency);
        
        return solution.getAssignments();
    }

    public Assignment getBestAssignment(List<Resource> resources, Order order) {
        List<Assignment> assignments = getQuickAssignment(resources, order);
        
        if (assignments.isEmpty()) {
            return null;
        }

        Assignment bestAssignment = assignments.get(0);
        if (bestAssignment.getResource() == null) {
            return bestAssignment;
        }

        double bestScore = calculateAssignmentScore(bestAssignment);

        for (Resource resource : resources) {
            Assignment tempAssignment = Assignment.builder()
                    .id(0L)
                    .order(order)
                    .resource(resource)
                    .build();
            
            double score = calculateAssignmentScore(tempAssignment);
            if (score > bestScore) {
                bestScore = score;
                bestAssignment = tempAssignment;
            }
        }

        return bestAssignment;
    }

    private double calculateAssignmentScore(Assignment assignment) {
        if (assignment.getResource() == null) {
            return -1000.0;
        }

        Order order = assignment.getOrder();
        Resource resource = assignment.getResource();

        double distance = calculateHaversineDistance(
                resource.getLatitude(), resource.getLongitude(),
                order.getLatitude(), order.getLongitude()
        );

        double distanceScore = 100.0 / (distance + 1.0);
        double skillScore = calculateSkillMatchScore(resource, order);
        double availabilityScore = (resource.getIsAvailable() != null && resource.getIsAvailable()) ? 100.0 : 0.0;
        double historicalScore = (resource.getHistoricalScore() != null) ? resource.getHistoricalScore() * 20.0 : 50.0;
        double reuseScore = calculateReuseScore(resource, order);

        return distanceScore * 0.3 + 
               skillScore * 0.25 + 
               availabilityScore * 0.2 + 
               historicalScore * 0.15 + 
               reuseScore * 0.1;
    }

    private double calculateHaversineDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return 10.0;
        }

        double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    private double calculateSkillMatchScore(Resource resource, Order order) {
        if (order.getRequiredSkills() == null || order.getRequiredSkills().isEmpty()) {
            return 100.0;
        }
        if (resource.getSkills() == null || resource.getSkills().isEmpty()) {
            return 0.0;
        }

        long matchedSkills = order.getRequiredSkills().stream()
                .filter(resource.getSkills()::contains)
                .count();

        return (double) matchedSkills / order.getRequiredSkills().size() * 100.0;
    }

    private double calculateReuseScore(Resource resource, Order order) {
        boolean isEmergency = order.getIsEmergency() != null && order.getIsEmergency();
        boolean allowReuse = resource.getAllowReuse() != null && resource.getAllowReuse();

        if (isEmergency) {
            return 100.0;
        }

        return allowReuse ? 100.0 : 20.0;
    }
}
