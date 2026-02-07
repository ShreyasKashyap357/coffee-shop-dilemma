package com.hackathon.coffee_shop_dilemma.model;

import lombok.Getter;

@Getter
public enum DrinkType {
    COLD_BREW(1, 0.25, 120),
    ESPRESSO(2, 0.20, 150),
    AMERICANO(2, 0.15, 140),
    CAPPUCCINO(4, 0.20, 180),
    LATTE(4, 0.12, 200),
    SPECIALTY(6, 0.08, 250);  // Mocha

    private final int prepTimeMinutes;
    private final double frequency;   // probability portion
    private final int price;

    DrinkType(int prepTimeMinutes, double frequency, int price) {
        this.prepTimeMinutes = prepTimeMinutes;
        this.frequency = frequency;
        this.price = price;
    }
}