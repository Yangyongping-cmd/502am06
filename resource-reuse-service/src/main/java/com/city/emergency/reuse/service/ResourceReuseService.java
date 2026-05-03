package com.city.emergency.reuse.service;

import com.city.emergency.reuse.dto.ResourceReuseDTO;
import com.city.emergency.reuse.enums.ResourceModeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceReuseService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private final Map<Long, ResourceReuseDTO> resourceReuseMap = new ConcurrentHashMap<>();
    
    private static final String REUSE_CACHE_PREFIX = "reuse:resource:";
    private static final String PEAK_HOUR_KEY = "reuse:peak-hour";
    
    private static final LocalTime PEAK_MORNING_START = LocalTime.of(7, 0);
    private static final LocalTime PEAK_MORNING_END = LocalTime.of(9, 0);
    private static final LocalTime PEAK_EVENING_START = LocalTime.of(17, 0);
    private static final LocalTime PEAK_EVENING_END = LocalTime.of(19, 0);
    
    private static final List<String> RESCUE_SKILLS = Arrays.asList(
            "FIRST_AID", "FIRE_FIGHTING", "TOWING", "MEDICAL_TRANSPORT", "ROAD_RESCUE"
    );
    
    private static final List<String> FREIGHT_COMPATIBLE_SKILLS = Arrays.asList(
            "FREIGHT_SMALL", "FREIGHT_LARGE", "EQUIPMENT_TRANSPORT", "MOVING", "PICKUP"
    );

    public ResourceReuseDTO getResourceReuseStatus(Long resourceId) {
        ResourceReuseDTO dto = resourceReuseMap.get(resourceId);
        if (dto == null) {
            dto = createDefaultResourceReuse(resourceId);
            resourceReuseMap.put(resourceId, dto);
        }
        
        updatePeakHourStatus(dto);
        calculateUtilizationRate(dto);
        
        return dto;
    }

    public List<ResourceReuseDTO> getAvailableReuseResources(List<Long> resourceIds) {
        List<ResourceReuseDTO> available = new ArrayList<>();
        
        for (Long resourceId : resourceIds) {
            ResourceReuseDTO dto = getResourceReuseStatus(resourceId);
            
            if (isEligibleForReuse(dto)) {
                available.add(dto);
            }
        }
        
        available.sort((a, b) -> {
            int scoreCompare = Double.compare(
                    calculateReuseScore(b), 
                    calculateReuseScore(a)
            );
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return Double.compare(a.getReuseEfficiency() == null ? 0 : a.getReuseEfficiency(),
                                   b.getReuseEfficiency() == null ? 0 : b.getReuseEfficiency());
        });
        
        return available;
    }

    public ResourceReuseDTO switchToReuseMode(Long resourceId, Long orderId, String orderType) {
        ResourceReuseDTO dto = getResourceReuseStatus(resourceId);
        
        if (dto.getCurrentMode() == ResourceModeEnum.RESCUE_ONLY) {
            throw new IllegalArgumentException("该资源不支持复用模式");
        }
        
        if (dto.getCurrentMode() == ResourceModeEnum.REUSE_ACTIVE) {
            throw new IllegalStateException("该资源已处于复用激活状态");
        }
        
        dto.setCurrentMode(ResourceModeEnum.SWITCHING);
        dto.setModeSwitchTime(LocalDateTime.now());
        dto.setStatusMessage("正在切换到复用模式...");
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        dto.setCurrentMode(ResourceModeEnum.REUSE_ACTIVE);
        dto.setCurrentOrderId(orderId);
        dto.setCurrentOrderType(orderType);
        dto.setLastReuseTime(LocalDateTime.now());
        dto.setStatusMessage("已切换到货运模式，正在执行同城货运任务");
        
        if (dto.getTotalReuseCount() == null) {
            dto.setTotalReuseCount(0);
        }
        dto.setTotalReuseCount(dto.getTotalReuseCount() + 1);
        
        cacheResourceReuse(dto);
        
        log.info("资源 {} 切换到复用模式, 订单ID: {}, 订单类型: {}", 
                 resourceId, orderId, orderType);
        
        return dto;
    }

    public ResourceReuseDTO switchToRescueMode(Long resourceId) {
        ResourceReuseDTO dto = getResourceReuseStatus(resourceId);
        
        if (dto.getCurrentMode() != ResourceModeEnum.REUSE_ACTIVE) {
            return dto;
        }
        
        dto.setCurrentMode(ResourceModeEnum.SWITCHING);
        dto.setStatusMessage("正在切换回救援模式...");
        
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        dto.setCurrentMode(ResourceModeEnum.REUSE_ALLOWED);
        dto.setCurrentOrderId(null);
        dto.setCurrentOrderType(null);
        dto.setModeSwitchTime(LocalDateTime.now());
        dto.setLastRescueTime(LocalDateTime.now());
        dto.setStatusMessage("已切换回救援模式，随时响应紧急任务");
        
        if (dto.getTotalRescueCount() == null) {
            dto.setTotalRescueCount(0);
        }
        dto.setTotalRescueCount(dto.getTotalRescueCount() + 1);
        
        calculateReuseEfficiency(dto);
        cacheResourceReuse(dto);
        
        log.info("资源 {} 切换回救援模式", resourceId);
        
        return dto;
    }

    public ResourceReuseDTO setReusePermission(Long resourceId, Boolean allowReuse) {
        ResourceReuseDTO dto = getResourceReuseStatus(resourceId);
        
        dto.setAllowReuse(allowReuse);
        
        if (!allowReuse) {
            if (dto.getCurrentMode() == ResourceModeEnum.REUSE_ACTIVE) {
                switchToRescueMode(resourceId);
            }
            dto.setCurrentMode(ResourceModeEnum.RESCUE_ONLY);
            dto.setStatusMessage("已设置为仅救援模式");
        } else {
            if (dto.getCurrentMode() == ResourceModeEnum.RESCUE_ONLY) {
                dto.setCurrentMode(ResourceModeEnum.REUSE_ALLOWED);
                dto.setStatusMessage("已设置为可复用模式");
            }
        }
        
        cacheResourceReuse(dto);
        
        log.info("资源 {} 复用权限设置为: {}", resourceId, allowReuse);
        
        return dto;
    }

    public List<ResourceReuseDTO> getReuseStats(List<Long> resourceIds) {
        List<ResourceReuseDTO> stats = new ArrayList<>();
        
        for (Long resourceId : resourceIds) {
            ResourceReuseDTO dto = getResourceReuseStatus(resourceId);
            calculateReuseEfficiency(dto);
            calculateUtilizationRate(dto);
            stats.add(dto);
        }
        
        return stats;
    }

    public Map<String, Object> getOverallReuseMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        long totalResources = resourceReuseMap.size();
        long rescueOnlyCount = resourceReuseMap.values().stream()
                .filter(dto -> dto.getCurrentMode() == ResourceModeEnum.RESCUE_ONLY)
                .count();
        long reuseAllowedCount = resourceReuseMap.values().stream()
                .filter(dto -> dto.getCurrentMode() == ResourceModeEnum.REUSE_ALLOWED)
                .count();
        long reuseActiveCount = resourceReuseMap.values().stream()
                .filter(dto -> dto.getCurrentMode() == ResourceModeEnum.REUSE_ACTIVE)
                .count();
        
        int totalRescueCount = resourceReuseMap.values().stream()
                .mapToInt(dto -> dto.getTotalRescueCount() == null ? 0 : dto.getTotalRescueCount())
                .sum();
        int totalReuseCount = resourceReuseMap.values().stream()
                .mapToInt(dto -> dto.getTotalReuseCount() == null ? 0 : dto.getTotalReuseCount())
                .sum();
        
        double avgUtilizationRate = resourceReuseMap.values().stream()
                .mapToDouble(dto -> dto.getUtilizationRate() == null ? 0 : dto.getUtilizationRate())
                .average()
                .orElse(0.0);
        
        double avgReuseEfficiency = resourceReuseMap.values().stream()
                .mapToDouble(dto -> dto.getReuseEfficiency() == null ? 0 : dto.getReuseEfficiency())
                .average()
                .orElse(0.0);
        
        metrics.put("totalResources", totalResources);
        metrics.put("rescueOnlyCount", rescueOnlyCount);
        metrics.put("reuseAllowedCount", reuseAllowedCount);
        metrics.put("reuseActiveCount", reuseActiveCount);
        metrics.put("totalRescueCount", totalRescueCount);
        metrics.put("totalReuseCount", totalReuseCount);
        metrics.put("avgUtilizationRate", String.format("%.2f%%", avgUtilizationRate * 100));
        metrics.put("avgReuseEfficiency", String.format("%.2f%%", avgReuseEfficiency * 100));
        metrics.put("isPeakHour", isPeakHourNow());
        metrics.put("peakHourReuseRatio", calculatePeakHourReuseRatio());
        
        return metrics;
    }

    private ResourceReuseDTO createDefaultResourceReuse(Long resourceId) {
        ResourceReuseDTO dto = new ResourceReuseDTO();
        dto.setResourceId(resourceId);
        dto.setResourceName("Resource-" + resourceId);
        dto.setCurrentMode(ResourceModeEnum.RESCUE_ONLY);
        dto.setAllowReuse(false);
        dto.setCompatibleSkills(new ArrayList<>());
        dto.setTotalRescueCount(0);
        dto.setTotalReuseCount(0);
        dto.setReuseEfficiency(0.0);
        dto.setUtilizationRate(0.0);
        dto.setPeakHourReuseRatio(50);
        dto.setStatusMessage("默认救援模式");
        
        return dto;
    }

    private void updatePeakHourStatus(ResourceReuseDTO dto) {
        boolean isPeakHour = isPeakHourNow();
        dto.setIsInPeakHour(isPeakHour);
    }

    private boolean isPeakHourNow() {
        LocalTime now = LocalTime.now();
        return (now.isAfter(PEAK_MORNING_START) && now.isBefore(PEAK_MORNING_END)) ||
               (now.isAfter(PEAK_EVENING_START) && now.isBefore(PEAK_EVENING_END));
    }

    private boolean isEligibleForReuse(ResourceReuseDTO dto) {
        if (dto.getAllowReuse() == null || !dto.getAllowReuse()) {
            return false;
        }
        
        if (dto.getCurrentMode() == ResourceModeEnum.RESCUE_ONLY) {
            return false;
        }
        
        if (dto.getCurrentMode() == ResourceModeEnum.REUSE_ACTIVE) {
            return false;
        }
        
        return true;
    }

    private double calculateReuseScore(ResourceReuseDTO dto) {
        double score = 0.0;
        
        if (dto.getReuseEfficiency() != null) {
            score += dto.getReuseEfficiency() * 30;
        }
        
        if (dto.getUtilizationRate() != null) {
            score += dto.getUtilizationRate() * 20;
        }
        
        if (dto.getCompatibleSkills() != null && !dto.getCompatibleSkills().isEmpty()) {
            score += dto.getCompatibleSkills().size() * 10;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (dto.getLastReuseTime() != null) {
            long hoursSinceLastReuse = java.time.Duration.between(dto.getLastReuseTime(), now).toHours();
            if (hoursSinceLastReuse < 24) {
                score += (24 - hoursSinceLastReuse) * 0.5;
            }
        }
        
        return score;
    }

    private void calculateUtilizationRate(ResourceReuseDTO dto) {
        int totalCount = (dto.getTotalRescueCount() == null ? 0 : dto.getTotalRescueCount()) +
                         (dto.getTotalReuseCount() == null ? 0 : dto.getTotalReuseCount());
        
        if (totalCount == 0) {
            dto.setUtilizationRate(0.0);
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = dto.getCreateTime() != null ? dto.getCreateTime() : 
                                  now.minusDays(30);
        
        long days = java.time.Duration.between(startDate, now).toDays() + 1;
        double avgDailyTasks = (double) totalCount / days;
        
        double utilizationRate = Math.min(1.0, avgDailyTasks / 10.0);
        dto.setUtilizationRate(utilizationRate);
    }

    private void calculateReuseEfficiency(ResourceReuseDTO dto) {
        int rescueCount = dto.getTotalRescueCount() == null ? 0 : dto.getTotalRescueCount();
        int reuseCount = dto.getTotalReuseCount() == null ? 0 : dto.getTotalReuseCount();
        int totalCount = rescueCount + reuseCount;
        
        if (totalCount == 0) {
            dto.setReuseEfficiency(0.0);
            return;
        }
        
        double efficiency = (double) reuseCount / totalCount;
        dto.setReuseEfficiency(efficiency);
    }

    private int calculatePeakHourReuseRatio() {
        long reuseActiveCount = resourceReuseMap.values().stream()
                .filter(dto -> dto.getCurrentMode() == ResourceModeEnum.REUSE_ACTIVE)
                .count();
        
        long totalReusableCount = resourceReuseMap.values().stream()
                .filter(dto -> dto.getAllowReuse() != null && dto.getAllowReuse())
                .count();
        
        if (totalReusableCount == 0) {
            return 0;
        }
        
        return (int) ((double) reuseActiveCount / totalReusableCount * 100);
    }

    private void cacheResourceReuse(ResourceReuseDTO dto) {
        String key = REUSE_CACHE_PREFIX + dto.getResourceId();
        redisTemplate.opsForValue().set(key, dto);
    }
}
