package com.city.emergency.location.controller;

import com.city.emergency.common.response.Result;
import com.city.emergency.location.dto.FusedLocationDTO;
import com.city.emergency.location.dto.LocationDataDTO;
import com.city.emergency.location.service.LocationFusionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "定位服务", description = "五模定位融合与卡尔曼滤波API")
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationFusionService locationFusionService;

    @Operation(summary = "上传定位数据", description = "上传来自不同数据源的定位数据进行融合")
    @PostMapping("/upload")
    public Result<Void> uploadLocation(@RequestBody LocationDataDTO locationData) {
        locationFusionService.addLocationData(locationData);
        return Result.success();
    }

    @Operation(summary = "获取融合后的定位", description = "获取经过五模融合和卡尔曼滤波后的精准定位")
    @GetMapping("/{resourceId}")
    public Result<FusedLocationDTO> getFusedLocation(@PathVariable Long resourceId) {
        FusedLocationDTO location = locationFusionService.getFusedLocation(resourceId);
        if (location == null) {
            return Result.error(404, "未找到该资源的定位信息");
        }
        return Result.success(location);
    }

    @Operation(summary = "批量获取融合定位", description = "批量获取多个资源的融合定位信息")
    @PostMapping("/batch")
    public Result<Map<Long, FusedLocationDTO>> getBatchLocations(@RequestBody List<Long> resourceIds) {
        Map<Long, FusedLocationDTO> locations = locationFusionService.getBatchFusedLocations(resourceIds);
        return Result.success(locations);
    }
}
