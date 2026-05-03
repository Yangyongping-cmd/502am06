package com.city.emergency.reuse.controller;

import com.city.emergency.common.response.Result;
import com.city.emergency.reuse.dto.ResourceReuseDTO;
import com.city.emergency.reuse.service.ResourceReuseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "资源复用服务", description = "救援-家政跨领域资源弹性复用API")
@RestController
@RequestMapping("/api/reuse")
@RequiredArgsConstructor
public class ResourceReuseController {

    private final ResourceReuseService resourceReuseService;

    @Operation(summary = "获取资源复用状态", description = "获取指定资源的复用模式和状态")
    @GetMapping("/status/{resourceId}")
    public Result<ResourceReuseDTO> getResourceReuseStatus(@PathVariable Long resourceId) {
        ResourceReuseDTO status = resourceReuseService.getResourceReuseStatus(resourceId);
        return Result.success(status);
    }

    @Operation(summary = "获取可用复用资源", description = "获取当前可用于家政任务的救援资源列表")
    @PostMapping("/available")
    public Result<List<ResourceReuseDTO>> getAvailableReuseResources(@RequestBody List<Long> resourceIds) {
        List<ResourceReuseDTO> available = resourceReuseService.getAvailableReuseResources(resourceIds);
        return Result.success(available);
    }

    @Operation(summary = "切换到复用模式", description = "将救援资源切换到家政复用模式")
    @PostMapping("/switch-to-reuse/{resourceId}")
    public Result<ResourceReuseDTO> switchToReuseMode(
            @PathVariable Long resourceId,
            @RequestParam Long orderId,
            @RequestParam String orderType) {
        try {
            ResourceReuseDTO result = resourceReuseService.switchToReuseMode(resourceId, orderId, orderType);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "切换回救援模式", description = "将资源从家政复用模式切换回救援模式")
    @PostMapping("/switch-to-rescue/{resourceId}")
    public Result<ResourceReuseDTO> switchToRescueMode(@PathVariable Long resourceId) {
        ResourceReuseDTO result = resourceReuseService.switchToRescueMode(resourceId);
        return Result.success(result);
    }

    @Operation(summary = "设置复用权限", description = "设置资源是否允许进入复用模式")
    @PostMapping("/permission/{resourceId}")
    public Result<ResourceReuseDTO> setReusePermission(
            @PathVariable Long resourceId,
            @RequestParam Boolean allowReuse) {
        ResourceReuseDTO result = resourceReuseService.setReusePermission(resourceId, allowReuse);
        return Result.success(result);
    }

    @Operation(summary = "获取资源复用统计", description = "获取多个资源的复用效率和利用率统计")
    @PostMapping("/stats")
    public Result<List<ResourceReuseDTO>> getReuseStats(@RequestBody List<Long> resourceIds) {
        List<ResourceReuseDTO> stats = resourceReuseService.getReuseStats(resourceIds);
        return Result.success(stats);
    }

    @Operation(summary = "获取整体复用指标", description = "获取平台整体资源复用效率指标")
    @GetMapping("/metrics")
    public Result<Map<String, Object>> getOverallReuseMetrics() {
        Map<String, Object> metrics = resourceReuseService.getOverallReuseMetrics();
        return Result.success(metrics);
    }
}
