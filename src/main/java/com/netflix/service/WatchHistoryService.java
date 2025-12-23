package com.netflix.service;

import com.netflix.dao.WatchHistoryDAO;
import com.netflix.model.WatchHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WatchHistoryService {

    private final WatchHistoryDAO watchHistoryDAO;

    @Autowired
    public WatchHistoryService(WatchHistoryDAO watchHistoryDAO) {
        this.watchHistoryDAO = watchHistoryDAO;
    }

    /**
     * Add entry to watch history
     */
    public void addToHistory(int profileId, int titleId, boolean isCompleted) {
        WatchHistory history = new WatchHistory();
        history.setProfileId(profileId);
        history.setTitleId(titleId);
        history.setCompleted(isCompleted);

        watchHistoryDAO.add(history);
    }

    /**
     * Get watch history for profile
     */
    public List<WatchHistory> getWatchHistory(int profileId) {
        return watchHistoryDAO.findByProfileId(profileId);
    }

    /**
     * Get watch history with pagination
     */
    public List<WatchHistory> getWatchHistory(int profileId, int limit, int offset) {
        return watchHistoryDAO.findByProfileIdWithLimit(profileId, limit, offset);
    }

    /**
     * Get completed watch history
     */
    public List<WatchHistory> getCompletedHistory(int profileId) {
        return watchHistoryDAO.findCompletedByProfileId(profileId);
    }

    /**
     * Get continue watching list (incomplete items)
     */
    public List<WatchHistory> getContinueWatching(int profileId) {
        return watchHistoryDAO.findByProfileId(profileId).stream()
                .filter(h -> !h.isCompleted())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Update completion status
     */
    public void updateCompletionStatus(long watchId, boolean isCompleted) {
        watchHistoryDAO.updateCompletionStatus(watchId, isCompleted);
    }

    /**
     * Get latest watch entry for a title
     */
    public WatchHistory getLatestWatch(int profileId, int titleId) {
        return watchHistoryDAO.findLatestByProfileAndTitle(profileId, titleId);
    }

    /**
     * Clean old watch history entries
     */
    public void cleanOldHistory(int profileId, int daysOld) {
        watchHistoryDAO.deleteOldEntries(profileId, daysOld);
    }

    /**
     * Get watch history count
     */
    public int getHistoryCount(int profileId) {
        return watchHistoryDAO.countByProfileId(profileId);
    }
}