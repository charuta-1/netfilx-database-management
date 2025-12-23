package com.netflix.controller;

import com.netflix.dao.UserGenrePreferenceDAO;
import com.netflix.model.Title;
import com.netflix.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserGenrePreferenceDAO userGenrePreferenceDAO;

    @Autowired
    public RecommendationController(RecommendationService recommendationService,
                                   UserGenrePreferenceDAO userGenrePreferenceDAO) {
        this.recommendationService = recommendationService;
        this.userGenrePreferenceDAO = userGenrePreferenceDAO;
    }

    /**
     * Get personalized recommendations for a profile
     */
    @GetMapping("/{profileId}")
    public ResponseEntity<List<Title>> getPersonalizedRecommendations(
            @PathVariable int profileId,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            List<Title> recommendations = recommendationService.getPersonalizedRecommendations(profileId, limit);
            return ResponseEntity.ok(recommendations);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Set genre preferences for a profile (typically for new users)
     */
    @PostMapping("/preferences/{profileId}")
    public ResponseEntity<?> setGenrePreferences(
            @PathVariable int profileId,
            @RequestBody GenrePreferencesRequest request) {

        try {
            userGenrePreferenceDAO.save(profileId, request.getGenreIds());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Genre preferences saved successfully");
            response.put("profileId", profileId);
            response.put("genreCount", request.getGenreIds().size());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get current genre preferences for a profile
     */
    @GetMapping("/preferences/{profileId}")
    public ResponseEntity<Map<String, Object>> getGenrePreferences(@PathVariable int profileId) {
        try {
            List<Integer> genreIds = userGenrePreferenceDAO.findGenreIdsByProfileId(profileId);

            Map<String, Object> response = new HashMap<>();
            response.put("profileId", profileId);
            response.put("genreIds", genreIds);
            response.put("hasPreferences", !genreIds.isEmpty());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get recommendations based on specific genres
     */
    @PostMapping("/{profileId}/by-genres")
    public ResponseEntity<List<Title>> getGenreBasedRecommendations(
            @PathVariable int profileId,
            @RequestBody GenreRecommendationRequest request,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            List<Title> recommendations = recommendationService.getGenreBasedRecommendations(
                profileId, request.getGenreIds(), limit);
            return ResponseEntity.ok(recommendations);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Check if a profile has genre preferences set (useful for onboarding)
     */
    @GetMapping("/preferences/{profileId}/exists")
    public ResponseEntity<Map<String, Object>> hasGenrePreferences(@PathVariable int profileId) {
        boolean hasPreferences = userGenrePreferenceDAO.hasPreferences(profileId);

        Map<String, Object> response = new HashMap<>();
        response.put("profileId", profileId);
        response.put("hasPreferences", hasPreferences);
        response.put("needsOnboarding", !hasPreferences);

        return ResponseEntity.ok(response);
    }

    /**
     * Add a single genre preference
     */
    @PostMapping("/preferences/{profileId}/add/{genreId}")
    public ResponseEntity<?> addGenrePreference(@PathVariable int profileId, @PathVariable int genreId) {
        try {
            userGenrePreferenceDAO.addPreference(profileId, genreId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Genre preference added");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Remove a single genre preference
     */
    @DeleteMapping("/preferences/{profileId}/remove/{genreId}")
    public ResponseEntity<?> removeGenrePreference(@PathVariable int profileId, @PathVariable int genreId) {
        try {
            userGenrePreferenceDAO.removePreference(profileId, genreId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Genre preference removed");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Clear all genre preferences for a profile
     */
    @DeleteMapping("/preferences/{profileId}/clear")
    public ResponseEntity<?> clearGenrePreferences(@PathVariable int profileId) {
        try {
            userGenrePreferenceDAO.clearPreferences(profileId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All genre preferences cleared");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    // Request DTOs
    public static class GenrePreferencesRequest {
        private List<Integer> genreIds;

        public GenrePreferencesRequest() {}

        public List<Integer> getGenreIds() { return genreIds; }
        public void setGenreIds(List<Integer> genreIds) { this.genreIds = genreIds; }
    }

    public static class GenreRecommendationRequest {
        private List<Integer> genreIds;

        public GenreRecommendationRequest() {}

        public List<Integer> getGenreIds() { return genreIds; }
        public void setGenreIds(List<Integer> genreIds) { this.genreIds = genreIds; }
    }
}