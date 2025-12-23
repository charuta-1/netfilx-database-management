package com.netflix.model;

import java.math.BigDecimal;

public class SubscriptionPlan {
    private int planId;
    private String planName;
    private BigDecimal price;
    private String quality;
    private int screensAllowed;

    public SubscriptionPlan() {
    }

    public SubscriptionPlan(int planId, String planName, BigDecimal price, String quality, int screensAllowed) {
        this.planId = planId;
        this.planName = planName;
        this.price = price;
        this.quality = quality;
        this.screensAllowed = screensAllowed;
    }

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public int getScreensAllowed() {
        return screensAllowed;
    }

    public void setScreensAllowed(int screensAllowed) {
        this.screensAllowed = screensAllowed;
    }

    @Override
    public String toString() {
        return "SubscriptionPlan{" +
                "planId=" + planId +
                ", planName='" + planName + '\'' +
                ", price=" + price +
                ", quality='" + quality + '\'' +
                ", screensAllowed=" + screensAllowed +
                '}';
    }
}
