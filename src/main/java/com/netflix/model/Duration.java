package com.netflix.model;

public class Duration {
    private int durationId;
    private String unit; // 'min' or 'season'
    private int value;

    public Duration() {}

    public Duration(int durationId, String unit, int value) {
        this.durationId = durationId;
        this.unit = unit;
        this.value = value;
    }

    // Getters and Setters
    public int getDurationId() { return durationId; }
    public void setDurationId(int durationId) { this.durationId = durationId; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    @Override
    public String toString() {
        return "Duration{" +
                "durationId=" + durationId +
                ", unit='" + unit + "'" +
                ", value=" + value +
                '}';
    }
}