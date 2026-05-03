package com.city.emergency.scheduling.compute.controller;

import com.city.emergency.common.response.Result;
import com.city.emergency.scheduling.compute.dto.SchedulingComputeRequest;
import com.city.emergency.scheduling.compute.dto.SchedulingComputeResponse;
import com.city.emergency.scheduling.compute.domain.SchedulingTask;
import com.city.emergency.scheduling.compute.enums.SchedulingTaskStatus;
import com.city.emergency.scheduling.compute.service.SchedulingComputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "调度计算服务", description = "独立异步OptaPlanner算法求解服务")
@RestController
@RequestMapping("/api/scheduling-compute")
@RequiredArgsConstructor
public class SchedulingComputeController {

    private final SchedulingComputeService schedulingComputeService;

    @Operation(summary = "提交调度计算请求", description = "异步提交调度计算请求，立即返回请求ID")
    @PostMapping("/submit")
    public Result<String> submitSchedulingRequest(@RequestBody SchedulingComputeRequest request) {
        String requestId = schedulingComputeService.submitSchedulingRequest(request);
        return Result.success(requestId);
    }

    @Operation(summary = "获取任务状态", description = "根据请求ID获取调度计算任务的状态")
    @GetMapping("/status/{requestId}")
    public Result<SchedulingTaskStatus> getTaskStatus(@PathVariable String requestId) {
        SchedulingTaskStatus status = schedulingComputeService.getTaskStatus(requestId);
        if (status == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(status);
    }

    @Operation(summary = "获取任务结果", description = "根据请求ID获取调度计算结果")
    @GetMapping("/result/{requestId}")
    public Result<SchedulingComputeResponse> getTaskResult(@PathVariable String requestId) {
        SchedulingComputeResponse response = schedulingComputeService.getTaskResult(requestId);
        if (response == null) {
            return Result.error(404, "结果不存在或计算尚未完成");
        }
        return Result.success(response);
    }

    @Operation(summary = "获取任务详情", description = "根据请求ID获取调度计算任务的完整信息")
    @GetMapping("/task/{requestId}")
    public Result<SchedulingTask> getTaskInfo(@PathVariable String requestId) {
        SchedulingTask task = schedulingComputeService.getTaskInfo(requestId);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(task);
    }

    @Operation(summary = "取消任务", description = "取消正在执行的调度计算任务")
    @PostMapping("/cancel/{requestId}")
    public Result<Boolean> cancelTask(@PathVariable String requestId) {
        boolean cancelled = schedulingComputeService.cancelTask(requestId);
        return Result.success(cancelled);
    }

    @Operation(summary = "获取服务指标", description = "获取调度计算服务的运行指标")
    @GetMapping("/metrics")
    public Result<Map<String, Object>> getServiceMetrics() {
        Map<String, Object> metrics = schedulingComputeService.getServiceMetrics();
        return Result.success(metrics);
    }
}
