package com.city.emergency.scheduling.solver;

import com.city.emergency.scheduling.domain.Assignment;
import com.city.emergency.scheduling.domain.Order;
import com.city.emergency.scheduling.domain.Resource;
import com.city.emergency.scheduling.domain.SchedulingSolution;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SchedulingScoreCalculator implements EasyScoreCalculator<SchedulingSolution, HardSoftScore> {

    private static final double EARTH_RADIUS = 6371.0;
    
    private static final int HARD_PENALTY_UNASSIGNED = 10000;
    private static final int HARD_PENALTY_SKILL_MISMATCH = 5000;
    private static final int HARD_PENALTY_STATUS_UNAVAILABLE = 8000;
    private static final int HARD_PENALTY_TIME_EXCEED = 3000;
    private static final int HARD_PENALTY_CAPACITY_EXCEED = 4000;

    private static final int SOFT_PENALTY_DISTANCE = 10;
    private static final int SOFT_PENALTY_TIME = 5;
    private static final int SOFT_PENALTY_TRAFFIC = 20;
    private static final int SOFT_PENALTY_LOAD_IMBALANCE = 15;
    private static final int SOFT_PENALTY_HISTORICAL_LOW = 8;
    private static final int SOFT_PENALTY_AREA_MISMATCH = 25;
    private static final int SOFT_PENALTY_REUSE_CONFLICT = 30;
    private static final int SOFT_PENALTY_WEATHER = 12;
    private static final int SOFT_PENALTY_PEAK_HOUR = 18;
    private static final int SOFT_PENALTY_CUSTOMER_PREF = 10;

    private final Map<String, Double> skillWeightMap = new HashMap<>();
    private final Map<String, Double> trafficConditionMap = new HashMap<>();
    private final Map<Long, Integer> resourceLoadMap = new HashMap<>();
    private Boolean isPeakHour = false;
    private String weatherCondition = "CLEAR";
    private final Map<Long, Long> customerPreferredStaff = new HashMap<>();

    public SchedulingScoreCalculator() {
        initializeSkillWeights();
        initializeTrafficConditions();
    }

    private void initializeSkillWeights() {
        skillWeightMap.put("FIRST_AID", 5.0);
        skillWeightMap.put("FIRE_FIGHTING", 5.0);
        skillWeightMap.put("TOWING", 4.0);
        skillWeightMap.put("REPAIR", 3.0);
        skillWeightMap.put("MOVING", 2.0);
        skillWeightMap.put("CLEANING", 1.5);
        skillWeightMap.put("DELIVERY", 1.0);
    }

    private void initializeTrafficConditions() {
        trafficConditionMap.put("FLUID", 1.0);
        trafficConditionMap.put("MODERATE", 1.5);
        trafficConditionMap.put("CONGESTED", 2.5);
        trafficConditionMap.put("SEVERE", 4.0);
    }

    public void setPeakHour(Boolean peakHour) {
        isPeakHour = peakHour;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public void setCustomerPreferredStaff(Long customerId, Long staffId) {
        customerPreferredStaff.put(customerId, staffId);
    }

    @Override
    public HardSoftScore calculateScore(SchedulingSolution solution) {
        int hardScore = 0;
        int softScore = 0;

        resourceLoadMap.clear();
        solution.getResources().forEach(r -> resourceLoadMap.put(r.getId(), 0));

        for (Assignment assignment : solution.getAssignments()) {
            Resource resource = assignment.getResource();
            Order order = assignment.getOrder();

            if (resource == null) {
                hardScore -= HARD_PENALTY_UNASSIGNED;
                continue;
            }

            hardScore -= calculateHardPenalties(resource, order);
            softScore -= calculateSoftPenalties(assignment, resource, order);
        }

        softScore -= calculateLoadBalancePenalty();

        return HardSoftScore.of(hardScore, softScore);
    }

    private int calculateHardPenalties(Resource resource, Order order) {
        int penalty = 0;

        if (resource.getIsAvailable() == null || !resource.getIsAvailable()) {
            penalty += HARD_PENALTY_STATUS_UNAVAILABLE;
        }

        if (!checkSkillMatch(resource, order)) {
            penalty += HARD_PENALTY_SKILL_MISMATCH;
        }

        if (order.getLatestStartTime() != null && 
            LocalDateTime.now().isAfter(order.getLatestStartTime())) {
            penalty += HARD_PENALTY_TIME_EXCEED;
        }

        if (resource.getCurrentLoad() != null && 
            resource.getMaxLoad() != null &&
            resource.getCurrentLoad() >= resource.getMaxLoad()) {
            penalty += HARD_PENALTY_CAPACITY_EXCEED;
        }

        if (order.getIsEmergency() != null && order.getIsEmergency() &&
            resource.getAllowReuse() != null && !resource.getAllowReuse()) {
            penalty += HARD_PENALTY_SKILL_MISMATCH / 2;
        }

        return penalty;
    }

    private int calculateSoftPenalties(Assignment assignment, Resource resource, Order order) {
        int penalty = 0;

        double distance = calculateDistance(resource, order);
        assignment.setDistance(distance);
        penalty += (int) (distance * SOFT_PENALTY_DISTANCE);

        double estimatedTime = distance / 30.0 * 60;
        assignment.setEstimatedTravelTime(estimatedTime);
        penalty += (int) (estimatedTime * SOFT_PENALTY_TIME);

        penalty += calculateTrafficPenalty(distance);
        penalty += calculateSkillMatchSoftPenalty(resource, order);
        penalty += calculateHistoricalPenalty(resource);
        penalty += calculateAreaPenalty(resource, order);
        penalty += calculateReusePenalty(resource, order);
        penalty += calculateWeatherPenalty();
        penalty += calculatePeakHourPenalty();
        penalty += calculateCustomerPreferencePenalty(resource, order);

        resourceLoadMap.compute(resource.getId(), (k, v) -> (v == null ? 0 : v) + 1);

        return penalty;
    }

    private boolean checkSkillMatch(Resource resource, Order order) {
        if (order.getRequiredSkills() == null || order.getRequiredSkills().isEmpty()) {
            return true;
        }
        if (resource.getSkills() == null || resource.getSkills().isEmpty()) {
            return false;
        }
        return resource.getSkills().containsAll(order.getRequiredSkills());
    }

    private double calculateDistance(Resource resource, Order order) {
        if (resource.getLatitude() == null || resource.getLongitude() == null ||
            order.getLatitude() == null || order.getLongitude() == null) {
            return 10.0;
        }

        double lat1 = Math.toRadians(resource.getLatitude());
        double lon1 = Math.toRadians(resource.getLongitude());
        double lat2 = Math.toRadians(order.getLatitude());
        double lon2 = Math.toRadians(order.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    private int calculateTrafficPenalty(double distance) {
        String trafficLevel = getCurrentTrafficLevel();
        Double factor = trafficConditionMap.getOrDefault(trafficLevel, 1.0);
        return (int) (distance * factor * SOFT_PENALTY_TRAFFIC / 10);
    }

    private String getCurrentTrafficLevel() {
        if (isPeakHour != null && isPeakHour) {
            return "CONGESTED";
        }
        return "MODERATE";
    }

    private int calculateSkillMatchSoftPenalty(Resource resource, Order order) {
        if (order.getRequiredSkills() == null || order.getRequiredSkills().isEmpty()) {
            return 0;
        }
        if (resource.getSkills() == null) {
            return 100;
        }

        int penalty = 0;
        for (String skill : order.getRequiredSkills()) {
            if (!resource.getSkills().contains(skill)) {
                double weight = skillWeightMap.getOrDefault(skill, 1.0);
                penalty += (int) (weight * 20);
            }
        }
        return penalty;
    }

    private int calculateHistoricalPenalty(Resource resource) {
        if (resource.getHistoricalScore() == null) {
            return 0;
        }
        double score = resource.getHistoricalScore();
        if (score >= 4.5) return 0;
        if (score >= 4.0) return (int) (SOFT_PENALTY_HISTORICAL_LOW * 1);
        if (score >= 3.5) return (int) (SOFT_PENALTY_HISTORICAL_LOW * 2);
        if (score >= 3.0) return (int) (SOFT_PENALTY_HISTORICAL_LOW * 3);
        return (int) (SOFT_PENALTY_HISTORICAL_LOW * 5);
    }

    private int calculateAreaPenalty(Resource resource, Order order) {
        if (resource.getServiceAreas() == null || resource.getServiceAreas().isEmpty()) {
            return SOFT_PENALTY_AREA_MISMATCH;
        }
        return 0;
    }

    private int calculateReusePenalty(Resource resource, Order order) {
        if (order.getIsEmergency() != null && order.getIsEmergency()) {
            return 0;
        }
        if (resource.getAllowReuse() != null && resource.getAllowReuse()) {
            return 0;
        }
        return SOFT_PENALTY_REUSE_CONFLICT;
    }

    private int calculateWeatherPenalty() {
        return switch (weatherCondition) {
            case "RAIN" -> (int) (SOFT_PENALTY_WEATHER * 1.5);
            case "SNOW" -> (int) (SOFT_PENALTY_WEATHER * 2.5);
            case "STORM" -> (int) (SOFT_PENALTY_WEATHER * 4.0);
            default -> 0;
        };
    }

    private int calculatePeakHourPenalty() {
        if (isPeakHour != null && isPeakHour) {
            return SOFT_PENALTY_PEAK_HOUR;
        }
        return 0;
    }

    private int calculateCustomerPreferencePenalty(Resource resource, Order order) {
        Long preferredStaffId = customerPreferredStaff.get(order.getCustomerName().hashCode());
        if (preferredStaffId != null && resource.getStaffId() != null &&
            !resource.getStaffId().equals(preferredStaffId)) {
            return SOFT_PENALTY_CUSTOMER_PREF;
        }
        return 0;
    }

    private int calculateLoadBalancePenalty() {
        if (resourceLoadMap.isEmpty()) {
            return 0;
        }

        double average = resourceLoadMap.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        int variance = 0;
        for (int load : resourceLoadMap.values()) {
            variance += (int) Math.pow(load - average, 2);
        }

        return variance * SOFT_PENALTY_LOAD_IMBALANCE;
    }
}
