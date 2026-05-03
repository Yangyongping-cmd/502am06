package com.city.emergency.scheduling.compute.service;

import com.city.emergency.scheduling.compute.domain.*;
import com.city.emergency.scheduling.compute.dto.*;
import com.city.emergency.scheduling.compute.enums.SchedulingTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingComputeService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private final Map<String, SchedulingTask> taskMap = new ConcurrentHashMap<>();
    
    private static final String TASK_CACHE_PREFIX = "scheduling:task:";
    private static final String RESULT_CACHE_PREFIX = "scheduling:result:";
    
    private static final long DEFAULT_EMERGENCY_TIMEOUT = 5000L;
    private static final long DEFAULT_NORMAL_TIMEOUT = 30000L;

    public String submitSchedulingRequest(SchedulingComputeRequest request) {
        String requestId = request.getRequestId() != null ? 
                request.getRequestId() : UUID.randomUUID().toString().replace("-", "");
        
        SchedulingTask task = SchedulingTask.builder()
                .requestId(requestId)
                .request(request)
                .status(SchedulingTaskStatus.PENDING)
                .submitTime(LocalDateTime.now())
                .build();
        
        taskMap.put(requestId, task);
        cacheTask(task);
        
        executeSchedulingAsync(requestId, request);
        
        return requestId;
    }

    public SchedulingTaskStatus getTaskStatus(String requestId) {
        SchedulingTask task = taskMap.get(requestId);
        if (task == null) {
            task = getCachedTask(requestId);
            if (task == null) {
                return null;
            }
        }
        return task.getStatus();
    }

    public SchedulingComputeResponse getTaskResult(String requestId) {
        String key = RESULT_CACHE_PREFIX + requestId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof SchedulingComputeResponse) {
            return (SchedulingComputeResponse) cached;
        }
        return null;
    }

    public SchedulingTask getTaskInfo(String requestId) {
        SchedulingTask task = taskMap.get(requestId);
        if (task == null) {
            task = getCachedTask(requestId);
        }
        return task;
    }

    public boolean cancelTask(String requestId) {
        SchedulingTask task = taskMap.get(requestId);
        if (task == null) {
            task = getCachedTask(requestId);
            if (task == null) {
                return false;
            }
        }
        
        if (task.getStatus() == SchedulingTaskStatus.RUNNING) {
            task.setStatus(SchedulingTaskStatus.CANCELLED);
            taskMap.put(requestId, task);
            cacheTask(task);
            return true;
        }
        
        return false;
    }

    @Async
    public void executeSchedulingAsync(String requestId, SchedulingComputeRequest request) {
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            updateTaskStatus(requestId, SchedulingTaskStatus.RUNNING, startTime, null, null);
            
            SchedulingProblem problem = buildSchedulingProblem(request);
            
            long timeout = request.getTimeoutSeconds() != null ? 
                    request.getTimeoutSeconds() * 1000 : 
                    (request.getIsEmergency() != null && request.getIsEmergency() ? 
                            DEFAULT_EMERGENCY_TIMEOUT : DEFAULT_NORMAL_TIMEOUT);
            
            SchedulingSolution solution = solve(problem, timeout, request.getIsEmergency());
            
            SchedulingComputeResponse response = buildResponse(solution, requestId, startTime);
            response.setStatus(SchedulingTaskStatus.COMPLETED.getDesc());
            
            updateTaskStatus(requestId, SchedulingTaskStatus.COMPLETED, startTime, LocalDateTime.now(), response);
            cacheResult(requestId, response);
            
            log.info("调度计算完成, requestId: {}, 耗时: {}ms, 硬分数: {}, 软分数: {}",
                    requestId, response.getDurationMs(), response.getHardScore(), response.getSoftScore());
            
        } catch (Exception e) {
            log.error("调度计算执行失败, requestId: {}", requestId, e);
            
            SchedulingComputeResponse response = SchedulingComputeResponse.builder()
                    .requestId(requestId)
                    .status(SchedulingTaskStatus.FAILED.getDesc())
                    .errorMessage(e.getMessage())
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .build();
            
            updateTaskStatus(requestId, SchedulingTaskStatus.FAILED, startTime, LocalDateTime.now(), response);
            cacheResult(requestId, response);
        }
    }

    private SchedulingProblem buildSchedulingProblem(SchedulingComputeRequest request) {
        List<Resource> resources = new ArrayList<>();
        if (request.getResources() != null) {
            for (SchedulingComputeRequest.ResourceInfo info : request.getResources()) {
                Resource resource = Resource.builder()
                        .id(info.getId())
                        .name(info.getName())
                        .resourceType(info.getType())
                        .longitude(info.getLongitude())
                        .latitude(info.getLatitude())
                        .skills(info.getSkills())
                        .serviceAreas(info.getServiceAreas())
                        .status(info.getStatus())
                        .currentLoad(info.getCurrentLoad())
                        .maxLoad(info.getMaxLoad())
                        .historicalScore(info.getHistoricalScore())
                        .isAvailable(info.getIsAvailable())
                        .allowReuse(info.getAllowReuse())
                        .vehicleType(info.getVehicleType())
                        .vehicleCapacity(info.getVehicleCapacity())
                        .build();
                resources.add(resource);
            }
        }

        List<Order> orders = new ArrayList<>();
        if (request.getOrders() != null) {
            for (SchedulingComputeRequest.OrderInfo info : request.getOrders()) {
                Order order = Order.builder()
                        .id(info.getId())
                        .orderNo(info.getOrderNo())
                        .longitude(info.getLongitude())
                        .latitude(info.getLatitude())
                        .address(info.getAddress())
                        .requiredSkills(info.getRequiredSkills())
                        .requiredVehicleType(info.getRequiredVehicleType())
                        .requiredCapacity(info.getRequiredCapacity())
                        .isEmergency(info.getIsEmergency())
                        .urgencyLevel(info.getUrgencyLevel())
                        .priority(info.getPriority())
                        .build();
                orders.add(order);
            }
        }

        List<Assignment> assignments = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            Assignment assignment = Assignment.builder()
                    .id((long) i)
                    .order(orders.get(i))
                    .build();
            assignments.add(assignment);
        }

        return SchedulingProblem.builder()
                .id(UUID.randomUUID().getMostSignificantBits())
                .name("Scheduling-" + LocalDateTime.now())
                .resources(resources)
                .assignments(assignments)
                .isEmergency(request.getIsEmergency() != null && request.getIsEmergency())
                .build();
    }

    private SchedulingSolution solve(SchedulingProblem problem, long timeoutMs, boolean isEmergency) {
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(SchedulingSolution.class)
                .withEntityClasses(Assignment.class)
                .withEasyScoreCalculatorClass(SchedulingScoreCalculator.class)
                .withTerminationSpentLimit(Duration.ofMillis(timeoutMs));
        
        if (!isEmergency) {
            solverConfig.withTerminationUnimprovedSecondsSpentLimit(5);
        }

        SolverFactory<SchedulingSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<SchedulingSolution> solver = solverFactory.buildSolver();

        return solver.solve(problem);
    }

    private SchedulingComputeResponse buildResponse(SchedulingSolution solution, 
                                                     String requestId,
                                                     LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

        List<SchedulingComputeResponse.AssignmentResult> assignmentResults = new ArrayList<>();
        double totalDistance = 0.0;
        double totalTime = 0.0;
        int unassignedCount = 0;

        if (solution.getAssignments() != null) {
            for (Assignment assignment : solution.getAssignments()) {
                if (assignment.getResource() == null) {
                    unassignedCount++;
                    continue;
                }

                SchedulingComputeResponse.AssignmentResult result = SchedulingComputeResponse.AssignmentResult.builder()
                        .orderId(assignment.getOrder().getId())
                        .orderNo(assignment.getOrder().getOrderNo())
                        .resourceId(assignment.getResource().getId())
                        .resourceName(assignment.getResource().getName())
                        .distance(assignment.getDistance())
                        .estimatedTravelTime(assignment.getEstimatedTravelTime())
                        .waitingTime(assignment.getWaitingTime())
                        .score(assignment.getScore())
                        .scoreDetails(assignment.getScoreDetails())
                        .startTime(assignment.getStartTime())
                        .endTime(assignment.getEndTime())
                        .build();
                
                assignmentResults.add(result);
                
                if (assignment.getDistance() != null) {
                    totalDistance += assignment.getDistance();
                }
                if (assignment.getEstimatedTravelTime() != null) {
                    totalTime += assignment.getEstimatedTravelTime();
                }
            }
        }

        return SchedulingComputeResponse.builder()
                .requestId(requestId)
                .status(SchedulingTaskStatus.COMPLETED.getDesc())
                .hardScore(solution.getScore() != null ? solution.getScore().hardScore() : 0)
                .softScore(solution.getScore() != null ? solution.getScore().softScore() : 0)
                .totalDistance(totalDistance)
                .estimatedTotalTime(totalTime)
                .unassignedOrders(unassignedCount)
                .solutionDetails(solution.getSolutionDetails())
                .assignments(assignmentResults)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(durationMs)
                .build();
    }

    private void updateTaskStatus(String requestId, SchedulingTaskStatus status,
                                   LocalDateTime startTime, LocalDateTime endTime,
                                   SchedulingComputeResponse response) {
        SchedulingTask task = taskMap.get(requestId);
        if (task == null) {
            task = SchedulingTask.builder()
                    .requestId(requestId)
                    .build();
        }
        
        task.setStatus(status);
        if (startTime != null) {
            task.setStartTime(startTime);
        }
        if (endTime != null) {
            task.setEndTime(endTime);
            task.setDurationMs(java.time.Duration.between(task.getStartTime(), endTime).toMillis());
        }
        if (response != null) {
            task.setResult(response);
        }
        
        taskMap.put(requestId, task);
        cacheTask(task);
    }

    private void cacheTask(SchedulingTask task) {
        String key = TASK_CACHE_PREFIX + task.getRequestId();
        redisTemplate.opsForValue().set(key, task);
    }

    private SchedulingTask getCachedTask(String requestId) {
        String key = TASK_CACHE_PREFIX + requestId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof SchedulingTask) {
            return (SchedulingTask) cached;
        }
        return null;
    }

    private void cacheResult(String requestId, SchedulingComputeResponse response) {
        String key = RESULT_CACHE_PREFIX + requestId;
        redisTemplate.opsForValue().set(key, response);
    }

    public Map<String, Object> getServiceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long pendingCount = taskMap.values().stream()
                .filter(t -> t.getStatus() == SchedulingTaskStatus.PENDING)
                .count();
        long runningCount = taskMap.values().stream()
                .filter(t -> t.getStatus() == SchedulingTaskStatus.RUNNING)
                .count();
        long completedCount = taskMap.values().stream()
                .filter(t -> t.getStatus() == SchedulingTaskStatus.COMPLETED)
                .count();
        long failedCount = taskMap.values().stream()
                .filter(t -> t.getStatus() == SchedulingTaskStatus.FAILED)
                .count();
        
        metrics.put("totalTasks", taskMap.size());
        metrics.put("pendingCount", pendingCount);
        metrics.put("runningCount", runningCount);
        metrics.put("completedCount", completedCount);
        metrics.put("failedCount", failedCount);
        
        return metrics;
    }
}
