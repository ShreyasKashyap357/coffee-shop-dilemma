package com.hackathon.coffee_shop_dilemma.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SimulationResult {
    private double averageWaitTimeMinutes;
    private double timeoutRatePercent;
    private double workloadBalanceStdDev;
    private double fairnessViolationRatePercent;
    private int numComplaints;
    private List<Order> completedOrders;
    private Map<Long, Integer> baristaWorkloads;
    private Map<String, Integer> drinkCounts;
}