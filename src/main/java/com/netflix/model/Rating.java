package com.netflix.model;

public class Rating {
    private int ratingId;
    private String code;
    private String description;

    public Rating() {}

    public Rating(int ratingId, String code, String description) {
        this.ratingId = ratingId;
        this.code = code;
        this.description = description;
    }

    // Getters and Setters
    public int getRatingId() { return ratingId; }
    public void setRatingId(int ratingId) { this.ratingId = ratingId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "Rating{" +
                "ratingId=" + ratingId +
                ", code='" + code + "'" +
                ", description='" + description + "'" +
                '}';
    }
}