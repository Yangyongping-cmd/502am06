package com.city.emergency.location.service;

import com.city.emergency.location.algorithm.KalmanFilter;
import com.city.emergency.location.dto.FusedLocationDTO;
import com.city.emergency.location.dto.LocationDataDTO;
import com.city.emergency.location.enums.LocationSourceEnum;
import com.city.emergency.location.exception.LocationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LocationFusionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<Long, KalmanFilter> kalmanFilterMap = new ConcurrentHashMap<>();
    private final Map<Long, List<LocationDataDTO>> pendingLocationMap = new ConcurrentHashMap<>();

    private static final String LOCATION_CACHE_PREFIX = "location:fused:";
    private static final int MAX_PENDING_LOCATIONS = 10;
    private static final double HIGH_ACCURACY_THRESHOLD = 0.5;
    private static final double FUSION_TIME_WINDOW_SECONDS = 5.0;

    public LocationFusionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addLocationData(LocationDataDTO locationData) {
        if (locationData == null || locationData.getResourceId() == null) {
            throw new LocationException("定位数据不能为空");
        }

        Long resourceId = locationData.getResourceId();
        
        pendingLocationMap.compute(resourceId, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(locationData);
            if (v.size() > MAX_PENDING_LOCATIONS) {
                v = v.subList(v.size() - MAX_PENDING_LOCATIONS, v.size());
            }
            return v;
        });

        tryFuseAndUpdate(resourceId);
    }

    private void tryFuseAndUpdate(Long resourceId) {
        List<LocationDataDTO> pendingLocations = pendingLocationMap.get(resourceId);
        if (pendingLocations == null || pendingLocations.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<LocationDataDTO> validLocations = pendingLocations.stream()
                .filter(loc -> ChronoUnit.SECONDS.between(loc.getTimestamp(), now) <= FUSION_TIME_WINDOW_SECONDS)
                .sorted(Comparator.comparing(LocationDataDTO::getTimestamp))
                .toList();

        if (validLocations.isEmpty()) {
            return;
        }

        Set<Integer> sourceTypes = new HashSet<>();
        validLocations.forEach(loc -> sourceTypes.add(loc.getSource().getCode()));

        FusedLocationDTO fusedLocation = fuseLocations(validLocations, resourceId);
        fusedLocation.setFusedSources(sourceTypes.size());

        applyKalmanFilter(resourceId, fusedLocation);

        cacheFusedLocation(fusedLocation);

        log.debug("资源 {} 定位融合完成, 精度: {}米, 置信度: {}", 
                resourceId, fusedLocation.getAccuracy(), fusedLocation.getConfidence());
    }

    private FusedLocationDTO fuseLocations(List<LocationDataDTO> locations, Long resourceId) {
        if (locations.size() == 1) {
            LocationDataDTO loc = locations.get(0);
            return FusedLocationDTO.builder()
                    .resourceId(resourceId)
                    .longitude(loc.getLongitude())
                    .latitude(loc.getLatitude())
                    .altitude(loc.getAltitude())
                    .speed(loc.getSpeed())
                    .direction(loc.getDirection())
                    .accuracy(loc.getAccuracy())
                    .timestamp(loc.getTimestamp())
                    .isHighAccurate(loc.getAccuracy() <= HIGH_ACCURACY_THRESHOLD)
                    .confidence(calculateConfidence(loc.getSource(), loc.getAccuracy()))
                    .build();
        }

        double totalWeight = 0.0;
        double weightedLongitude = 0.0;
        double weightedLatitude = 0.0;
        double weightedAltitude = 0.0;
        double weightedSpeed = 0.0;
        double weightedDirection = 0.0;
        double bestAccuracy = Double.MAX_VALUE;
        LocalDateTime latestTimestamp = LocalDateTime.MIN;

        for (LocationDataDTO loc : locations) {
            double weight = calculateWeight(loc);
            totalWeight += weight;

            weightedLongitude += loc.getLongitude() * weight;
            weightedLatitude += loc.getLatitude() * weight;
            if (loc.getAltitude() != null) {
                weightedAltitude += loc.getAltitude() * weight;
            }
            if (loc.getSpeed() != null) {
                weightedSpeed += loc.getSpeed() * weight;
            }
            if (loc.getDirection() != null) {
                weightedDirection += loc.getDirection() * weight;
            }

            if (loc.getAccuracy() < bestAccuracy) {
                bestAccuracy = loc.getAccuracy();
            }
            if (loc.getTimestamp().isAfter(latestTimestamp)) {
                latestTimestamp = loc.getTimestamp();
            }
        }

        if (totalWeight == 0) {
            totalWeight = 1.0;
        }

        double fusedAccuracy = calculateFusedAccuracy(locations);
        double confidence = calculateOverallConfidence(locations, fusedAccuracy);

        return FusedLocationDTO.builder()
                .resourceId(resourceId)
                .longitude(weightedLongitude / totalWeight)
                .latitude(weightedLatitude / totalWeight)
                .altitude(weightedAltitude > 0 ? weightedAltitude / totalWeight : null)
                .speed(weightedSpeed > 0 ? weightedSpeed / totalWeight : null)
                .direction(weightedDirection > 0 ? weightedDirection / totalWeight : null)
                .accuracy(fusedAccuracy)
                .timestamp(latestTimestamp)
                .isHighAccurate(fusedAccuracy <= HIGH_ACCURACY_THRESHOLD)
                .confidence(confidence)
                .build();
    }

    private double calculateWeight(LocationDataDTO location) {
        double sourceWeight = getSourceWeight(location.getSource());
        double accuracyWeight = 1.0 / (location.getAccuracy() + 0.001);
        double recencyWeight = calculateRecencyWeight(location.getTimestamp());
        
        return sourceWeight * accuracyWeight * recencyWeight;
    }

    private double getSourceWeight(LocationSourceEnum source) {
        return switch (source) {
            case GPS, BEIDOU -> 1.0;
            case INERTIAL -> 0.8;
            case WIFI -> 0.5;
            case CELL -> 0.3;
        };
    }

    private double calculateRecencyWeight(LocalDateTime timestamp) {
        long seconds = ChronoUnit.SECONDS.between(timestamp, LocalDateTime.now());
        return Math.exp(-seconds / 10.0);
    }

    private double calculateFusedAccuracy(List<LocationDataDTO> locations) {
        if (locations.isEmpty()) {
            return 100.0;
        }

        double totalInverseVariance = 0.0;
        for (LocationDataDTO loc : locations) {
            double variance = loc.getAccuracy() * loc.getAccuracy();
            totalInverseVariance += 1.0 / variance;
        }

        return Math.sqrt(1.0 / totalInverseVariance);
    }

    private double calculateConfidence(LocationSourceEnum source, double accuracy) {
        double sourceConfidence = switch (source) {
            case GPS, BEIDOU -> 0.95;
            case INERTIAL -> 0.85;
            case WIFI -> 0.70;
            case CELL -> 0.50;
        };

        double accuracyFactor = Math.exp(-accuracy * 2.0);
        
        return sourceConfidence * (0.5 + 0.5 * accuracyFactor);
    }

    private double calculateOverallConfidence(List<LocationDataDTO> locations, double fusedAccuracy) {
        if (locations.size() == 1) {
            return calculateConfidence(locations.get(0).getSource(), fusedAccuracy);
        }

        Set<LocationSourceEnum> sources = new HashSet<>();
        locations.forEach(loc -> sources.add(loc.getSource()));

        double diversityBonus = Math.min(0.2, sources.size() * 0.05);
        double baseConfidence = 0.7;
        double accuracyFactor = Math.exp(-fusedAccuracy * 2.0);

        return Math.min(0.99, baseConfidence + diversityBonus + (0.3 * accuracyFactor));
    }

    private void applyKalmanFilter(Long resourceId, FusedLocationDTO fusedLocation) {
        KalmanFilter kf = kalmanFilterMap.computeIfAbsent(resourceId, k -> {
            KalmanFilter newKf = new KalmanFilter();
            newKf.setInitialState(fusedLocation.getLongitude(), fusedLocation.getLatitude());
            return newKf;
        });

        kf.setDeltaTime(1.0);
        kf.predict();
        kf.update(fusedLocation.getLongitude(), fusedLocation.getLatitude());

        fusedLocation.setLongitude(kf.getLongitude());
        fusedLocation.setLatitude(kf.getLatitude());

        double speed = fusedLocation.getSpeed() != null ? fusedLocation.getSpeed() : 0.0;
        double direction = fusedLocation.getDirection() != null ? fusedLocation.getDirection() : 0.0;
        double velocityX = speed * Math.cos(Math.toRadians(direction));
        double velocityY = speed * Math.sin(Math.toRadians(direction));
        
        fusedLocation.setSpeed(Math.sqrt(kf.getVelocityX() * kf.getVelocityX() + 
                                         kf.getVelocityY() * kf.getVelocityY()));
    }

    private void cacheFusedLocation(FusedLocationDTO fusedLocation) {
        String key = LOCATION_CACHE_PREFIX + fusedLocation.getResourceId();
        redisTemplate.opsForValue().set(key, fusedLocation);
    }

    public FusedLocationDTO getFusedLocation(Long resourceId) {
        String key = LOCATION_CACHE_PREFIX + resourceId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof FusedLocationDTO) {
            return (FusedLocationDTO) cached;
        }
        return null;
    }

    public Map<Long, FusedLocationDTO> getBatchFusedLocations(List<Long> resourceIds) {
        Map<Long, FusedLocationDTO> result = new HashMap<>();
        for (Long resourceId : resourceIds) {
            FusedLocationDTO location = getFusedLocation(resourceId);
            if (location != null) {
                result.put(resourceId, location);
            }
        }
        return result;
    }
}
