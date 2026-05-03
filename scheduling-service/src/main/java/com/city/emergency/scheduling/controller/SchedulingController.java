package com.city.emergency.scheduling.controller;

import com.city.emergency.common.response.Result;
import com.city.emergency.scheduling.domain.Assignment;
import com.city.emergency.scheduling.domain.Order;
import com.city.emergency.scheduling.domain.Resource;
import com.city.emergency.scheduling.domain.SchedulingSolution;
import com.city.emergency.scheduling.service.SchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "智能调度服务", description = "基于OptaPlanner的18维度智能调度API")
@RestController
@RequestMapping("/api/scheduling")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;

    @Operation(summary = "执行批量调度", description = "对多个订单执行18维度智能调度优化")
    @PostMapping("/batch")
    public Result<SchedulingSolution> solveBatchScheduling(
            @RequestParam(required = false, defaultValue = "false") boolean isEmergency,
            @RequestBody SchedulingRequest request) {
        
        SchedulingSolution solution = schedulingService.solveSchedulingProblem(
                request.getResources(), 
                request.getOrders(), 
                isEmergency
        );
        
        return Result.success(solution);
    }

    @Operation(summary = "获取单个订单最佳分配", description = "为单个订单快速找到最佳资源分配")
    @PostMapping("/quick")
    public Result<Assignment> getQuickAssignment(@RequestBody SchedulingRequest request) {
        if (request.getOrders() == null || request.getOrders().isEmpty()) {
            return Result.error("订单不能为空");
        }
        
        Assignment assignment = schedulingService.getBestAssignment(
                request.getResources(),
                request.getOrders().get(0)
        );
        
        return Result.success(assignment);
    }

    @Operation(summary = "紧急调度", description = "紧急任务的快速调度，5秒内返回结果")
    @PostMapping("/emergency")
    public Result<SchedulingSolution> emergencyScheduling(@RequestBody SchedulingRequest request) {
        SchedulingSolution solution = schedulingService.solveSchedulingProblem(
                request.getResources(),
                request.getOrders(),
                true
        );
        
        return Result.success(solution);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SchedulingRequest {
        private List<Resource> resources;
        private List<Order> orders;
    }
}
