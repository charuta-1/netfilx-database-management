package com.netflix.service;

import com.netflix.dao.*;
import com.netflix.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NetflixService {

    private final TitleDAO titleDAO;
    private final ActorDAO actorDAO;
    private final DirectorDAO directorDAO;
    private final CountryDAO countryDAO;
    private final GenreDAO genreDAO;
    private final RatingDAO ratingDAO;
    private final DurationDAO durationDAO;

    // New user-related DAOs
    private final UserDAO userDAO;
    private final UserProfileDAO userProfileDAO;
    private final WatchlistDAO watchlistDAO;
    private final WatchHistoryDAO watchHistoryDAO;
    private final UserRatingDAO userRatingDAO;
    private final UserGenrePreferenceDAO userGenrePreferenceDAO;

    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public NetflixService(TitleDAO titleDAO, ActorDAO actorDAO, DirectorDAO directorDAO,
                         CountryDAO countryDAO, GenreDAO genreDAO, RatingDAO ratingDAO,
                         DurationDAO durationDAO, UserDAO userDAO, UserProfileDAO userProfileDAO,
                         WatchlistDAO watchlistDAO, WatchHistoryDAO watchHistoryDAO,
                         UserRatingDAO userRatingDAO, UserGenrePreferenceDAO userGenrePreferenceDAO,
                         BCryptPasswordEncoder passwordEncoder) {
        this.titleDAO = titleDAO;
        this.actorDAO = actorDAO;
        this.directorDAO = directorDAO;
        this.countryDAO = countryDAO;
        this.genreDAO = genreDAO;
        this.ratingDAO = ratingDAO;
        this.durationDAO = durationDAO;
        this.userDAO = userDAO;
        this.userProfileDAO = userProfileDAO;
        this.watchlistDAO = watchlistDAO;
        this.watchHistoryDAO = watchHistoryDAO;
        this.userRatingDAO = userRatingDAO;
        this.userGenrePreferenceDAO = userGenrePreferenceDAO;
        this.passwordEncoder = passwordEncoder;
    }

    // ===================== EXISTING ADMIN METHODS =====================

    @Transactional
    public boolean addTitle(String showId, String title, String type, String description, 
                           String dateAdded, int releaseYear, String ratingCode, String durationUnit, 
                           int durationValue, String[] countries, String[] genres, 
                           String[] directors, String[] cast) {
        try {
            // Find or create rating
            int ratingId = findOrCreateRating(ratingCode);

            // Find or create duration
            int durationId = findOrCreateDuration(durationUnit, durationValue);

            // Create Title object
            Title newTitle = new Title();
            newTitle.setShowId(showId);
            newTitle.setTitle(title);
            newTitle.setType(type);
            newTitle.setDescription(description);
            newTitle.setDateAdded(Date.valueOf(dateAdded));
            newTitle.setReleaseYear(releaseYear);
            newTitle.setRatingId(ratingId);
            newTitle.setDurationId(durationId);

            // Insert title and get the ID
            boolean success = titleDAO.insertTitle(newTitle, Arrays.asList(countries), Arrays.asList(genres), Arrays.asList(directors), Arrays.asList(cast)); return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Title> getAllTitles() {
        return titleDAO.getAllTitles();
    }

    public TitleDAO.PagedResult<Title> getTitlesPage(TitleDAO.TitleFilter filter, int page, int pageSize) {
        return titleDAO.getTitlesPaged(filter, page, pageSize);
    }

    public List<Actor> getAllActors() {
        return actorDAO.getAllActors();
    }

    public List<Director> getAllDirectors() {
        return directorDAO.getAllDirectors();
    }

    public List<Country> getAllCountries() {
        return countryDAO.getAllCountries();
    }

    public List<Genre> getAllGenres() {
        return genreDAO.getAllGenres();
    }

    public List<Rating> getAllRatings() {
        return ratingDAO.getAllRatings();
    }

    public List<Title> searchTitles(String titleName, String type, String genre, String country, Integer year) {
        return titleDAO.searchTitles(titleName, type, genre, country, year);
    }

    /**
     * Update Title
     */
    @Transactional
    public boolean updateTitle(int titleId, String title, String type, String description,
                              String dateAdded, int releaseYear, String ratingCode,
                              String durationUnit, int durationValue) {
        try {
            Title existingTitle = titleDAO.getTitleById(titleId);
            if (existingTitle == null) {
                return false;
            }

            // Find or create rating and duration
            int ratingId = findOrCreateRating(ratingCode);
            int durationId = findOrCreateDuration(durationUnit, durationValue);

            // Update title fields
            existingTitle.setTitle(title);
            existingTitle.setType(type);
            existingTitle.setDescription(description);
            if (dateAdded != null && !dateAdded.trim().isEmpty()) {
                existingTitle.setDateAdded(Date.valueOf(dateAdded));
            }
            existingTitle.setReleaseYear(releaseYear);
            existingTitle.setRatingId(ratingId);
            existingTitle.setDurationId(durationId);

            return titleDAO.updateTitle(existingTitle);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete Title
     */
    @Transactional
    public boolean deleteTitle(int titleId) {
        try {
            return titleDAO.deleteTitle(titleId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Add Actor
     */
    @Transactional
    public boolean addActor(String fullName) {
        try {
            Actor actor = new Actor();
            actor.setFullName(fullName);
            return actorDAO.insertActor(actor);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete Actor
     */
    @Transactional
    public boolean deleteActor(int actorId) {
        try {
            return actorDAO.deleteActor(actorId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Add Director
     */
    @Transactional
    public boolean addDirector(String fullName) {
        try {
            Director director = new Director();
            director.setFullName(fullName);
            return directorDAO.insertDirector(director);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete Director
     */
    @Transactional
    public boolean deleteDirector(int directorId) {
        try {
            return directorDAO.deleteDirector(directorId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===================== NEW USER MANAGEMENT METHODS =====================

    /**
     * User Registration
     */
    @Transactional
    public User registerUser(String email, String username, String password, String dateOfBirth) {
        // Check if user already exists
        if (userDAO.findByEmail(email) != null) {
            throw new RuntimeException("User with this email already exists");
        }

        if (userDAO.findByUsername(username) != null) {
            throw new RuntimeException("User with this username already exists");
        }

        // Create new user
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));

        if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
            user.setDateOfBirth(LocalDate.parse(dateOfBirth));
        }

        userDAO.save(user);

        // Return user without password hash
        User savedUser = userDAO.findByEmail(email);
        savedUser.setPasswordHash(null);
        return savedUser;
    }

    /**
     * User Authentication
     */
    public User authenticateUser(String email, String password) {
        User user = userDAO.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        // Return user without password hash
        user.setPasswordHash(null);
        return user;
    }

    /**
     * Create User Profile
     */
    public UserProfile createProfile(int userId, String profileName, String maturityRatingOverride) {
        // Check profile limit
        int profileCount = userProfileDAO.countByUserId(userId);
        if (profileCount >= 5) {
            throw new RuntimeException("Maximum number of profiles (5) reached");
        }

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setProfileName(profileName);
        profile.setMaturityRatingOverride(maturityRatingOverride);

        userProfileDAO.save(profile);

        // Return the saved profile
        List<UserProfile> profiles = userProfileDAO.findByUserId(userId);
        return profiles.stream()
                .filter(p -> p.getProfileName().equals(profileName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get User Profiles
     */
    public List<UserProfile> getUserProfiles(int userId) {
        return userProfileDAO.findByUserId(userId);
    }

    /**
     * Set Genre Preferences for Profile
     */
    @Transactional
    public void setGenrePreferences(int profileId, List<Integer> genreIds) {
        userGenrePreferenceDAO.save(profileId, genreIds);
    }

    /**
     * Get Genre Preferences for Profile
     */
    public List<Integer> getGenrePreferences(int profileId) {
        return userGenrePreferenceDAO.findGenreIdsByProfileId(profileId);
    }

    /**
     * Add to Watchlist
     */
    public void addToWatchlist(int profileId, int titleId) {
        if (watchlistDAO.exists(profileId, titleId)) {
            throw new RuntimeException("Title is already in watchlist");
        }
        watchlistDAO.add(profileId, titleId);
    }

    /**
     * Remove from Watchlist
     */
    public void removeFromWatchlist(int profileId, int titleId) {
        watchlistDAO.remove(profileId, titleId);
    }

    /**
     * Get Watchlist
     */
    public List<Watchlist> getWatchlist(int profileId) {
        return watchlistDAO.findByProfileId(profileId);
    }

    /**
     * Add to Watch History
     */
    public void addToWatchHistory(int profileId, int titleId, boolean isCompleted) {
        WatchHistory history = new WatchHistory();
        history.setProfileId(profileId);
        history.setTitleId(titleId);
        history.setCompleted(isCompleted);
        watchHistoryDAO.add(history);
    }

    /**
     * Get Watch History
     */
    public List<WatchHistory> getWatchHistory(int profileId) {
        return watchHistoryDAO.findByProfileId(profileId);
    }

    /**
     * Rate Title
     */
    public void rateTitle(int profileId, int titleId, UserRating.RatingValue ratingValue) {
        UserRating rating = new UserRating();
        rating.setProfileId(profileId);
        rating.setTitleId(titleId);
        rating.setRatingValue(ratingValue);
        userRatingDAO.save(rating);
    }

    /**
     * Admin Authentication (simple for now)
     */
    public boolean authenticateAdmin(String username, String password) {
        // Simple hardcoded admin authentication
        // In production, this should be stored in database with proper encryption
        return "admin".equals(username) && "admin123".equals(password);
    }

    /**
     * Get Dashboard Statistics for Admin
     */
    public DashboardStats getDashboardStats() {
        int totalTitles = titleDAO.getAllTitles().size();
        int totalActors = actorDAO.getAllActors().size();
        int totalDirectors = directorDAO.getAllDirectors().size();
        int totalUsers = getAllUsers().size();
        int totalProfiles = getAllProfiles().size();

        return new DashboardStats(totalTitles, totalActors, totalDirectors, totalUsers, totalProfiles);
    }

    /**
     * Get All Users (Admin only)
     */
    public List<User> getAllUsers() {
        return userDAO.findAll().stream()
                .peek(user -> user.setPasswordHash(null))
                .collect(Collectors.toList());
    }

    /**
     * Get All Profiles (Admin only)
     */
    public List<UserProfile> getAllProfiles() {
        // This method should be restricted to admin access only
        return userProfileDAO.findAll(); // You'll need to implement this in UserProfileDAO
    }

    public AdminStats getAdminStats() {
    TitleDAO.TitleTypeCounts counts = titleDAO.getTitleTypeCounts();
    int totalTitles = counts.getTotal();
    int totalUsers = userDAO.countAll();
    int totalProfiles = userProfileDAO.countAll();
    int totalActors = actorDAO.countAll();
    int totalDirectors = directorDAO.countAll();
        int totalWatchHistoryEntries = watchHistoryDAO.countAll();
        int totalRatingInteractions = userRatingDAO.countAll();

        return new AdminStats(
        totalTitles,
        totalUsers,
        totalProfiles,
                totalActors,
                totalDirectors,
                totalWatchHistoryEntries,
                totalRatingInteractions,
        counts.getMovieCount(),
        counts.getTvShowCount()
        );
    }

    // ===================== HELPER METHODS =====================

    private int findOrCreateRating(String ratingCode) {
        Rating rating = ratingDAO.findByCode(ratingCode);
        if (rating == null) {
            rating = new Rating();
            rating.setCode(ratingCode);
            rating.setDescription("Rating: " + ratingCode);
            ratingDAO.insertRating(rating);
            rating = ratingDAO.findByCode(ratingCode);
        }
        return rating.getRatingId();
    }

    private int findOrCreateDuration(String unit, int value) {
        Duration duration = durationDAO.findByUnitAndValue(unit, value);
        if (duration == null) {
            duration = new Duration();
            duration.setUnit(unit);
            duration.setValue(value);
            durationDAO.insertDuration(duration);
            duration = durationDAO.findByUnitAndValue(unit, value);
        }
        return duration.getDurationId();
    }

    /**
     * Dashboard Statistics Helper Class
     */
    public static class DashboardStats {
        private final int totalTitles;
        private final int totalActors;
        private final int totalDirectors;
        private final int totalUsers;
        private final int totalProfiles;

        public DashboardStats(int totalTitles, int totalActors, int totalDirectors, int totalUsers, int totalProfiles) {
            this.totalTitles = totalTitles;
            this.totalActors = totalActors;
            this.totalDirectors = totalDirectors;
            this.totalUsers = totalUsers;
            this.totalProfiles = totalProfiles;
        }

        // Getters
        public int getTotalTitles() { return totalTitles; }
        public int getTotalActors() { return totalActors; }
        public int getTotalDirectors() { return totalDirectors; }
        public int getTotalUsers() { return totalUsers; }
        public int getTotalProfiles() { return totalProfiles; }
    }

    public static class AdminStats {
        private final int totalTitles;
        private final int totalUsers;
        private final int totalProfiles;
        private final int totalActors;
        private final int totalDirectors;
        private final int totalWatchHistoryEntries;
        private final int totalRatingInteractions;
        private final int movieCount;
        private final int tvShowCount;

        public AdminStats(int totalTitles, int totalUsers, int totalProfiles, int totalActors,
                          int totalDirectors, int totalWatchHistoryEntries, int totalRatingInteractions,
                          int movieCount, int tvShowCount) {
            this.totalTitles = totalTitles;
            this.totalUsers = totalUsers;
            this.totalProfiles = totalProfiles;
            this.totalActors = totalActors;
            this.totalDirectors = totalDirectors;
            this.totalWatchHistoryEntries = totalWatchHistoryEntries;
            this.totalRatingInteractions = totalRatingInteractions;
            this.movieCount = movieCount;
            this.tvShowCount = tvShowCount;
        }

        public int getTotalTitles() { return totalTitles; }
        public int getTotalUsers() { return totalUsers; }
        public int getTotalProfiles() { return totalProfiles; }
        public int getTotalActors() { return totalActors; }
        public int getTotalDirectors() { return totalDirectors; }
        public int getTotalWatchHistoryEntries() { return totalWatchHistoryEntries; }
        public int getTotalRatingInteractions() { return totalRatingInteractions; }
        public int getMovieCount() { return movieCount; }
        public int getTvShowCount() { return tvShowCount; }
    }
}


