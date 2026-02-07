package com.hackathon.coffee_shop_dilemma.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Barista {
    private Long id;
    private double currentWorkload = 0.0; // total prep minutes assigned
    private LocalDateTime availableTime = LocalDateTime.now();
}