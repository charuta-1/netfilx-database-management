package com.netflix.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netflix.dao.TitleDAO;
import com.netflix.model.*;
import com.netflix.service.NetflixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class NetflixController {

    private final NetflixService netflixService;

    @Autowired
    public NetflixController(NetflixService netflixService) {
        this.netflixService = netflixService;
    }

    // Quick health endpoint
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "UP");
        try {
            int titles = netflixService.getAllTitles().size();
            int actors = netflixService.getAllActors().size();
            int directors = netflixService.getAllDirectors().size();
            int countries = netflixService.getAllCountries().size();
            int genres = netflixService.getAllGenres().size();
            int ratings = netflixService.getAllRatings().size();
            Map<String, Integer> counts = new HashMap<>();
            counts.put("titles", titles);
            counts.put("actors", actors);
            counts.put("directors", directors);
            counts.put("countries", countries);
            counts.put("genres", genres);
            counts.put("ratings", ratings);
            m.put("db", "OK");
            m.put("counts", counts);
        } catch (Exception e) {
            m.put("db", "ERROR");
            m.put("error", e.getMessage());
        }
        return m;
    }

    // DTO for creating a title from frontend
    static class TitleRequest {
        @JsonProperty("show_id") public String showId;
        public String title;
        public String type;
        public String description;
        @JsonProperty("date_added") public String dateAdded; // yyyy-MM-dd
        @JsonProperty("release_year") public int releaseYear;
        public String rating; // rating code, e.g., "TV-14"
        public String duration; // e.g., "116 min" or "4 seasons"
        public List<String> countries;
        public List<String> genres;
        public List<String> directors;
        public List<String> cast;
    }

    // DTO for sending to frontend
    static class TitleResponse {
        @JsonProperty("title_id") public int titleId;
        @JsonProperty("show_id") public String showId;
        public String title;
        public String type;
        public String description;
        @JsonProperty("date_added") public String dateAdded;
        @JsonProperty("release_year") public int releaseYear;
        public String rating;
        public String duration;
        public List<String> countries;
        public List<String> genres;
        public List<String> directors;
        public List<String> cast;
    }

    private static String buildDuration(Duration d) {
        if (d == null || d.getUnit() == null || d.getValue() <= 0) return "";
        String unit = d.getUnit();
        int val = d.getValue();
        // Normalize: season/min -> add plural s when >1
        if (val != 1 && !unit.endsWith("s")) unit = unit + "s";
        return val + " " + unit;
    }

    static TitleResponse mapTitle(Title t) {
        TitleResponse r = new TitleResponse();
        r.titleId = t.getTitleId();
        r.showId = t.getShowId();
        r.title = t.getTitle();
        r.type = t.getType();
        r.description = t.getDescription();
        Date da = t.getDateAdded();
        r.dateAdded = (da != null) ? da.toString() : null;
        r.releaseYear = t.getReleaseYear();
        r.rating = (t.getRating() != null) ? t.getRating().getCode() : null;
        r.duration = buildDuration(t.getDuration());
        r.countries = t.getCountries() == null ? new ArrayList<>() : t.getCountries().stream().map(Country::getName).collect(Collectors.toList());
        r.genres = t.getGenres() == null ? new ArrayList<>() : t.getGenres().stream().map(Genre::getName).collect(Collectors.toList());
        r.directors = t.getDirectors() == null ? new ArrayList<>() : t.getDirectors().stream().map(Director::getFullName).collect(Collectors.toList());
        r.cast = t.getCast() == null ? new ArrayList<>() : t.getCast().stream().map(Actor::getFullName).collect(Collectors.toList());
        return r;
    }

    @GetMapping("/titles")
    public List<TitleResponse> getAllTitles() {
        return netflixService.getAllTitles().stream().map(NetflixController::mapTitle).collect(Collectors.toList());
    }

    private static String normalizeUnit(String rawUnit) {
        if (rawUnit == null) return "min";
        String u = rawUnit.toLowerCase(Locale.ROOT).trim();
        // keep only a trailing plural 's'
        if (u.endsWith("s")) u = u.substring(0, u.length() - 1);
        // common synonyms
        if (u.startsWith("min") || u.equals("minute")) return "min";
        if (u.startsWith("season")) return "season";
        return "min";
    }

    private static class ParsedDuration {
        String unit; int value;
        ParsedDuration(String unit, int value) { this.unit = unit; this.value = value; }
    }

    private static ParsedDuration parseDuration(String raw) {
        if (raw == null || raw.isBlank()) return new ParsedDuration("min", 0);
        String[] parts = raw.trim().split("\\s+");
        int value = 0; String unit = "min";
        try {
            value = Integer.parseInt(parts[0]);
        } catch (Exception ignored) { value = 0; }
        if (parts.length > 1) unit = normalizeUnit(parts[1]);
        return new ParsedDuration(unit, value);
    }

    @PostMapping("/titles")
    public ResponseEntity<?> createTitle(@RequestBody TitleRequest req) {
        // Ensure show_id fits schema (VARCHAR(10))
        String computedShowId;
        if (req.showId != null && !req.showId.isEmpty()) {
            computedShowId = req.showId.length() > 10 ? req.showId.substring(0, 10) : req.showId;
        } else {
            computedShowId = "s" + (System.currentTimeMillis() % 1_000_000_000L);
        }

        ParsedDuration pd = parseDuration(req.duration);

        boolean ok = netflixService.addTitle(
                computedShowId,
                req.title,
                req.type,
                req.description,
                req.dateAdded,
                req.releaseYear,
                req.rating,
                pd.unit,
                pd.value,
                req.countries != null ? req.countries.toArray(new String[0]) : null,
                req.genres != null ? req.genres.toArray(new String[0]) : null,
                req.directors != null ? req.directors.toArray(new String[0]) : null,
                req.cast != null ? req.cast.toArray(new String[0]) : null
        );

        if (ok) return ResponseEntity.ok().build();
        return ResponseEntity.badRequest().body("Failed to add title");
    }

    @PutMapping("/titles/{id}")
    public ResponseEntity<?> updateTitle(@PathVariable("id") int id, @RequestBody TitleRequest req) {
        ParsedDuration pd = parseDuration(req.duration);

        boolean ok = netflixService.updateTitle(
                id,
                req.title,
                req.type,
                req.description,
                req.dateAdded,
                req.releaseYear,
                req.rating,
                pd.unit,
                pd.value
        );
        if (ok) return ResponseEntity.noContent().build();
        return ResponseEntity.badRequest().body("Failed to update title");
    }

    @DeleteMapping("/titles/{id}")
    public ResponseEntity<?> deleteTitle(@PathVariable("id") int id) {
        boolean ok = netflixService.deleteTitle(id);
        if (ok) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/actors")
    public List<Map<String, Object>> getAllActors() {
        return netflixService.getAllActors().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("actor_id", a.getActorId());
            m.put("full_name", a.getFullName());
            return m;
        }).collect(Collectors.toList());
    }

    static class PersonRequest { public String full_name; }

    @PostMapping("/actors")
    public ResponseEntity<?> addActor(@RequestBody PersonRequest req) {
        boolean ok = netflixService.addActor(req.full_name);
        if (ok) return ResponseEntity.ok().build();
        return ResponseEntity.badRequest().body("Failed to add actor");
    }

    @DeleteMapping("/actors/{id}")
    public ResponseEntity<?> deleteActor(@PathVariable("id") int id) {
        boolean ok = netflixService.deleteActor(id);
        if (ok) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/directors")
    public List<Map<String, Object>> getAllDirectors() {
        return netflixService.getAllDirectors().stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("director_id", d.getDirectorId());
            m.put("full_name", d.getFullName());
            return m;
        }).collect(Collectors.toList());
    }

    @PostMapping("/directors")
    public ResponseEntity<?> addDirector(@RequestBody PersonRequest req) {
        boolean ok = netflixService.addDirector(req.full_name);
        if (ok) return ResponseEntity.ok().build();
        return ResponseEntity.badRequest().body("Failed to add director");
    }

    @DeleteMapping("/directors/{id}")
    public ResponseEntity<?> deleteDirector(@PathVariable("id") int id) {
        boolean ok = netflixService.deleteDirector(id);
        if (ok) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/countries")
    public List<String> getAllCountries() {
        return netflixService.getAllCountries().stream().map(Country::getName).collect(Collectors.toList());
    }

    static class GenreResponse {
        @JsonProperty("genre_id") public int genreId;
        public String name;

        GenreResponse(int genreId, String name) {
            this.genreId = genreId;
            this.name = name;
        }
    }

    @GetMapping("/genres")
    public List<GenreResponse> getAllGenres() {
        return netflixService.getAllGenres().stream()
                .map(g -> new GenreResponse(g.getGenreId(), g.getName()))
                .collect(Collectors.toList());
    }

    @GetMapping("/ratings")
    public List<String> getAllRatings() { return netflixService.getAllRatings().stream().map(Rating::getCode).collect(Collectors.toList()); }

    @GetMapping("/actors/{id}/titles")
    public List<TitleResponse> getTitlesByActor(@PathVariable("id") int id) {
        TitleDAO dao = new TitleDAO();
        return dao.getTitlesByActorId(id).stream().map(NetflixController::mapTitle).collect(Collectors.toList());
    }

    @GetMapping("/directors/{id}/titles")
    public List<TitleResponse> getTitlesByDirector(@PathVariable("id") int id) {
        TitleDAO dao = new TitleDAO();
        return dao.getTitlesByDirectorId(id).stream().map(NetflixController::mapTitle).collect(Collectors.toList());
    }
}
