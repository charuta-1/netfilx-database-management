package com.netflix.controller;

import com.netflix.dao.TitleDAO;
import com.netflix.model.Title;
import com.netflix.service.NetflixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/titles")
@CrossOrigin(origins = "*")
public class AdminTitleController {

    private final NetflixService netflixService;

    @Autowired
    public AdminTitleController(NetflixService netflixService) {
        this.netflixService = netflixService;
    }

    @GetMapping
    public ResponseEntity<PagedTitleResponse> listTitles(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "types", required = false) String types,
            @RequestParam(value = "ratings", required = false) String ratings,
            @RequestParam(value = "genres", required = false) String genres,
            @RequestParam(value = "countries", required = false) String countries,
            @RequestParam(value = "yearMin", required = false) Integer yearMin,
            @RequestParam(value = "yearMax", required = false) Integer yearMax
    ) {
        int sanitizedSize = Math.max(1, Math.min(size, 200));
        int sanitizedPage = Math.max(1, page);

        TitleDAO.TitleFilter.Builder builder = TitleDAO.TitleFilter.builder();

        if (StringUtils.hasText(search)) {
            builder.search(search.trim());
        }

        List<String> typeList = splitToList(types);
        if (!typeList.isEmpty()) {
            builder.types(typeList);
        }

        List<String> ratingList = splitToList(ratings);
        if (!ratingList.isEmpty()) {
            builder.ratings(ratingList);
        }

        List<Integer> genreIds = splitToIntegerList(genres);
        if (!genreIds.isEmpty()) {
            builder.genreIds(genreIds);
        }

        List<String> countryList = splitToList(countries);
        if (!countryList.isEmpty()) {
            builder.countries(countryList);
        }

        if (yearMin != null) {
            builder.yearMin(yearMin);
        }
        if (yearMax != null) {
            builder.yearMax(yearMax);
        }

        TitleDAO.PagedResult<Title> result = netflixService.getTitlesPage(builder.build(), sanitizedPage, sanitizedSize);
        List<NetflixController.TitleResponse> items = result.getItems().stream()
                .map(NetflixController::mapTitle)
                .collect(Collectors.toList());

        PagedTitleResponse body = new PagedTitleResponse(
                items,
                result.getPage(),
                result.getPageSize(),
                result.getTotalItems(),
                result.getTotalPages()
        );

        return ResponseEntity.ok(body);
    }

    private List<String> splitToList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(token -> token.trim())
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }

    private List<Integer> splitToIntegerList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        List<Integer> values = new ArrayList<>();
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                values.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
                // Skip invalid values instead of failing the entire request
            }
        }
        return values;
    }

    public static class PagedTitleResponse {
        private final List<NetflixController.TitleResponse> items;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;

        public PagedTitleResponse(List<NetflixController.TitleResponse> items, int page, int size, int totalItems, int totalPages) {
            this.items = items;
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
        }

        public List<NetflixController.TitleResponse> getItems() {
            return items;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public int getTotalPages() {
            return totalPages;
        }
    }
}
