package com.research.gbjournal.service;

import com.research.gbjournal.dto.admin.DashboardStatsDTO;
import com.research.gbjournal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SubmissionRepository submissionRepository;
    private final ArticleRepository articleRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ReviewAssignmentRepository reviewAssignmentRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getStats() {
        return DashboardStatsDTO.builder()
                .activeSubmissions(submissionRepository.countActiveSubmissions())
                .underReview(submissionRepository.countUnderReview())
                .accepted(submissionRepository.countAccepted())
                .publishedArticles(articleRepository.count())
                .activeReviewers(reviewAssignmentRepository.countActiveReviewers())
                .publishedIssues(issueRepository.count())
                .registeredUsers(userRepository.count())
                .build();
    }
}
