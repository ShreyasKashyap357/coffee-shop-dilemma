package com.hackathon.coffee_shop_dilemma.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Barista {
    private Long id;
    private int currentWorkload = 0; // total prep minutes assigned
    private LocalDateTime availableTime = LocalDateTime.now();

    public boolean isAvailable(LocalDateTime now) {
        return !availableTime.isAfter(now);
    }

    public void assignOrder(Order order, LocalDateTime currentTime) {
        int prepTime = order.totalPrepTime();
        order.setAssignedTime(currentTime);

        LocalDateTime finishTime = currentTime.plusMinutes(prepTime);
        order.setCompletionTime(finishTime);
        this.availableTime = finishTime;
        this.currentWorkload += prepTime;
    }
}