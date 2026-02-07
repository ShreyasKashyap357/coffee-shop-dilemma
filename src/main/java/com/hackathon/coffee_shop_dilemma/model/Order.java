package com.hackathon.coffee_shop_dilemma.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Order {
    private Long id;
    private LocalDateTime arrivalTime;
    private List<DrinkType> drinks;
    private boolean loyaltyGold;  // renamed from isLoyaltyGold for consistency
    private LocalDateTime assignedTime;
    private LocalDateTime completionTime;
    private int skippedCount = 0;
    private double priorityScore;
    private String status = "PENDING";

    // Transient fields for display only (not persisted)
    private transient int estimatedWait;
    private transient String reason;

    public int getEstimatedWait() {
        return estimatedWait;
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