package com.research.gbjournal.service;

import com.research.gbjournal.dto.content.PageContentDTO;
import com.research.gbjournal.entity.PageContent;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.PageContentRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageContentService {

    private final PageContentRepository pageContentRepository;

    /**
     * Default sections for every JSON-defined public page. The files are copied from the
     * frontend (src/lib/cms/defaults) by `npm run cms:sync`; the frontend renders the same
     * copy as its fallback, so seeded content matches the live page until an admin edits it.
     */
    private static final String JSON_DEFAULTS_PATTERN = "classpath*:cms/defaults/*.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Placeholder sections seeded by earlier versions that no public page rendered. They are
     * removed on startup only while untouched (system-authored with the original title).
     */
    private static final Map<String, String> LEGACY_SYSTEM_SEED_TITLES = Map.ofEntries(
            Map.entry("about/overview", "About Gono Bishwabidyalay Journal"),
            Map.entry("about/aims-scope", "Aims and Scope"),
            Map.entry("about/indexing-metrics", "Indexing & Abstracting Standards"),
            Map.entry("authors/guidelines", "Author Guidelines & Manuscript Preparation"),
            Map.entry("authors/checklist", "Submission Preparation Checklist"),
            Map.entry("authors/apc-waiver", "Article Processing Charges (APC) & Fee Policy"),
            Map.entry("policies/peer-review", "Double-Blind Peer Review Framework"),
            Map.entry("policies/ethics-plagiarism", "Publication Ethics & Anti-Plagiarism Policy"),
            Map.entry("policies/open-access", "Open Access & Copyright Licensing"),
            Map.entry("contact/office-info", "Editorial Secretariat & Publishing Office"),
            Map.entry("editorial-board/governance-charter", "Editorial Governance & Academic Independence"),
            Map.entry("editorial-board/advisory-council", "International Advisory Council & Section Chairs"),
            Map.entry("reviewers/peer-review-protocol", "Double-Blind Evaluation Protocol & Rubric"),
            Map.entry("reviewers/reviewer-benefits", "Reviewer Recognition & Academic Acknowledgement"),
            Map.entry("issues/publication-schedule", "Biannual Publication Cadence & Special Volumes"),
            Map.entry("issues/digital-archiving", "Permanent Digital Preservation & Archiving"),
            Map.entry("articles/repository-scope", "Peer-Reviewed Research Repository & Discovery"),
            Map.entry("articles/citation-metrics", "Real-Time Reader Metrics & Impact Analytics")
    );

    private volatile Map<String, List<PageContent>> jsonDefaultsCache;

    @PostConstruct
    public void init() {
        // Detect and heal legacy numeric OIDs saved by previous @Lob Hibernate mapping
        boolean hasLegacyOids = pageContentRepository.findAll().stream()
                .anyMatch(pc -> pc.getContent() != null && pc.getContent().matches("^\\d{4,8}$"));
        if (hasLegacyOids) {
            log.info("Detected legacy OID references in page_contents. Re-seeding clean default content...");
            pageContentRepository.deleteAll();
        }
        purgeUntouchedLegacySeeds();
        // Inserts only sections that are missing, so admin edits are never overwritten.
        seedDefaultAcademicContent();
    }

    private void purgeUntouchedLegacySeeds() {
        List<PageContent> stale = pageContentRepository.findAll().stream()
                .filter(pc -> "system".equals(pc.getLastUpdatedBy()))
                .filter(pc -> {
                    String legacyTitle = LEGACY_SYSTEM_SEED_TITLES.get(pc.getPageKey() + "/" + pc.getSectionKey());
                    return legacyTitle != null && legacyTitle.equals(pc.getTitle());
                })
                .toList();
        if (!stale.isEmpty()) {
            pageContentRepository.deleteAll(stale);
            log.info("Removed {} unused legacy placeholder page sections.", stale.size());
        }
    }

    /** Public lookup: get published sections for a page */
    @Transactional(readOnly = true)
    @Cacheable(value = "pageContent", key = "#pageKey")
    public List<PageContentDTO> getPublishedPageContent(String pageKey) {
        List<PageContent> list = pageContentRepository.findByPageKeyAndPublishedTrueOrderByDisplayOrderAsc(pageKey.toLowerCase());
        if (list.isEmpty()) {
            // If nothing in database, return seeded defaults on the fly
            return getSeedListForPage(pageKey.toLowerCase());
        }
        return list.stream().map(this::toDTO).toList();
    }

    /** Admin lookup: get all sections for a page (including drafts) */
    @Transactional(readOnly = true)
    @Cacheable(value = "pageContentAdmin", key = "#pageKey != null ? #pageKey.toLowerCase() : ''")
    public List<PageContentDTO> getAdminPageContent(String pageKey) {
        List<PageContent> list = pageContentRepository.findByPageKeyOrderByDisplayOrderAsc(pageKey.toLowerCase());
        return list.stream().map(this::toDTO).toList();
    }

    /** Admin lookup: get all content grouped across all pages */
    @Transactional(readOnly = true)
    @Cacheable(value = "allPageContent")
    public Map<String, List<PageContentDTO>> getAllPagesContent() {
        List<PageContent> all = pageContentRepository.findAllByOrderByPageKeyAscDisplayOrderAsc();
        Map<String, List<PageContentDTO>> result = new LinkedHashMap<>();
        for (PageContent pc : all) {
            result.computeIfAbsent(pc.getPageKey(), k -> new ArrayList<>()).add(toDTO(pc));
        }
        return result;
    }

    /** Admin update section */
    @Transactional
    @CacheEvict(value = {"pageContent", "pageContentAdmin", "allPageContent"}, allEntries = true)
    public PageContentDTO updateSection(String pageKey, String sectionKey, PageContentDTO dto, String adminEmail) {
        PageContent section = pageContentRepository.findByPageKeyAndSectionKey(pageKey.toLowerCase(), sectionKey)
                .orElseGet(() -> PageContent.builder()
                        .pageKey(pageKey.toLowerCase())
                        .sectionKey(sectionKey)
                        .title(dto.getTitle() != null ? dto.getTitle() : "Section Title")
                        .build());

        if (dto.getTitle() != null) section.setTitle(dto.getTitle());
        if (dto.getSubtitle() != null) section.setSubtitle(dto.getSubtitle());
        if (dto.getContent() != null) section.setContent(dto.getContent());
        if (dto.getMetaJson() != null) section.setMetaJson(dto.getMetaJson());
        section.setDisplayOrder(dto.getDisplayOrder());
        section.setPublished(dto.isPublished());
        section.setLastUpdatedBy(adminEmail);

        PageContent saved = pageContentRepository.save(section);
        log.info("Admin {} updated page section [{}/{}]", adminEmail, pageKey, sectionKey);
        return toDTO(saved);
    }

    /** Admin create a new section */
    @Transactional
    @CacheEvict(value = {"pageContent", "pageContentAdmin", "allPageContent"}, allEntries = true)
    public PageContentDTO createSection(PageContentDTO dto, String adminEmail) {
        String pageKey = dto.getPageKey().toLowerCase().trim();
        String sectionKey = dto.getSectionKey().toLowerCase().trim().replaceAll("[^a-z0-9-_]", "-");

        if (pageContentRepository.findByPageKeyAndSectionKey(pageKey, sectionKey).isPresent()) {
            throw new BadRequestException("A section with key '" + sectionKey + "' already exists for page '" + pageKey + "'.");
        }

        PageContent pc = PageContent.builder()
                .pageKey(pageKey)
                .sectionKey(sectionKey)
                .title(dto.getTitle().trim())
                .subtitle(dto.getSubtitle())
                .content(dto.getContent())
                .metaJson(dto.getMetaJson())
                .displayOrder(dto.getDisplayOrder())
                .published(dto.isPublished())
                .lastUpdatedBy(adminEmail)
                .build();

        PageContent saved = pageContentRepository.save(pc);
        log.info("Admin {} created new page section [{}/{}]", adminEmail, pageKey, sectionKey);
        return toDTO(saved);
    }

    /** Admin delete section */
    @Transactional
    @CacheEvict(value = {"pageContent", "pageContentAdmin", "allPageContent"}, allEntries = true)
    public void deleteSection(Long id, String adminEmail) {
        PageContent pc = pageContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PageContent", "id", id));
        pageContentRepository.delete(pc);
        log.info("Admin {} deleted page section ID {}", adminEmail, id);
    }

    /** Reset to default academic content */
    @Transactional
    @CacheEvict(value = {"pageContent", "pageContentAdmin", "allPageContent"}, allEntries = true)
    public void resetDefaults(String pageKey, String adminEmail) {
        if (pageKey != null && !pageKey.isBlank() && !pageKey.equalsIgnoreCase("all")) {
            pageContentRepository.deleteByPageKey(pageKey.toLowerCase());
            seedPageDefaults(pageKey.toLowerCase());
            log.info("Admin {} reset defaults for page: {}", adminEmail, pageKey);
        } else {
            pageContentRepository.deleteAll();
            seedDefaultAcademicContent();
            log.info("Admin {} reset all site page defaults", adminEmail);
        }
    }

    // ===== Seed Data Helpers =====

    private void seedDefaultAcademicContent() {
        seedPageDefaults("home");
        seedPageDefaults("announcements");
        for (String pageKey : getJsonDefaults().keySet()) {
            seedPageDefaults(pageKey);
        }
    }

    private Map<String, List<PageContent>> getJsonDefaults() {
        Map<String, List<PageContent>> cached = jsonDefaultsCache;
        if (cached != null) return cached;
        Map<String, List<PageContent>> loaded = new LinkedHashMap<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(JSON_DEFAULTS_PATTERN);
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    JsonNode root = JSON.readTree(in);
                    String pageKey = root.path("pageKey").asString("").toLowerCase().trim();
                    JsonNode sections = root.path("sections");
                    if (pageKey.isEmpty() || !sections.isObject() || sections.isEmpty()) continue;
                    List<PageContent> list = new ArrayList<>();
                    int order = 1;
                    for (Map.Entry<String, JsonNode> entry : sections.properties()) {
                        JsonNode sec = entry.getValue();
                        JsonNode meta = sec.path("meta");
                        list.add(PageContent.builder()
                                .pageKey(pageKey)
                                .sectionKey(entry.getKey())
                                .title(sec.path("title").asString(entry.getKey()))
                                .subtitle(sec.hasNonNull("subtitle") ? sec.get("subtitle").asString() : null)
                                .content(sec.hasNonNull("content") ? sec.get("content").asString() : null)
                                .metaJson(meta.isObject() && !meta.isEmpty() ? JSON.writeValueAsString(meta) : null)
                                .displayOrder(order++)
                                .published(true)
                                .lastUpdatedBy("system")
                                .build());
                    }
                    loaded.put(pageKey, list);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load CMS page defaults from {}", JSON_DEFAULTS_PATTERN, e);
        }
        jsonDefaultsCache = loaded;
        return loaded;
    }

    /** Fresh, unsaved copies of a JSON page's default sections. */
    private List<PageContent> copyJsonDefaults(String pageKey) {
        return getJsonDefaults().getOrDefault(pageKey, List.of()).stream()
                .map(pc -> PageContent.builder()
                        .pageKey(pc.getPageKey())
                        .sectionKey(pc.getSectionKey())
                        .title(pc.getTitle())
                        .subtitle(pc.getSubtitle())
                        .content(pc.getContent())
                        .metaJson(pc.getMetaJson())
                        .displayOrder(pc.getDisplayOrder())
                        .published(true)
                        .lastUpdatedBy("system")
                        .build())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private void seedPageDefaults(String pageKey) {
        List<PageContent> seeds = getSeedEntitiesForPage(pageKey);
        for (PageContent sc : seeds) {
            if (pageContentRepository.findByPageKeyAndSectionKey(sc.getPageKey(), sc.getSectionKey()).isEmpty()) {
                pageContentRepository.save(sc);
            }
        }
    }

    private List<PageContent> getSeedEntitiesForPage(String pageKey) {
        if (getJsonDefaults().containsKey(pageKey.toLowerCase())) {
            return copyJsonDefaults(pageKey.toLowerCase());
        }
        List<PageContent> list = new ArrayList<>();
        switch (pageKey.toLowerCase()) {
            case "home":
                list.add(PageContent.builder()
                        .pageKey("home").sectionKey("hero-main")
                        .title("Gono Bishwabidyalay Journal of Science and Technology")
                        .subtitle("A Premier Multidisciplinary Double-Blind Peer-Reviewed Research Publication.")
                        .content("Disseminating breakthrough scientific discoveries, evidence-based health solutions, clinical pharmaceutical innovations, and engineering advances from global and regional academic communities.")
                        .metaJson("{\"badge\":\"Official Biannual Journal\",\"issnPrint\":\"2073-8447\",\"issnOnline\":\"2790-2188\",\"primaryCtaText\":\"Submit Manuscript\",\"secondaryCtaText\":\"Explore Latest Issue\"}")
                        .displayOrder(1).published(true).lastUpdatedBy("system").build());

                list.add(PageContent.builder()
                        .pageKey("home").sectionKey("latest-research")
                        .title("Latest Research")
                        .subtitle("Recent breakthroughs and peer-reviewed scholarly papers from our global community.")
                        .content("Curated collection of recently accepted and published peer-reviewed manuscripts.")
                        .metaJson("{\"viewAllText\":\"View all articles\",\"selectedArticleIds\":[\"la-01\",\"la-02\",\"la-03\",\"la-04\"]}")
                        .displayOrder(2).published(true).lastUpdatedBy("system").build());

                list.add(PageContent.builder()
                        .pageKey("home").sectionKey("current-issue")
                        .title("Current Issue")
                        .subtitle("May 2025")
                        .content("This issue features high-impact original research papers, clinical evaluations, and breakthrough methodologies across molecular sciences and allied health.")
                        .metaJson("{\"journalName\":\"Nexus Journal of Molecular Sciences\",\"volumeIssue\":\"Vol. 12, No. 4\",\"publicationDate\":\"May 15, 2025\",\"issnPrint\":\"2073-8447\",\"issnOnline\":\"2790-2188\",\"featuredPaperTitle\":\"Machine learning-guided discovery of allosteric inhibitors targeting emergent viral polymerases\",\"browseHref\":\"/issues/current\",\"pdfHref\":\"/pdfs/current-issue.pdf\"}")
                        .displayOrder(3).published(true).lastUpdatedBy("system").build());

                list.add(PageContent.builder()
                        .pageKey("home").sectionKey("most-read")
                        .title("Most Read Research")
                        .subtitle("Highly cited and trending papers across all disciplines in the GB Journal repository.")
                        .content("Ranked list of the most viewed and cited scholarly articles over the past academic quarter.")
                        .metaJson("{\"articleCount\":\"5\",\"selectedArticleIds\":[\"community-healthcare-access-savar\",\"pharmacy-practice-antimicrobial-stewardship\",\"climate-resilient-agriculture-manifolds\",\"legal-aid-university-clinic\",\"ai-assisted-learning-private-universities\"]}")
                        .displayOrder(4).published(true).lastUpdatedBy("system").build());

                list.add(PageContent.builder()
                        .pageKey("home").sectionKey("explore-topics")
                        .title("Explore by Topic")
                        .subtitle("Discover peer-reviewed scholarship organized by discipline and research frontier.")
                        .content("Browse peer-reviewed manuscripts across our core academic faculties and research disciplines.")
                        .metaJson("{\"tracks\":[\"Artificial Intelligence\",\"Medicine\",\"Engineering\",\"Climate Science\",\"Data Science\",\"Social Research\",\"Pharmacy\",\"Biomedical Technology\"]}")
                        .displayOrder(5).published(true).lastUpdatedBy("system").build());

                list.add(PageContent.builder()
                        .pageKey("home").sectionKey("featured-journals")
                        .title("Featured Focus Areas & Journals")
                        .subtitle("Specialized peer-reviewed journal collections and interdisciplinary research tracks.")
                        .content("Explore specialized biannual series published under the GB Journal academic press.")
                        .metaJson("{\"category\":\"Multidisciplinary Series\"}")
                        .displayOrder(6).published(true).lastUpdatedBy("system").build());

                list.add(PageContent.builder()
                        .pageKey("home").sectionKey("call-for-papers")
                        .title("Calls for Papers & Special Issues")
                        .subtitle("Thematic submissions currently inviting high-impact manuscripts.")
                        .content("The Editorial Board invites high-quality original research papers, reviews, and clinical studies for the upcoming biannual issue. Authors are requested to submit manuscripts online through the Research Workspace portal.")
                        .metaJson("{\"badge\":\"Active Call\",\"deadline\":\"October 31, 2026\",\"targetVolume\":\"Volume 14, Issue 2\",\"fastTrack\":\"Available\"}")
                        .displayOrder(7).published(true).lastUpdatedBy("system").build());

                list.add(PageContent.builder()
                        .pageKey("home").sectionKey("research-community")
                        .title("From Our Research Community")
                        .subtitle("Interviews, spotlights, and academic symposia connecting authors and reviewers.")
                        .content("Explore perspectives, researcher spotlights, and multimedia highlights from our contributing faculty and scholars.")
                        .metaJson("{\"spotlightAuthor\":\"Prof. Dr. Arup Chandra, PhD\",\"symposium\":\"Annual Research Colloquium 2026\"}")
                        .displayOrder(8).published(true).lastUpdatedBy("system").build());

                list.add(PageContent.builder()
                        .pageKey("home").sectionKey("home-faq")
                        .title("Frequently Asked Questions")
                        .subtitle("Essential information for authors, reviewers, and institutions regarding submission and indexing.")
                        .content("Answers to the most common questions regarding peer review, APCs, submission requirements, and publication timelines.")
                        .metaJson("{\"faqs\":[{\"q\":\"What types of manuscripts does GB Journal accept?\",\"a\":\"GB Journal welcomes original research articles, comprehensive review papers, clinical case studies, technical notes, and scholarly commentaries across multidisciplinary fields.\"},{\"q\":\"How does the double-blind peer review process operate?\",\"a\":\"Author identities and reviewer identities are completely concealed from each other to safeguard academic objectivity.\"},{\"q\":\"What is the typical turnaround timeline from submission to decision?\",\"a\":\"Initial editorial desk screening is conducted within 3 to 5 business days, and comprehensive peer review spans 4 to 6 weeks.\"},{\"q\":\"Are there publication charges or Article Processing Charges (APCs)?\",\"a\":\"GB Journal is committed to open scholarship with generous waiver programs available for researchers and students.\"}]}")
                        .displayOrder(9).published(true).lastUpdatedBy("system").build());

                list.add(PageContent.builder()
                        .pageKey("home").sectionKey("journal-stats")
                        .title("Advancing knowledge. Driving impact.")
                        .subtitle("Transparent editorial milestones and turnaround metrics.")
                        .content("GB Journal maintains strict turnaround benchmarks: 18 days average first decision, 42 days average review completion, and 34% overall acceptance rate.")
                        .metaJson("{\"articlesPublished\":\"12,486+\",\"globalReaders\":\"85,000+\",\"acceptanceRate\":\"34%\",\"turnaroundDays\":\"18 Days\",\"reviewersActive\":\"140+\",\"indexedArticles\":\"380+\",\"newsletterTitle\":\"Stay informed with GB Journal research alerts\",\"newsletterSubtitle\":\"Receive curated research highlights, call-for-papers announcements, and table-of-contents notifications.\"}")
                        .displayOrder(10).published(true).lastUpdatedBy("system").build());
                break;

            case "announcements":
                list.add(PageContent.builder()
                        .pageKey("announcements").sectionKey("call-for-papers")
                        .title("Call for Papers — Upcoming Issue (Vol. 14, No. 2)")
                        .subtitle("Submission Deadline: October 31, 2026 | Fast-Track Review Available")
                        .content("The Editorial Board invites high-quality original research papers, reviews, and clinical studies for the upcoming biannual issue. Authors are requested to submit manuscripts online through the Research Workspace portal.")
                        .metaJson("{\"badge\":\"Active Call\",\"deadline\":\"October 31, 2026\",\"targetVolume\":\"Volume 14, Issue 2\",\"fastTrack\":\"Available\"}")
                        .displayOrder(1).published(true).lastUpdatedBy("system").build());
                break;

        }
        return list;
    }

    private List<PageContentDTO> getSeedListForPage(String pageKey) {
        return getSeedEntitiesForPage(pageKey).stream().map(this::toDTO).toList();
    }

    private PageContentDTO toDTO(PageContent pc) {
        return PageContentDTO.builder()
                .id(pc.getId())
                .pageKey(pc.getPageKey())
                .sectionKey(pc.getSectionKey())
                .title(pc.getTitle())
                .subtitle(pc.getSubtitle())
                .content(pc.getContent())
                .metaJson(pc.getMetaJson())
                .displayOrder(pc.getDisplayOrder())
                .published(pc.isPublished())
                .lastUpdatedBy(pc.getLastUpdatedBy())
                .updatedAt(pc.getUpdatedAt())
                .build();
    }
}
