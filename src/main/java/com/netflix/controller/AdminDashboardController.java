package com.netflix.controller;

import com.netflix.model.UserProfile;
import com.netflix.service.NetflixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    private final NetflixService netflixService;

    @Autowired
    public AdminDashboardController(NetflixService netflixService) {
        this.netflixService = netflixService;
    }

    @GetMapping("/stats")
    public ResponseEntity<NetflixService.AdminStats> getStats() {
        return ResponseEntity.ok(netflixService.getAdminStats());
    }

    @GetMapping("/profiles")
    public ResponseEntity<List<UserProfile>> getProfiles() {
        return ResponseEntity.ok(netflixService.getAllProfiles());
    }
}
