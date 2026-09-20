package com.research.gbjournal.service;

import com.research.gbjournal.dto.navigation.NavItemDTO;
import com.research.gbjournal.dto.navigation.NavSubItemDTO;
import com.research.gbjournal.entity.NavigationItem;
import com.research.gbjournal.repository.NavigationItemRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NavigationService {

    private final NavigationItemRepository repository;

    @PostConstruct
    public void initDefaultNavigationIfEmpty() {
        if (repository.count() == 0) {
            log.info("Initializing default GB Journal navigation items into PostgreSQL database...");
            seedDefaultNavigation();
        }
    }

    /**
     * Public endpoint: Get active/published navigation tree.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "navigation")
    public List<NavItemDTO> getPublishedNavigation() {
        List<NavigationItem> allItems = repository.findByEnabledTrueOrderByDisplayOrderAsc();
        Map<Long, List<NavSubItemDTO>> childrenByParent = new HashMap<>();
        List<NavigationItem> topItems = new ArrayList<>();

        for (NavigationItem item : allItems) {
            if (item.getParentId() == null) {
                topItems.add(item);
            } else {
                childrenByParent.computeIfAbsent(item.getParentId(), k -> new ArrayList<>())
                        .add(toSubDTO(item));
            }
        }

        List<NavItemDTO> dtos = new ArrayList<>();
        for (NavigationItem top : topItems) {
            NavItemDTO dto = toDTO(top);
            dto.setDropdown(childrenByParent.getOrDefault(top.getId(), Collections.emptyList()));
            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * Admin endpoint: Get full navigation tree including disabled/hidden items.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "navigationAdmin")
    public List<NavItemDTO> getAllNavigationAdmin() {
        List<NavigationItem> allItems = repository.findAllByOrderByDisplayOrderAsc();
        Map<Long, List<NavSubItemDTO>> childrenByParent = new HashMap<>();
        List<NavigationItem> topItems = new ArrayList<>();

        for (NavigationItem item : allItems) {
            if (item.getParentId() == null) {
                topItems.add(item);
            } else {
                childrenByParent.computeIfAbsent(item.getParentId(), k -> new ArrayList<>())
                        .add(toSubDTO(item));
            }
        }

        List<NavItemDTO> dtos = new ArrayList<>();
        for (NavigationItem top : topItems) {
            NavItemDTO dto = toDTO(top);
            dto.setDropdown(childrenByParent.getOrDefault(top.getId(), Collections.emptyList()));
            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * Admin endpoint: Save entire hierarchical navigation structure in bulk (reorder, add, edit).
     */
    @Transactional
    @CacheEvict(value = {"navigation", "navigationAdmin"}, allEntries = true)
    public List<NavItemDTO> saveBulkNavigation(List<NavItemDTO> dtos) {
        // Clear existing navigation records to cleanly rebuild from the updated tree
        repository.deleteAll();

        int topOrder = 0;
        for (NavItemDTO topDto : dtos) {
            NavigationItem topEntity = NavigationItem.builder()
                    .clientId(topDto.getClientId() != null ? topDto.getClientId() : "nav-" + System.currentTimeMillis())
                    .label(topDto.getLabel())
                    .href(topDto.getHref())
                    .dropdownHeader(topDto.getDropdownHeader())
                    .footerLabel(topDto.getFooterLabel())
                    .footerHref(topDto.getFooterHref())
                    .openInNewTab(topDto.isOpenInNewTab())
                    .enabled(topDto.isEnabled())
                    .displayOrder(topOrder++)
                    .build();

            NavigationItem savedTop = repository.save(topEntity);

            if (topDto.getDropdown() != null && !topDto.getDropdown().isEmpty()) {
                int subOrder = 0;
                for (NavSubItemDTO subDto : topDto.getDropdown()) {
                    NavigationItem subEntity = NavigationItem.builder()
                            .parentId(savedTop.getId())
                            .clientId(subDto.getClientId() != null ? subDto.getClientId() : "sub-" + System.currentTimeMillis())
                            .label(subDto.getLabel())
                            .href(subDto.getHref())
                            .description(subDto.getDescription())
                            .iconName(subDto.getIconName())
                            .enabled(subDto.isEnabled())
                            .displayOrder(subOrder++)
                            .build();

                    repository.save(subEntity);
                }
            }
        }

        log.info("Saved updated navigation hierarchy with {} top-level items into database.", dtos.size());
        return getAllNavigationAdmin();
    }

    /**
     * Admin endpoint: Reset navigation back to default GB Journal structure.
     */
    @Transactional
    @CacheEvict(value = "navigation", allEntries = true)
    public List<NavItemDTO> resetDefaults() {
        repository.deleteAll();
        seedDefaultNavigation();
        log.info("Reset navigation items to default GB Journal structure.");
        return getAllNavigationAdmin();
    }

    private void seedDefaultNavigation() {
        // 1. Home
        repository.save(NavigationItem.builder()
                .clientId("nav-home")
                .label("Home")
                .href("/")
                .displayOrder(0)
                .enabled(true)
                .build());

        // 2. About & Governance
        NavigationItem about = repository.save(NavigationItem.builder()
                .clientId("nav-about")
                .label("About & Governance")
                .href("/about")
                .dropdownHeader("About & Governance")
                .footerLabel("View full journal overview")
                .footerHref("/about")
                .displayOrder(1)
                .enabled(true)
                .build());

        repository.save(NavigationItem.builder()
                .parentId(about.getId())
                .clientId("sub-about-1")
                .label("About the Journal")
                .href("/about")
                .description("Scope, open access mandate & editorial vision")
                .iconName("BookOpen")
                .displayOrder(0)
                .enabled(true)
                .build());

        repository.save(NavigationItem.builder()
                .parentId(about.getId())
                .clientId("sub-about-2")
                .label("Editorial Board")
                .href("/editorial-board")
                .description("Academic leadership & discipline chairs")
                .iconName("Users")
                .displayOrder(1)
                .enabled(true)
                .build());

        repository.save(NavigationItem.builder()
                .parentId(about.getId())
                .clientId("sub-about-3")
                .label("Reviewer Guidelines")
                .href("/reviewers")
                .description("Peer-review standards & reviewer panel")
                .iconName("ShieldCheck")
                .displayOrder(2)
                .enabled(true)
                .build());

        repository.save(NavigationItem.builder()
                .parentId(about.getId())
                .clientId("sub-about-4")
                .label("Ethics & Policies")
                .href("/policies")
                .description("COPE compliance, copyright & retractions")
                .iconName("Scale")
                .displayOrder(3)
                .enabled(true)
                .build());

        // 3. Issues & Articles
        NavigationItem issues = repository.save(NavigationItem.builder()
                .clientId("nav-issues")
                .label("Issues & Articles")
                .href("/issues")
                .dropdownHeader("Issues & Archive")
                .footerLabel("Search all articles")
                .footerHref("/articles")
                .displayOrder(2)
                .enabled(true)
                .build());

        repository.save(NavigationItem.builder()
                .parentId(issues.getId())
                .clientId("sub-issues-1")
                .label("All Issues & Archive")
                .href("/issues")
                .description("Browse complete publication record by year")
                .iconName("Library")
                .displayOrder(0)
                .enabled(true)
                .build());

        repository.save(NavigationItem.builder()
                .parentId(issues.getId())
                .clientId("sub-issues-2")
                .label("Search Articles")
                .href("/articles")
                .description("Filter indexed papers by topic, DOI & keywords")
                .iconName("FileText")
                .displayOrder(1)
                .enabled(true)
                .build());

        // 4. For Authors
        NavigationItem authors = repository.save(NavigationItem.builder()
                .clientId("nav-authors")
                .label("For Authors")
                .href("/authors")
                .dropdownHeader("Author Resources")
                .footerLabel("Submit your manuscript")
                .footerHref("/dashboard/submissions/new")
                .displayOrder(3)
                .enabled(true)
                .build());

        repository.save(NavigationItem.builder()
                .parentId(authors.getId())
                .clientId("sub-authors-1")
                .label("Author Guidelines")
                .href("/authors")
                .description("Manuscript structure, formatting & checklist")
                .iconName("PenLine")
                .displayOrder(0)
                .enabled(true)
                .build());

        repository.save(NavigationItem.builder()
                .parentId(authors.getId())
                .clientId("sub-authors-2")
                .label("Submit Manuscript")
                .href("/dashboard/submissions/new")
                .description("Online manuscript submission portal")
                .iconName("Send")
                .displayOrder(1)
                .enabled(true)
                .build());

        // 5. Contact
        repository.save(NavigationItem.builder()
                .clientId("nav-contact")
                .label("Contact")
                .href("/contact")
                .displayOrder(4)
                .enabled(true)
                .build());
    }

    private NavItemDTO toDTO(NavigationItem entity) {
        return NavItemDTO.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .label(entity.getLabel())
                .href(entity.getHref())
                .dropdownHeader(entity.getDropdownHeader())
                .footerLabel(entity.getFooterLabel())
                .footerHref(entity.getFooterHref())
                .displayOrder(entity.getDisplayOrder())
                .openInNewTab(entity.isOpenInNewTab())
                .enabled(entity.isEnabled())
                .dropdown(new ArrayList<>())
                .build();
    }

    private NavSubItemDTO toSubDTO(NavigationItem entity) {
        return NavSubItemDTO.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .label(entity.getLabel())
                .href(entity.getHref())
                .description(entity.getDescription())
                .iconName(entity.getIconName())
                .displayOrder(entity.getDisplayOrder())
                .enabled(entity.isEnabled())
                .build();
    }
}
