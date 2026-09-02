package com.research.gbjournal.controller;

import com.research.gbjournal.dto.navigation.NavItemDTO;
import com.research.gbjournal.service.NavigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/navigation")
@RequiredArgsConstructor
public class NavigationController {

    private final NavigationService navigationService;

    /**
     * GET /api/v1/navigation — Public endpoint returning published navigation items from database.
     */
    @GetMapping
    public ResponseEntity<List<NavItemDTO>> getPublishedNavigation() {
        return ResponseEntity.ok(navigationService.getPublishedNavigation());
    }
}
