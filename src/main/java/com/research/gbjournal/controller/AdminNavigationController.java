package com.research.gbjournal.controller;

import com.research.gbjournal.dto.navigation.NavItemDTO;
import com.research.gbjournal.service.NavigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/navigation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminNavigationController {

    private final NavigationService navigationService;

    /**
     * GET /api/v1/admin/navigation — Admin endpoint returning all navigation items (including disabled).
     */
    @GetMapping
    public ResponseEntity<List<NavItemDTO>> getAllNavigation() {
        return ResponseEntity.ok(navigationService.getAllNavigationAdmin());
    }

    /**
     * PUT /api/v1/admin/navigation/bulk — Save / reorder complete navigation hierarchy.
     */
    @PutMapping("/bulk")
    public ResponseEntity<List<NavItemDTO>> saveBulkNavigation(@RequestBody List<NavItemDTO> dtos) {
        return ResponseEntity.ok(navigationService.saveBulkNavigation(dtos));
    }

    /**
     * POST /api/v1/admin/navigation/reset-defaults — Reset navigation to default GB Journal structure.
     */
    @PostMapping("/reset-defaults")
    public ResponseEntity<List<NavItemDTO>> resetDefaults() {
        return ResponseEntity.ok(navigationService.resetDefaults());
    }
}
