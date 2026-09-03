package com.research.gbjournal.service;

import com.research.gbjournal.dto.content.PageContentDTO;
import com.research.gbjournal.entity.PageContent;
import com.research.gbjournal.exception.BadRequestException;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.PageContentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageContentService {

    private final PageContentRepository pageContentRepository;

    @PostConstruct
    public void init() {
        if (pageContentRepository.count() == 0) {
            seedDefaultAcademicContent();
        } else {
            // Detect and heal legacy numeric OIDs saved by previous @Lob Hibernate mapping
            boolean hasLegacyOids = pageContentRepository.findAll().stream()
                    .anyMatch(pc -> pc.getContent() != null && pc.getContent().matches("^\\d{4,8}$"));
            if (hasLegacyOids) {
                log.info("Detected legacy OID references in page_contents. Re-seeding clean default content...");
                pageContentRepository.deleteAll();
                seedDefaultAcademicContent();
                log.info("Re-seeding complete.");
            }
        }
    }

    /** Public lookup: get published sections for a page */
    @Transactional(readOnly = true)
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
    public List<PageContentDTO> getAdminPageContent(String pageKey) {
        List<PageContent> list = pageContentRepository.findByPageKeyOrderByDisplayOrderAsc(pageKey.toLowerCase());
        return list.stream().map(this::toDTO).toList();
    }

    /** Admin lookup: get all content grouped across all pages */
    @Transactional(readOnly = true)
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
    public void deleteSection(Long id, String adminEmail) {
        PageContent pc = pageContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PageContent", "id", id));
        pageContentRepository.delete(pc);
        log.info("Admin {} deleted page section ID {}", adminEmail, id);
    }

    /** Reset to default academic content */
    @Transactional
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
        log.info("Seeding default academic page contents...");
        seedPageDefaults("home");
        seedPageDefaults("about");
        seedPageDefaults("authors");
        seedPageDefaults("policies");
        seedPageDefaults("announcements");
        seedPageDefaults("contact");
        log.info("Academic page content seed complete.");
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
        List<PageContent> list = new ArrayList<>();
        switch (pageKey.toLowerCase()) {
            case "about":
                list.add(PageContent.builder()
                        .pageKey("about").sectionKey("overview")
                        .title("About Gono Bishwabidyalay Journal")
                        .subtitle("A premier multidisciplinary peer-reviewed research forum founded at Gono Bishwabidyalay.")
                        .content("The Gono Bishwabidyalay Journal of Science and Technology is an official biannual, double-blind peer-reviewed academic journal dedicated to disseminating high-impact discoveries across basic sciences, allied health, pharmacy, engineering, and social development.")
                        .displayOrder(1).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey("about").sectionKey("aims-scope")
                        .title("Aims and Scope")
                        .subtitle("Fostering innovative scientific enquiry and evidence-based solutions.")
                        .content("GB Journal publishes original research articles, comprehensive review papers, short communications, and technical notes covering multidisciplinary sciences, clinical pharmacy, biomedical technology, computer science, and public health innovation.")
                        .displayOrder(2).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey("about").sectionKey("indexing-metrics")
                        .title("Indexing & Abstracting Standards")
                        .subtitle("Committed to global citation tracking, open access visibility, and metadata preservation.")
                        .content("ISSN (Print): 2073-8447 | ISSN (Online): 2790-2188. Indexed in Google Scholar, CrossRef, DOI Foundation, Banglajol, ResearchGate, and Index Copernicus.")
                        .metaJson("{\"issnPrint\":\"2073-8447\",\"issnOnline\":\"2790-2188\",\"frequency\":\"Biannual (June & December)\",\"reviewModel\":\"Double-Blind Peer Review\"}")
                        .displayOrder(3).published(true).lastUpdatedBy("system").build());
                break;

            case "authors":
                list.add(PageContent.builder()
                        .pageKey("authors").sectionKey("guidelines")
                        .title("Author Guidelines & Manuscript Preparation")
                        .subtitle("Instructions for preparing and formatting submissions for rapid editorial evaluation.")
                        .content("Manuscripts must be written in clear, grammatical English in Microsoft Word (.docx) or LaTeX format. Submissions must include Title, Structured Abstract (250 words max), 4-6 Keywords, Introduction, Materials & Methods, Results, Discussion, and References formatted in Vancouver/IEEE style.")
                        .displayOrder(1).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey("authors").sectionKey("checklist")
                        .title("Submission Preparation Checklist")
                        .subtitle("Verify all prerequisites before uploading to the manuscript pipeline.")
                        .content("1. The submission has not been previously published nor is it before another journal for consideration.\n2. The manuscript file is in OpenOffice or Microsoft Word document file format.\n3. All co-authors have reviewed and approved the final submitted draft.\n4. Ethical approval and consent certificates are included for clinical/human subject research.")
                        .displayOrder(2).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey("authors").sectionKey("apc-waiver")
                        .title("Article Processing Charges (APC) & Fee Policy")
                        .subtitle("Transparent publication policy supporting academic dissemination.")
                        .content("GB Journal operates as an open-access platform. Submissions are free of submission charges. Modest processing fees apply upon acceptance to cover DOI registration, metadata indexing, and XML typesetting. Automatic fee waivers are granted to researchers from low-income developing institutions upon formal request.")
                        .displayOrder(3).published(true).lastUpdatedBy("system").build());
                break;

            case "policies":
                list.add(PageContent.builder()
                        .pageKey("policies").sectionKey("peer-review")
                        .title("Double-Blind Peer Review Framework")
                        .subtitle("Rigorous, unbiased, and transparent scientific evaluation.")
                        .content("All submitted manuscripts undergo double-blind peer review where both reviewer and author identities remain anonymous. Each paper is evaluated independently by at least two domain specialists. Editorial decisions are based strictly on scientific merit, methodology rigor, and original contribution.")
                        .displayOrder(1).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey("policies").sectionKey("ethics-plagiarism")
                        .title("Publication Ethics & Anti-Plagiarism Policy")
                        .subtitle("Adherence to COPE (Committee on Publication Ethics) international standards.")
                        .content("GB Journal strictly prohibits plagiarism, redundant publication, fabrication, and unauthorized image manipulation. All incoming submissions are screened using CrossCheck/Turnitin similarity software. Similarity index exceeding 15% (excluding references) is subject to immediate rejection.")
                        .displayOrder(2).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey("policies").sectionKey("open-access")
                        .title("Open Access & Copyright Licensing")
                        .subtitle("CC BY 4.0 Creative Commons Attribution License.")
                        .content("Articles are published under the Creative Commons Attribution 4.0 International License (CC BY 4.0), permitting unrestricted use, distribution, and reproduction in any medium, provided the original work is properly cited. Authors retain full copyright ownership of their articles.")
                        .displayOrder(3).published(true).lastUpdatedBy("system").build());
                break;

            case "home":
            case "announcements":
                list.add(PageContent.builder()
                        .pageKey(pageKey.toLowerCase()).sectionKey("hero-main")
                        .title("Gono Bishwabidyalay Journal of Science and Technology")
                        .subtitle("A Premier Multidisciplinary Double-Blind Peer-Reviewed Research Publication.")
                        .content("Disseminating breakthrough scientific discoveries, evidence-based health solutions, clinical pharmaceutical innovations, and engineering advances from global and regional academic communities.")
                        .metaJson("{\"badge\":\"Official Biannual Journal\",\"issnPrint\":\"2073-8447\",\"issnOnline\":\"2790-2188\",\"primaryCtaText\":\"Submit Manuscript\",\"secondaryCtaText\":\"Explore Latest Issue\"}")
                        .displayOrder(1).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey(pageKey.toLowerCase()).sectionKey("call-for-papers")
                        .title("Call for Papers — Upcoming Issue (Vol. 14, No. 2)")
                        .subtitle("Submission Deadline: October 31, 2026 | Fast-Track Review Available")
                        .content("The Editorial Board invites high-quality original research papers, reviews, and clinical studies for the upcoming biannual issue. Authors are requested to submit manuscripts online through the Research Workspace portal.")
                        .metaJson("{\"badge\":\"Active Call\",\"deadline\":\"October 31, 2026\",\"targetVolume\":\"Volume 14, Issue 2\",\"fastTrack\":\"Available\"}")
                        .displayOrder(2).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey(pageKey.toLowerCase()).sectionKey("journal-stats")
                        .title("Journal Performance & Fast-Track Benchmarks")
                        .subtitle("Transparent editorial milestones and turnaround metrics.")
                        .content("GB Journal maintains strict turnaround benchmarks: 18 days average first decision, 42 days average review completion, and 34% overall acceptance rate.")
                        .metaJson("{\"turnaroundDays\":\"18 Days\",\"acceptanceRate\":\"34%\",\"reviewersActive\":\"140+\",\"indexedArticles\":\"380+\"}")
                        .displayOrder(3).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey(pageKey.toLowerCase()).sectionKey("scope-tracks")
                        .title("Multidisciplinary Scope & Key Research Domains")
                        .subtitle("Covering Pharmacy, Allied Health, Basic Sciences, Engineering & Public Innovation.")
                        .content("We welcome high-rigor manuscripts across 5 specialized subject tracks: Pharmaceutical Sciences, Biomedical & Health Technology, Applied Chemistry & Physics, Computer Science & Systems Engineering, and Community Health Development.")
                        .metaJson("{\"tracks\":[\"Pharmaceutical Sciences\",\"Biomedical & Allied Health\",\"Computer Science & AI\",\"Physical & Chemical Sciences\",\"Social Development\"]}")
                        .displayOrder(4).published(true).lastUpdatedBy("system").build());
                break;

            case "contact":
                list.add(PageContent.builder()
                        .pageKey("contact").sectionKey("office-info")
                        .title("Editorial Secretariat & Publishing Office")
                        .subtitle("Direct contact channels for authors, reviewers, and academic institutions.")
                        .content("Gono Bishwabidyalay Journal Editorial Office\nAdministrative Building, 2nd Floor\nMirzanagar, Savar, Dhaka 1344, Bangladesh\nEmail: journal@gonouniversity.edu.bd | Phone: +880-2-7792225\nWorking Hours: Sunday – Thursday (09:00 AM – 05:00 PM BST)")
                        .metaJson("{\"email\":\"journal@gonouniversity.edu.bd\",\"phone\":\"+880-2-7792225\",\"location\":\"Mirzanagar, Savar, Dhaka 1344\",\"office\":\"Administrative Building, Room 204\"}")
                        .displayOrder(1).published(true).lastUpdatedBy("system").build());
                break;

            case "editorial-board":
                list.add(PageContent.builder()
                        .pageKey("editorial-board").sectionKey("governance-charter")
                        .title("Editorial Governance & Academic Independence")
                        .subtitle("COPE-aligned editorial leadership and merit-driven decision framework.")
                        .content("The Editorial Board of Gono Bishwabidyalay Journal operates under strict academic independence. Acceptance or rejection of scholarly manuscripts is determined exclusively by rigorous peer review assessment and scientific merit, completely free from commercial, institutional, or political influence.")
                        .metaJson("{\"standards\":\"COPE Compliant\",\"oversight\":\"Double-Blind\",\"tenure\":\"3 Years\"}")
                        .displayOrder(1).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey("editorial-board").sectionKey("advisory-council")
                        .title("International Advisory Council & Section Chairs")
                        .subtitle("Distinguished senior scientists and academic mentors across key faculties.")
                        .content("Our advisory council provides strategic orientation on journal indexing, ethical frameworks, and emerging interdisciplinary research domains spanning Pharmacy, Public Health, Physical Sciences, and Computing Technologies.")
                        .metaJson("{\"disciplines\":[\"Pharmacy & Pharmacology\",\"Biomedical Sciences\",\"Computer Science & AI\",\"Physical Sciences\",\"Community Health\"]}")
                        .displayOrder(2).published(true).lastUpdatedBy("system").build());
                break;

            case "reviewers":
                list.add(PageContent.builder()
                        .pageKey("reviewers").sectionKey("peer-review-protocol")
                        .title("Double-Blind Evaluation Protocol & Rubric")
                        .subtitle("Standardized evaluation guidelines for invited domain specialists.")
                        .content("Reviewers evaluate submissions based on methodological rigor, novelty, ethical compliance, literature integration, and clarity of findings. GB Journal maintains structured 4-week review turnaround windows to ensure timely decisions.")
                        .metaJson("{\"turnaround\":\"28 Days\",\"rubricStages\":[\"Originality\",\"Methodology\",\"Data Validity\",\"Ethics & Clarity\"],\"blindMode\":\"Double-Blind\"}")
                        .displayOrder(1).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey("reviewers").sectionKey("reviewer-benefits")
                        .title("Reviewer Recognition & Academic Acknowledgement")
                        .subtitle("Honoring the vital contribution of our peer review community.")
                        .content("Every reviewer who completes timely evaluations receives verified digital review certificates, annual recognition in the journal volume index, and fast-track submission privileges for their own future manuscripts.")
                        .metaJson("{\"certificate\":\"Official Digital Certificate\",\"indexing\":\"Annual Volume Acknowledgement\",\"priority\":\"Fast-Track Handling\"}")
                        .displayOrder(2).published(true).lastUpdatedBy("system").build());
                break;

            case "issues":
                list.add(PageContent.builder()
                        .pageKey("issues").sectionKey("publication-schedule")
                        .title("Biannual Publication Cadence & Special Volumes")
                        .subtitle("Regular releases scheduled every June and December.")
                        .content("GB Journal publishes two formal issues per annual volume. In addition, the editorial board coordinates special thematic issues addressing breakthrough regional and global scientific developments.")
                        .metaJson("{\"frequency\":\"Biannual (June & December)\",\"format\":\"Open Access Full-Text PDF & Online HTML\",\"indexingDeposit\":\"CrossRef & Repositories\"}")
                        .displayOrder(1).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey("issues").sectionKey("digital-archiving")
                        .title("Permanent Digital Preservation & Archiving")
                        .subtitle("Long-term scholarly preservation across national and institutional repositories.")
                        .content("All issues are permanently preserved with registered DOIs and deposited in institutional digital archives and national scientific databases to ensure uninterrupted scholarly access for global researchers.")
                        .metaJson("{\"doiProvider\":\"CrossRef\",\"license\":\"CC BY 4.0\",\"preservation\":\"Institutional Digital Clock\"}")
                        .displayOrder(2).published(true).lastUpdatedBy("system").build());
                break;

            case "articles":
                list.add(PageContent.builder()
                        .pageKey("articles").sectionKey("repository-scope")
                        .title("Peer-Reviewed Research Repository & Discovery")
                        .subtitle("Immediate full-text open access to high-impact scholarly discoveries.")
                        .content("Browse, search, and download original research manuscripts, comprehensive reviews, and short communications published in the Gono Bishwabidyalay Journal of Science and Technology with zero paywalls.")
                        .metaJson("{\"access\":\"Diamond Open Access\",\"downloadFormat\":\"High-Res PDF\",\"citationFormats\":[\"RIS\",\"BibTeX\",\"APA\"]}")
                        .displayOrder(1).published(true).lastUpdatedBy("system").build());
                list.add(PageContent.builder()
                        .pageKey("articles").sectionKey("citation-metrics")
                        .title("Real-Time Reader Metrics & Impact Analytics")
                        .subtitle("Transparent tracking of article downloads, views, and academic citations.")
                        .content("Each published manuscript features live download counters, view metrics, and CrossRef citation tracking to measure real-world academic impact and dissemination velocity.")
                        .metaJson("{\"metrics\":[\"Abstract Views\",\"PDF Downloads\",\"Citation Counts\",\"Altmetric Velocity\"]}")
                        .displayOrder(2).published(true).lastUpdatedBy("system").build());
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
