package com.netflix.model;

import java.time.LocalDateTime;

public class UserProfile {
    private int profileId;
    private int userId;
    private String profileName;
    private String maturityRatingOverride; // optional content rating override
    private LocalDateTime createdAt;

    public UserProfile() {}

    public UserProfile(int profileId, int userId, String profileName, LocalDateTime createdAt) {
        this.profileId = profileId;
        this.userId = userId;
        this.profileName = profileName;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getMaturityRatingOverride() {
        return maturityRatingOverride;
    }

    public void setMaturityRatingOverride(String maturityRatingOverride) {
        this.maturityRatingOverride = maturityRatingOverride;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "profileId=" + profileId +
                ", userId=" + userId +
                ", profileName='" + profileName + '\'' +
                ", maturityRatingOverride='" + maturityRatingOverride + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}