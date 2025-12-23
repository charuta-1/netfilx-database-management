package com.netflix.model;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

public class Title {
    private int titleId;
    private String showId;
    private String title;
    private String type; // 'Movie' or 'TV Show'
    private String description;
    private Date dateAdded;
    private int releaseYear;
    private int ratingId;
    private int durationId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Related objects
    private Rating rating;
    private Duration duration;
    private List<Country> countries;
    private List<Genre> genres;
    private List<Director> directors;
    private List<Actor> cast;

    public Title() {}

    public Title(int titleId, String showId, String title, String type, String description, 
                 Date dateAdded, int releaseYear, int ratingId, int durationId) {
        this.titleId = titleId;
        this.showId = showId;
        this.title = title;
        this.type = type;
        this.description = description;
        this.dateAdded = dateAdded;
        this.releaseYear = releaseYear;
        this.ratingId = ratingId;
        this.durationId = durationId;
    }

    // Getters and Setters
    public int getTitleId() { return titleId; }
    public void setTitleId(int titleId) { this.titleId = titleId; }

    public String getShowId() { return showId; }
    public void setShowId(String showId) { this.showId = showId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getDateAdded() { return dateAdded; }
    public void setDateAdded(Date dateAdded) { this.dateAdded = dateAdded; }

    public int getReleaseYear() { return releaseYear; }
    public void setReleaseYear(int releaseYear) { this.releaseYear = releaseYear; }

    public int getRatingId() { return ratingId; }
    public void setRatingId(int ratingId) { this.ratingId = ratingId; }

    public int getDurationId() { return durationId; }
    public void setDurationId(int durationId) { this.durationId = durationId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Rating getRating() { return rating; }
    public void setRating(Rating rating) { this.rating = rating; }

    public Duration getDuration() { return duration; }
    public void setDuration(Duration duration) { this.duration = duration; }

    public List<Country> getCountries() { return countries; }
    public void setCountries(List<Country> countries) { this.countries = countries; }

    public List<Genre> getGenres() { return genres; }
    public void setGenres(List<Genre> genres) { this.genres = genres; }

    public List<Director> getDirectors() { return directors; }
    public void setDirectors(List<Director> directors) { this.directors = directors; }

    public List<Actor> getCast() { return cast; }
    public void setCast(List<Actor> cast) { this.cast = cast; }

    @Override
    public String toString() {
        return "Title{" +
                "titleId=" + titleId +
                ", showId='" + showId + "'" +
                ", title='" + title + "'" +
                ", type='" + type + "'" +
                ", description='" + description + "'" +
                ", dateAdded=" + dateAdded +
                ", releaseYear=" + releaseYear +
                ", ratingId=" + ratingId +
                ", durationId=" + durationId +
                '}';
    }
}