package com.city.emergency.scheduling.compute.domain;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchedulingScoreCalculator implements EasyScoreCalculator<SchedulingProblem, HardSoftScore> {

    private static final double EARTH_RADIUS = 6371.0;
    
    private static final int HARD_PENALTY_UNASSIGNED = 10000;
    private static final int HARD_PENALTY_SKILL_MISMATCH = 5000;
    private static final int HARD_PENALTY_STATUS_UNAVAILABLE = 8000;
    private static final int HARD_PENALTY_CAPACITY_EXCEED = 4000;

    private static final int SOFT_PENALTY_DISTANCE = 10;
    private static final int SOFT_PENALTY_TIME = 5;
    private static final int SOFT_PENALTY_LOAD_IMBALANCE = 15;
    private static final int SOFT_PENALTY_HISTORICAL_LOW = 8;
    private static final int SOFT_PENALTY_REUSE_CONFLICT = 30;
    private static final int SOFT_PENALTY_VEHICLE_MISMATCH = 20;
    private static final int SOFT_PENALTY_URGENCY = 50;

    private final Map<String, Double> skillWeightMap = new HashMap<>();
    private final Map<Long, Integer> resourceLoadMap = new HashMap<>();

    public SchedulingScoreCalculator() {
        initializeSkillWeights();
    }

    private void initializeSkillWeights() {
        skillWeightMap.put("FIRST_AID", 5.0);
        skillWeightMap.put("FIRE_FIGHTING", 5.0);
        skillWeightMap.put("TOWING", 4.0);
        skillWeightMap.put("ROAD_RESCUE", 4.5);
        skillWeightMap.put("MEDICAL_TRANSPORT", 5.0);
        
        skillWeightMap.put("FREIGHT_SMALL", 3.0);
        skillWeightMap.put("FREIGHT_LARGE", 3.5);
        skillWeightMap.put("FREIGHT_URGENT", 4.0);
        skillWeightMap.put("MOVING", 2.5);
        skillWeightMap.put("EQUIPMENT_TRANSPORT", 3.0);
        skillWeightMap.put("PICKUP", 2.0);
        skillWeightMap.put("DELIVERY", 1.5);
    }

    @Override
    public HardSoftScore calculateScore(SchedulingProblem solution) {
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

        if (!checkCapacityMatch(resource, order)) {
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

        penalty += calculateSkillMatchSoftPenalty(resource, order);
        penalty += calculateHistoricalPenalty(resource);
        penalty += calculateReusePenalty(resource, order);
        penalty += calculateVehicleMatchPenalty(resource, order);
        penalty += calculateUrgencyPenalty(order);

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

    private boolean checkCapacityMatch(Resource resource, Order order) {
        if (order.getRequiredCapacity() == null || order.getRequiredCapacity() <= 0) {
            return true;
        }
        if (resource.getVehicleCapacity() == null) {
            return false;
        }
        return resource.getVehicleCapacity() >= order.getRequiredCapacity();
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
        if (score >= 4.0) return SOFT_PENALTY_HISTORICAL_LOW;
        if (score >= 3.5) return SOFT_PENALTY_HISTORICAL_LOW * 2;
        if (score >= 3.0) return SOFT_PENALTY_HISTORICAL_LOW * 3;
        return SOFT_PENALTY_HISTORICAL_LOW * 5;
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

    private int calculateVehicleMatchPenalty(Resource resource, Order order) {
        if (order.getRequiredVehicleType() == null || order.getRequiredVehicleType().isEmpty()) {
            return 0;
        }
        if (resource.getVehicleType() == null) {
            return SOFT_PENALTY_VEHICLE_MISMATCH;
        }
        if (!resource.getVehicleType().equals(order.getRequiredVehicleType())) {
            return SOFT_PENALTY_VEHICLE_MISMATCH;
        }
        return 0;
    }

    private int calculateUrgencyPenalty(Order order) {
        if (order.getUrgencyLevel() == null) {
            return 0;
        }
        return order.getUrgencyLevel() * SOFT_PENALTY_URGENCY;
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
