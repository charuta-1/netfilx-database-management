package com.netflix.controller;

import com.netflix.model.WatchHistory;
import com.netflix.service.WatchHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watch-history")
@CrossOrigin(origins = "*")
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    @Autowired
    public WatchHistoryController(WatchHistoryService watchHistoryService) {
        this.watchHistoryService = watchHistoryService;
    }

    /**
     * Add entry to watch history
     */
    @PostMapping
    public ResponseEntity<?> addToHistory(@RequestBody WatchHistoryRequest request) {
        try {
            watchHistoryService.addToHistory(
                request.getProfileId(),
                request.getTitleId(),
                request.isCompleted()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Added to watch history");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get watch history for profile
     */
    @GetMapping("/{profileId}")
    public ResponseEntity<List<WatchHistory>> getWatchHistory(@PathVariable int profileId) {
        List<WatchHistory> history = watchHistoryService.getWatchHistory(profileId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get watch history with pagination
     */
    @GetMapping("/{profileId}/paged")
    public ResponseEntity<List<WatchHistory>> getWatchHistoryPaged(
            @PathVariable int profileId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        List<WatchHistory> history = watchHistoryService.getWatchHistory(profileId, limit, offset);
        return ResponseEntity.ok(history);
    }

    /**
     * Get completed watch history
     */
    @GetMapping("/{profileId}/completed")
    public ResponseEntity<List<WatchHistory>> getCompletedHistory(@PathVariable int profileId) {
        List<WatchHistory> history = watchHistoryService.getCompletedHistory(profileId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get continue watching list
     */
    @GetMapping("/{profileId}/continue-watching")
    public ResponseEntity<List<WatchHistory>> getContinueWatching(@PathVariable int profileId) {
        List<WatchHistory> history = watchHistoryService.getContinueWatching(profileId);
        return ResponseEntity.ok(history);
    }

    /**
     * Update completion status
     */
    @PutMapping("/{watchId}/completion")
    public ResponseEntity<?> updateCompletionStatus(
            @PathVariable long watchId,
            @RequestBody CompletionUpdateRequest request) {

        try {
            watchHistoryService.updateCompletionStatus(watchId, request.isCompleted());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Completion status updated");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get latest watch entry for a title
     */
    @GetMapping("/{profileId}/title/{titleId}/latest")
    public ResponseEntity<WatchHistory> getLatestWatch(@PathVariable int profileId, @PathVariable int titleId) {
        WatchHistory history = watchHistoryService.getLatestWatch(profileId, titleId);
        if (history == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(history);
    }

    /**
     * Clean old watch history
     */
    @DeleteMapping("/{profileId}/cleanup")
    public ResponseEntity<?> cleanupHistory(
            @PathVariable int profileId,
            @RequestParam(defaultValue = "90") int daysOld) {

        watchHistoryService.cleanOldHistory(profileId, daysOld);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Old history cleaned up");

        return ResponseEntity.ok(response);
    }

    // Request DTOs
    public static class WatchHistoryRequest {
        private int profileId;
        private int titleId;
        private boolean completed;

        // Getters and setters
        public int getProfileId() { return profileId; }
        public void setProfileId(int profileId) { this.profileId = profileId; }
        public int getTitleId() { return titleId; }
        public void setTitleId(int titleId) { this.titleId = titleId; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }

    public static class CompletionUpdateRequest {
        private boolean completed;

        // Getters and setters
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }
}