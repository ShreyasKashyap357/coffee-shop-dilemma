package com.hackathon.coffee_shop_dilemma.model;

public class DrinkStats {

    private final int count;
    private final double avgWait;

    public DrinkStats(int count, double avgWait) {
        this.count = count;
        this.avgWait = avgWait;
    }

    public int getCount() {
        return count;
    }

    public double getAvgWait() {
        return avgWait;
    }
}