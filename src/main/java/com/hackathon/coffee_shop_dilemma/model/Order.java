package com.hackathon.coffee_shop_dilemma.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Data
public class Order {
    private Long id;
    private LocalDateTime arrivalTime;
    private List<DrinkType> drinks;
    private boolean loyaltyGold;
    private LocalDateTime assignedTime;
    private LocalDateTime completionTime;
    private int skippedCount = 0;
    private int priorityScore;
    private String status = "PENDING";
    private int estimatedWait;
    private String reason;
    private boolean completed;
    private boolean forceServe;
    private Long baristaId;
    private long waitTimeMinutes;


    public int getEstimatedWait() {
        return estimatedWait;
    }

    public int totalPrepTime() {
        return drinks.stream()
                .mapToInt(DrinkType::getPrepTimeMinutes)
                .sum();
    }


    public long waitMinutes(LocalDateTime now) {
        return ChronoUnit.MINUTES.between(arrivalTime, now);
    }

    public void setEstimatedWait(int estimatedWait) {
        this.estimatedWait = estimatedWait;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}