package com.netflix.model;

import java.time.LocalDateTime;

public class UserRating {

    public enum RatingValue {
        THUMBS_UP("thumbs_up"),
        THUMBS_DOWN("thumbs_down");

        private final String value;

        RatingValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static RatingValue fromString(String value) {
            for (RatingValue rating : RatingValue.values()) {
                if (rating.getValue().equals(value)) {
                    return rating;
                }
            }
            throw new IllegalArgumentException("Invalid rating value: " + value);
        }
    }

    private long ratingId;
    private int profileId;
    private int titleId;
    private RatingValue ratingValue;
    private LocalDateTime createdAt;

    public UserRating() {}

    public UserRating(long ratingId, int profileId, int titleId, RatingValue ratingValue, LocalDateTime createdAt) {
        this.ratingId = ratingId;
        this.profileId = profileId;
        this.titleId = titleId;
        this.ratingValue = ratingValue;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public long getRatingId() {
        return ratingId;
    }

    public void setRatingId(long ratingId) {
        this.ratingId = ratingId;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public int getTitleId() {
        return titleId;
    }

    public void setTitleId(int titleId) {
        this.titleId = titleId;
    }

    public RatingValue getRatingValue() {
        return ratingValue;
    }

    public void setRatingValue(RatingValue ratingValue) {
        this.ratingValue = ratingValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UserRating{" +
                "ratingId=" + ratingId +
                ", profileId=" + profileId +
                ", titleId=" + titleId +
                ", ratingValue=" + ratingValue +
                ", createdAt=" + createdAt +
                '}';
    }
}