package com.research.gbjournal.config;

import com.research.gbjournal.entity.*;
import com.research.gbjournal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the development database with demo users, board members, issues,
 * articles,
 * and sample submissions on every application startup (since dev uses
 * create-drop).
 *
 * The seeded demo password for all accounts is: demopass
 * (matches the frontend's current hardcoded authenticate() function)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final IssueRepository issueRepository;
    private final ArticleRepository articleRepository;
    private final SubmissionRepository submissionRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEMO_PASSWORD = "demopass";

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== DataInitializer: Seeding database ===");
        seedUsers();
        seedBoardMembers();
        seedIssuesAndArticles();
        seedSubmissions();
        log.info("=== DataInitializer: Seeding complete ===");
    }

    // =========================================
    // USERS
    // =========================================

    private void seedUsers() {
        if (userRepository.count() > 0)
            return;

        String encoded = passwordEncoder.encode(DEMO_PASSWORD);

        List<User> users = List.of(
                User.builder()
                        .fullName("Prof. Dr. Laila Rahman")
                        .email("superadmin@gonouniversity.edu.bd")
                        .password(encoded)
                        .role(User.Role.SUPER_ADMIN)
                        .title("Editor-in-Chief & Administrator")
                        .department("Faculty of Health Sciences")
                        .institution("Gono Bishwabidyalay")
                        .country("BD")
                        .enabled(true)
                        .emailVerified(true)
                        .build(),

                User.builder()
                        .fullName("Md. Jamil Hossain")
                        .email("admin@gonouniversity.edu.bd")
                        .password(encoded)
                        .role(User.Role.ADMIN)
                        .title("System Administrator")
                        .department("Journal Operations")
                        .institution("Gono Bishwabidyalay")
                        .country("BD")
                        .enabled(true)
                        .emailVerified(true)
                        .build(),

                User.builder()
                        .fullName("Prof. Saiful Islam")
                        .email("editor@gonouniversity.edu.bd")
                        .password(encoded)
                        .role(User.Role.EDITOR)
                        .title("Managing Editor")
                        .department("Department of Pharmacy")
                        .institution("Gono Bishwabidyalay")
                        .country("BD")
                        .enabled(true)
                        .emailVerified(true)
                        .build(),

                User.builder()
                        .fullName("Dr. Salma Khatun")
                        .email("reviewer@gonouniversity.edu.bd")
                        .password(encoded)
                        .role(User.Role.REVIEWER)
                        .title("Peer Reviewer")
                        .department("Department of Microbiology")
                        .institution("Gono Bishwabidyalay")
                        .country("BD")
                        .enabled(true)
                        .emailVerified(true)
                        .build(),

                User.builder()
                        .fullName("Ayesha Siddique")
                        .email("author@gonouniversity.edu.bd")
                        .password(encoded)
                        .role(User.Role.AUTHOR)
                        .title("Researcher")
                        .department("Department of Public Health")
                        .institution("Gono Bishwabidyalay")
                        .country("BD")
                        .enabled(true)
                        .emailVerified(true)
                        .build());

        userRepository.saveAll(users);
        log.info("Seeded {} demo users", users.size());
    }

    // =========================================
    // BOARD MEMBERS
    // =========================================

    private void seedBoardMembers() {
        if (boardMemberRepository.count() > 0)
            return;

        List<BoardMember> members = List.of(
                BoardMember.builder()
                        .name("Prof. Dr. Laila Rahman")
                        .role("Editor-in-Chief")
                        .unit("Gono Bishwabidyalay")
                        .expertise("Public health, community medicine")
                        .sortOrder(1)
                        .build(),

                BoardMember.builder()
                        .name("Prof. Saiful Islam")
                        .role("Managing Editor")
                        .unit("Department of Pharmacy")
                        .expertise("Pharmacy education, antimicrobial stewardship")
                        .sortOrder(2)
                        .build(),

                BoardMember.builder()
                        .name("Dr. Rehana Akter")
                        .role("Section Editor")
                        .unit("Department of Law")
                        .expertise("Governance, access to justice")
                        .sortOrder(3)
                        .build(),

                BoardMember.builder()
                        .name("Dr. Mahbub Alam")
                        .role("Section Editor")
                        .unit("Faculty of Agriculture")
                        .expertise("Climate adaptation, rural systems")
                        .sortOrder(4)
                        .build(),

                BoardMember.builder()
                        .name("Dr. Nasima Begum")
                        .role("Reviewer Board")
                        .unit("Department of Microbiology")
                        .expertise("Infectious diseases, antimicrobials")
                        .sortOrder(5)
                        .build());

        boardMemberRepository.saveAll(members);
        log.info("Seeded {} board members", members.size());
    }

    // =========================================
    // ISSUES + ARTICLES
    // =========================================

    private void seedIssuesAndArticles() {
        if (issueRepository.count() > 0)
            return;

        // ---- Issue Vol 4, Issue 2 (CURRENT) ----
        Issue currentIssue = issueRepository.save(Issue.builder()
                .issueKey("2026-2")
                .year("2026")
                .volumeLabel("Volume 4")
                .issueLabel("Issue 2")
                .month("July 2026")
                .theme("Community Health, Stewardship, and Resilient Systems")
                .articleCount(3)
                .current(true)
                .editorNote("This issue presents community-focused research that bridges applied " +
                        "scholarship with real-world public service in the Bangladeshi context.")
                .build());

        // ---- Issue Vol 4, Issue 1 ----
        Issue issue2026_1 = issueRepository.save(Issue.builder()
                .issueKey("2026-1")
                .year("2026")
                .volumeLabel("Volume 4")
                .issueLabel("Issue 1")
                .month("January 2026")
                .theme("Governance, Learning, and Social Transformation")
                .articleCount(2)
                .current(false)
                .build());

        log.info("Seeded 4 issues");

        // ---- Articles for current issue ----
        seedArticle1(currentIssue);
        seedArticle2(currentIssue);
        seedArticle3(currentIssue);
        seedArticle4(issue2026_1);
        seedArticle5(issue2026_1);

        log.info("Seeded 5 articles");
    }

    private void seedArticle1(Issue issue) {
        Article article = Article.builder()
                .articleId("ART-2026-001")
                .slug("community-healthcare-access-savar")
                .title("Community healthcare access patterns around Savar: A mixed-method university catchment study")
                .type("Research Article")
                .topic("Public Health")
                .department("Faculty of Health Sciences")
                .abstractText("This study maps healthcare access, referral barriers, and household-level service " +
                        "confidence across communities surrounding the Gono Bishwabidyalay catchment area.")
                .issueLabel("Issue 2")
                .volumeLabel("Volume 4")
                .pages("11-28")
                .doi("10.5555/gbj.2026.001")
                .publishedAt("July 2026")
                .issue(issue)
                .metrics(ArticleMetrics.builder().views(2840).downloads(731).citations(12).build())
                .openAccess(true)
                .pdfAvailable(true)
                .pdfUrl("/pdfs/community-healthcare-access-savar.pdf")
                .imageUrl("/covers/medical.png")
                .build();

        addAuthors(article, List.of("Dr. Farhana Rahman", "Md. Jamil Hossain", "Nusrat A. Karim"));
        addKeywords(article, List.of("community health", "primary care", "Bangladesh", "Savar"));
        addSections(article, List.of(
                new String[] { "Abstract",
                        "A community-focused survey and interview program identified practical barriers " +
                                "in transport, appointment literacy, referral follow-up, and health information trust." },
                new String[] { "Key Points", "Patients valued proximity and known providers, but referral complexity " +
                        "reduced timely care. Mobile reminders and community health volunteers were repeatedly identified." },
                new String[] { "Methods",
                        "The study combined structured household surveys with semi-structured interviews. " +
                                "Responses were coded by access theme and compared across age, gender, income, and clinic contact." },
                new String[] { "Conclusion", "University-based health systems can become trusted local bridges when " +
                        "referral support, appointment guidance, and follow-up communication are part of the care journey." }));
        articleRepository.save(article);
    }

    private void seedArticle2(Issue issue) {
        Article article = Article.builder()
                .articleId("ART-2026-002")
                .slug("pharmacy-practice-antimicrobial-stewardship")
                .title("Pharmacy practice readiness for antimicrobial stewardship in teaching settings")
                .type("Review Article")
                .topic("Pharmacy")
                .department("Department of Pharmacy")
                .abstractText("A review of stewardship education, dispensing governance, and clinical collaboration " +
                        "models for pharmacy students and teaching pharmacies.")
                .issueLabel("Issue 2")
                .volumeLabel("Volume 4")
                .pages("29-44")
                .doi("10.5555/gbj.2026.002")
                .publishedAt("July 2026")
                .issue(issue)
                .metrics(ArticleMetrics.builder().views(1935).downloads(502).citations(8).build())
                .openAccess(true)
                .pdfAvailable(true)
                .pdfUrl("/pdfs/pharmacy-practice-antimicrobial-stewardship.pdf")
                .imageUrl("/covers/pharmacy.png")
                .build();

        addAuthors(article, List.of("Prof. Saiful Islam", "Tania Sultana"));
        addKeywords(article, List.of("pharmacy", "antimicrobial stewardship", "education"));
        addSections(article, List.of(
                new String[] { "Abstract", "Teaching pharmacies can support antimicrobial stewardship by combining " +
                        "curriculum, dispensing audits, and physician-pharmacist collaboration." },
                new String[] { "Practice Implications",
                        "The most feasible early interventions are student-led counseling " +
                                "checklists, prescription review simulations, and supervised community awareness activities." },
                new String[] { "Conclusion", "Stewardship should be treated as an applied professional habit, " +
                        "not a late-stage theoretical topic." }));
        articleRepository.save(article);
    }

    private void seedArticle3(Issue issue) {
        Article article = Article.builder()
                .articleId("ART-2026-003")
                .slug("climate-resilient-agriculture-manifolds")
                .title("Climate-resilient smallholder agriculture: Field observations from central Bangladesh")
                .type("Case Study")
                .topic("Agriculture")
                .department("Faculty of Agriculture")
                .abstractText("A field case study documents practical adaptation strategies used by smallholder " +
                        "farming communities under changing rainfall patterns.")
                .issueLabel("Issue 2")
                .volumeLabel("Volume 4")
                .pages("45-59")
                .doi("10.5555/gbj.2026.003")
                .publishedAt("July 2026")
                .issue(issue)
                .metrics(ArticleMetrics.builder().views(1430).downloads(378).citations(6).build())
                .openAccess(true)
                .pdfAvailable(true)
                .pdfUrl("/pdfs/climate-resilient-agriculture-manifolds.pdf")
                .imageUrl("/covers/agriculture.png")
                .build();

        addAuthors(article, List.of("Dr. Mahbub Alam", "Sharmin Jahan"));
        addKeywords(article, List.of("climate", "agriculture", "adaptation"));
        addSections(article, List.of(
                new String[] { "Abstract",
                        "Farmers combine crop diversification, local seed exchange, water retention, " +
                                "and cooperative labor to reduce seasonal uncertainty." },
                new String[] { "Field Observations",
                        "Participants emphasized practical risk-sharing, short-cycle crops, " +
                                "and the value of local knowledge networks in deciding when to plant or delay." },
                new String[] { "Conclusion", "Climate resilience depends on both agronomic technique and the social " +
                        "infrastructure that helps farmers act on time." }));
        articleRepository.save(article);
    }

    private void seedArticle4(Issue issue) {
        Article article = Article.builder()
                .articleId("ART-2026-004")
                .slug("legal-aid-university-clinic")
                .title("University legal aid clinics and access to justice: A governance perspective")
                .type("Perspective")
                .topic("Law and Governance")
                .department("Department of Law")
                .abstractText("This perspective argues for structured legal aid clinics as both a pedagogical model " +
                        "and a public-interest service pathway.")
                .issueLabel("Issue 1")
                .volumeLabel("Volume 4")
                .pages("71-82")
                .doi("10.5555/gbj.2026.004")
                .publishedAt("January 2026")
                .issue(issue)
                .metrics(ArticleMetrics.builder().views(990).downloads(221).citations(3).build())
                .openAccess(true)
                .pdfAvailable(false)
                .build();

        addAuthors(article, List.of("Dr. Rehana Akter"));
        addKeywords(article, List.of("law clinic", "governance", "access to justice"));
        addSections(article, List.of(
                new String[] { "Abstract",
                        "Legal aid clinics can translate classroom learning into supervised public service. " +
                                "The model requires confidentiality rules, referral protocols, and careful case supervision." },
                new String[] { "Governance Model",
                        "A clinic charter, faculty supervision board, student ethics agreement, " +
                                "and external partner network create the minimum structure for responsible operation." },
                new String[] { "Conclusion", "A university clinic can become a trusted access point when education, " +
                        "ethics, and public service are designed together." }));
        articleRepository.save(article);
    }

    private void seedArticle5(Issue issue) {
        Article article = Article.builder()
                .articleId("ART-2026-005")
                .slug("ai-assisted-learning-private-universities")
                .title("AI-assisted learning in private universities: Student confidence, risk, and academic integrity")
                .type("Research Article")
                .topic("Technology")
                .department("Center for Teaching and Learning")
                .abstractText("A cross-sectional study of student use of AI tools, perceived learning benefit, " +
                        "and uncertainty around academic integrity expectations.")
                .issueLabel("Issue 1")
                .volumeLabel("Volume 4")
                .pages("83-101")
                .doi("10.5555/gbj.2026.005")
                .publishedAt("January 2026")
                .issue(issue)
                .metrics(ArticleMetrics.builder().views(3120).downloads(814).citations(18).build())
                .openAccess(true)
                .pdfAvailable(true)
                .pdfUrl("/pdfs/ai-assisted-learning-private-universities.pdf")
                .imageUrl("/covers/technology.png")
                .build();

        addAuthors(article, List.of("Md. Rafiq Hasan", "Samia Noor", "Dr. Arup Chandra"));
        addKeywords(article, List.of("AI", "higher education", "academic integrity"));
        addSections(article, List.of(
                new String[] { "Abstract", "Students reported high experimentation with AI tools but uneven confidence "
                        +
                        "in citation, disclosure, and acceptable use. Clear policy examples were preferred over general warnings." },
                new String[] { "Findings",
                        "The strongest predictor of responsible use was not technical confidence but " +
                                "whether instructors provided task-specific boundaries." },
                new String[] { "Conclusion", "Academic integrity policy should be course-visible, example-driven, " +
                        "and paired with learning design rather than punishment alone." }));
        articleRepository.save(article);
    }

    // =========================================
    // SUBMISSIONS
    // =========================================

    private void seedSubmissions() {
        if (submissionRepository.count() > 0)
            return;

        User author = userRepository.findByEmailIgnoreCase("author@gonouniversity.edu.bd").orElse(null);
        User editor = userRepository.findByEmailIgnoreCase("editor@gonouniversity.edu.bd").orElse(null);
        User reviewer = userRepository.findByEmailIgnoreCase("reviewer@gonouniversity.edu.bd").orElse(null);

        if (author == null || editor == null || reviewer == null)
            return;

        // Submission 1 — Under Review
        Submission s1 = submissionRepository.save(Submission.builder()
                .submissionId("GBJ-2026-104")
                .title("Mental health service confidence among first-year university students")
                .type("Research Article")
                .topic("Public Health")
                .abstractText("A cross-sectional survey of mental health service attitudes among first-year " +
                        "students at Gono Bishwabidyalay.")
                .keywords("mental health, university students, service confidence")
                .status(Submission.SubmissionStatus.UNDER_REVIEW)
                .submittingAuthor(author)
                .assignedEditor(editor)
                .copyrightAgreed(true)
                .submittedAt(java.time.Instant.now().minusSeconds(86400 * 5))
                .build());

        // Assign reviewer
        reviewAssignmentRepository.save(ReviewAssignment.builder()
                .submission(s1)
                .reviewer(reviewer)
                .status(ReviewAssignment.ReviewStatus.ACCEPTED)
                .dueDate(java.time.Instant.now().plusSeconds(86400 * 14))
                .build());

        // Submission 2 — Revision Requested
        submissionRepository.save(Submission.builder()
                .submissionId("GBJ-2026-103")
                .title("Veterinary teleconsultation readiness in peri-urban farms")
                .type("Short Communication")
                .topic("Veterinary Sciences")
                .abstractText("An exploratory study of teleconsultation adoption among peri-urban farm owners " +
                        "near Savar, Bangladesh.")
                .keywords("veterinary, teleconsultation, peri-urban")
                .status(Submission.SubmissionStatus.REVISION_REQUESTED)
                .submittingAuthor(author)
                .assignedEditor(editor)
                .editorDecisionNote("Interesting topic — please expand the methodology section and clarify sampling.")
                .copyrightAgreed(true)
                .submittedAt(java.time.Instant.now().minusSeconds(86400 * 10))
                .decisionDate(java.time.Instant.now().minusSeconds(86400 * 2))
                .build());

        // Submission 3 — Draft
        submissionRepository.save(Submission.builder()
                .submissionId("GBJ-2026-105")
                .title("Role of traditional medicine in rural healthcare in Bangladesh")
                .type("Review Article")
                .topic("Public Health")
                .abstractText("A literature review of traditional medicine practices and their integration into " +
                        "primary healthcare in rural Bangladesh.")
                .status(Submission.SubmissionStatus.DRAFT)
                .submittingAuthor(author)
                .copyrightAgreed(false)
                .build());

        // Submission 4 — Accepted / Copyediting
        submissionRepository.save(Submission.builder()
                .submissionId("GBJ-2026-102")
                .title("Pharmacy dispensing practices and cold-chain integrity during monsoon flooding")
                .type("Research Article")
                .topic("Pharmacy")
                .abstractText("A multi-district investigation into pharmaceutical supply chain resilience and medicine quality retention under acute monsoon conditions.")
                .keywords("pharmacy, supply chain, monsoon, disaster preparedness")
                .status(Submission.SubmissionStatus.COPYEDITING)
                .submittingAuthor(author)
                .assignedEditor(editor)
                .copyrightAgreed(true)
                .submittedAt(java.time.Instant.now().minusSeconds(86400 * 20))
                .decisionDate(java.time.Instant.now().minusSeconds(86400 * 4))
                .build());

        // Submission 5 — Scheduled
        submissionRepository.save(Submission.builder()
                .submissionId("GBJ-2026-101")
                .title("Community-led micro-sanitation initiatives in riverine deltas")
                .type("Case Study")
                .topic("Social Sciences")
                .abstractText("Case evaluations of indigenous collective actions and water quality monitoring in deltaic communities.")
                .keywords("sanitation, water quality, community governance")
                .status(Submission.SubmissionStatus.SCHEDULED)
                .submittingAuthor(author)
                .assignedEditor(editor)
                .copyrightAgreed(true)
                .submittedAt(java.time.Instant.now().minusSeconds(86400 * 30))
                .decisionDate(java.time.Instant.now().minusSeconds(86400 * 8))
                .build());

        log.info("Seeded 5 sample submissions covering full workflow");
    }

    // =========================================
    // HELPERS
    // =========================================

    private void addAuthors(Article article, List<String> names) {
        for (int i = 0; i < names.size(); i++) {
            article.getAuthors().add(ArticleAuthor.builder()
                    .article(article)
                    .name(names.get(i))
                    .authorOrder(i + 1)
                    .corresponding(i == 0)
                    .build());
        }
    }

    private void addKeywords(Article article, List<String> keywords) {
        for (String keyword : keywords) {
            article.getKeywords().add(ArticleKeyword.builder()
                    .article(article)
                    .keyword(keyword)
                    .build());
        }
    }

    private void addSections(Article article, List<String[]> sections) {
        for (int i = 0; i < sections.size(); i++) {
            article.getSections().add(ArticleSection.builder()
                    .article(article)
                    .heading(sections.get(i)[0])
                    .body(sections.get(i)[1])
                    .sortOrder(i + 1)
                    .build());
        }
    }
}
