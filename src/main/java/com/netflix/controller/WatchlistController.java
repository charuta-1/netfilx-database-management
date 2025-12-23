package com.netflix.controller;

import com.netflix.model.Watchlist;
import com.netflix.service.WatchlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watchlist")
@CrossOrigin(origins = "*")
public class WatchlistController {

    private final WatchlistService watchlistService;

    @Autowired
    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    /**
     * Add title to watchlist
     */
    @PostMapping
    public ResponseEntity<?> addToWatchlist(@RequestBody WatchlistRequest request) {
        try {
            watchlistService.addToWatchlist(request.getProfileId(), request.getTitleId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Added to watchlist");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Remove title from watchlist
     */
    @DeleteMapping("/{profileId}/{titleId}")
    public ResponseEntity<?> removeFromWatchlist(@PathVariable int profileId, @PathVariable int titleId) {
        try {
            watchlistService.removeFromWatchlist(profileId, titleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Removed from watchlist");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get profile's watchlist
     */
    @GetMapping("/{profileId}")
    public ResponseEntity<List<Watchlist>> getWatchlist(@PathVariable int profileId) {
        List<Watchlist> watchlist = watchlistService.getWatchlist(profileId);
        return ResponseEntity.ok(watchlist);
    }

    /**
     * Check if title is in watchlist
     */
    @GetMapping("/{profileId}/{titleId}/exists")
    public ResponseEntity<Map<String, Object>> isInWatchlist(@PathVariable int profileId, @PathVariable int titleId) {
        boolean inWatchlist = watchlistService.isInWatchlist(profileId, titleId);

        Map<String, Object> response = new HashMap<>();
        response.put("inWatchlist", inWatchlist);

        return ResponseEntity.ok(response);
    }

    /**
     * Toggle watchlist status
     */
    @PostMapping("/toggle")
    public ResponseEntity<?> toggleWatchlist(@RequestBody WatchlistRequest request) {
        try {
            boolean added = watchlistService.toggleWatchlist(request.getProfileId(), request.getTitleId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", added ? "Added to watchlist" : "Removed from watchlist");
            response.put("inWatchlist", added);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Clear entire watchlist
     */
    @DeleteMapping("/{profileId}/clear")
    public ResponseEntity<?> clearWatchlist(@PathVariable int profileId) {
        watchlistService.clearWatchlist(profileId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Watchlist cleared");

        return ResponseEntity.ok(response);
    }

    // Request DTO
    public static class WatchlistRequest {
        private int profileId;
        private int titleId;

        // Getters and setters
        public int getProfileId() { return profileId; }
        public void setProfileId(int profileId) { this.profileId = profileId; }
        public int getTitleId() { return titleId; }
        public void setTitleId(int titleId) { this.titleId = titleId; }
    }
}