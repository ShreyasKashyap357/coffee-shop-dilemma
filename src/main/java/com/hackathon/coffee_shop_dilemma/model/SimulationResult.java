package com.hackathon.coffee_shop_dilemma.model;

import lombok.Data;

@Data
public class SimulationResult {
    private double averageWaitTimeMinutes;
    private double timeoutRatePercent;
    private double workloadBalanceStdDev;
    private double fairnessViolationRatePercent;
}