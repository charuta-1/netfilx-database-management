package com.netflix.model;

import java.time.LocalDateTime;

public class WatchHistory {
    private long watchId;
    private int profileId;
    private int titleId;
    private LocalDateTime watchedAt;
    private boolean isCompleted;

    public WatchHistory() {}

    public WatchHistory(long watchId, int profileId, int titleId, LocalDateTime watchedAt, boolean isCompleted) {
        this.watchId = watchId;
        this.profileId = profileId;
        this.titleId = titleId;
        this.watchedAt = watchedAt;
        this.isCompleted = isCompleted;
    }

    // Getters and Setters
    public long getWatchId() {
        return watchId;
    }

    public void setWatchId(long watchId) {
        this.watchId = watchId;
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

    public LocalDateTime getWatchedAt() {
        return watchedAt;
    }

    public void setWatchedAt(LocalDateTime watchedAt) {
        this.watchedAt = watchedAt;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    @Override
    public String toString() {
        return "WatchHistory{" +
                "watchId=" + watchId +
                ", profileId=" + profileId +
                ", titleId=" + titleId +
                ", watchedAt=" + watchedAt +
                ", isCompleted=" + isCompleted +
                '}';
    }
}