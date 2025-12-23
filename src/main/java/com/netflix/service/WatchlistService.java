package com.netflix.service;

import com.netflix.dao.WatchlistDAO;
import com.netflix.model.Watchlist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WatchlistService {

    private final WatchlistDAO watchlistDAO;

    @Autowired
    public WatchlistService(WatchlistDAO watchlistDAO) {
        this.watchlistDAO = watchlistDAO;
    }

    /**
     * Add title to profile's watchlist
     */
    public void addToWatchlist(int profileId, int titleId) {
        if (watchlistDAO.exists(profileId, titleId)) {
            throw new RuntimeException("Title is already in watchlist");
        }

        watchlistDAO.add(profileId, titleId);
    }

    /**
     * Remove title from profile's watchlist
     */
    public void removeFromWatchlist(int profileId, int titleId) {
        if (!watchlistDAO.exists(profileId, titleId)) {
            throw new RuntimeException("Title is not in watchlist");
        }

        watchlistDAO.remove(profileId, titleId);
    }

    /**
     * Get profile's watchlist
     */
    public List<Watchlist> getWatchlist(int profileId) {
        return watchlistDAO.findByProfileId(profileId);
    }

    /**
     * Check if title is in watchlist
     */
    public boolean isInWatchlist(int profileId, int titleId) {
        return watchlistDAO.exists(profileId, titleId);
    }

    /**
     * Get watchlist count for profile
     */
    public int getWatchlistCount(int profileId) {
        return watchlistDAO.countByProfileId(profileId);
    }

    /**
     * Clear entire watchlist for profile
     */
    public void clearWatchlist(int profileId) {
        watchlistDAO.clearByProfileId(profileId);
    }

    /**
     * Toggle watchlist status (add if not present, remove if present)
     */
    public boolean toggleWatchlist(int profileId, int titleId) {
        if (watchlistDAO.exists(profileId, titleId)) {
            watchlistDAO.remove(profileId, titleId);
            return false; // Removed from watchlist
        } else {
            watchlistDAO.add(profileId, titleId);
            return true; // Added to watchlist
        }
    }
}