package com.netflix.model;

import java.time.LocalDateTime;

public class Watchlist {
    private int watchlistId;
    private int profileId;
    private int titleId;
    private LocalDateTime addedDate;

    public Watchlist() {}

    public Watchlist(int watchlistId, int profileId, int titleId, LocalDateTime addedDate) {
        this.watchlistId = watchlistId;
        this.profileId = profileId;
        this.titleId = titleId;
        this.addedDate = addedDate;
    }

    // Getters and Setters
    public int getWatchlistId() {
        return watchlistId;
    }

    public void setWatchlistId(int watchlistId) {
        this.watchlistId = watchlistId;
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

    public LocalDateTime getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(LocalDateTime addedDate) {
        this.addedDate = addedDate;
    }

    @Override
    public String toString() {
        return "Watchlist{" +
                "watchlistId=" + watchlistId +
                ", profileId=" + profileId +
                ", titleId=" + titleId +
                ", addedDate=" + addedDate +
                '}';
    }
}