package com.research.gbjournal.service;

import com.research.gbjournal.dto.article.ArticleDTO;
import com.research.gbjournal.dto.issue.IssueDTO;
import com.research.gbjournal.entity.Issue;
import com.research.gbjournal.exception.ResourceNotFoundException;
import com.research.gbjournal.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ArticleService articleService;

    // ===== Current Issue =====

    @Transactional(readOnly = true)
    public IssueDTO getCurrentIssue() {
        Issue issue = issueRepository.findByCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No current issue has been set."));
        return toDetailedDTO(issue);
    }

    // ===== Archive =====

    @Transactional(readOnly = true)
    public List<IssueDTO> getAllIssues() {
        return issueRepository.findAllByOrderByYearDescIssueLabelDesc()
                .stream()
                .map(iss -> toSummaryDTO(iss))
                .toList();
    }

    // ===== Issue by key =====

    @Transactional(readOnly = true)
    public IssueDTO getIssueByKey(String issueKey) {
        Issue issue = issueRepository.findByIssueKey(issueKey)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "key", issueKey));
        return toDetailedDTO(issue);
    }

    // ===== Admin: set current issue =====

    @Transactional
    public IssueDTO setCurrentIssue(Long issueId) {
        issueRepository.clearCurrentIssue();
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));
        issue.setCurrent(true);
        issueRepository.save(issue);
        return toSummaryDTO(issue);
    }

    @Transactional
    public IssueDTO createIssue(IssueDTO dto) {
        if (dto.isCurrent()) {
            issueRepository.clearCurrentIssue();
        }
        Issue issue = Issue.builder()
                .issueKey(dto.getIssueKey())
                .year(dto.getYear())
                .volumeLabel(dto.getVolumeLabel())
                .issueLabel(dto.getIssueLabel())
                .month(dto.getMonth())
                .theme(dto.getTheme())
                .articleCount(dto.getArticleCount())
                .current(dto.isCurrent())
                .coverImageUrl(dto.getCoverImageUrl())
                .editorNote(dto.getEditorNote())
                .build();
        issueRepository.save(issue);
        return toSummaryDTO(issue);
    }

    @Transactional
    public IssueDTO updateIssue(Long issueId, IssueDTO dto) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));
        if (dto.isCurrent() && !issue.isCurrent()) {
            issueRepository.clearCurrentIssue();
        }
        issue.setYear(dto.getYear());
        issue.setVolumeLabel(dto.getVolumeLabel());
        issue.setIssueLabel(dto.getIssueLabel());
        issue.setMonth(dto.getMonth());
        issue.setTheme(dto.getTheme());
        issue.setCurrent(dto.isCurrent());
        issue.setCoverImageUrl(dto.getCoverImageUrl());
        issue.setEditorNote(dto.getEditorNote());
        issueRepository.save(issue);
        return toSummaryDTO(issue);
    }

    // ===== Mappers =====

    private IssueDTO toSummaryDTO(Issue issue) {
        List<ArticleDTO> articles = issue.getArticles() != null
                ? issue.getArticles().stream()
                        .map(art -> articleService.toDTO(art))
                        .toList()
                : List.of();

        return IssueDTO.builder()
                .id(issue.getId())
                .issueKey(issue.getIssueKey())
                .year(issue.getYear())
                .volumeLabel(issue.getVolumeLabel())
                .issueLabel(issue.getIssueLabel())
                .month(issue.getMonth())
                .theme(issue.getTheme())
                .articleCount(issue.getArticleCount())
                .current(issue.isCurrent())
                .coverImageUrl(issue.getCoverImageUrl())
                .editorNote(issue.getEditorNote())
                .articles(articles)
                .build();
    }

    private IssueDTO toDetailedDTO(Issue issue) {
        List<ArticleDTO> articles = issue.getArticles() != null
                ? issue.getArticles().stream()
                        .map(art -> articleService.toDTO(art))
                        .toList()
                : List.of();

        Map<String, List<ArticleDTO>> byType = articles.stream()
                .collect(Collectors.groupingBy(art -> art.getType() != null ? art.getType() : "General"));

        return IssueDTO.builder()
                .id(issue.getId())
                .issueKey(issue.getIssueKey())
                .year(issue.getYear())
                .volumeLabel(issue.getVolumeLabel())
                .issueLabel(issue.getIssueLabel())
                .month(issue.getMonth())
                .theme(issue.getTheme())
                .articleCount(issue.getArticleCount())
                .current(issue.isCurrent())
                .coverImageUrl(issue.getCoverImageUrl())
                .editorNote(issue.getEditorNote())
                .articles(articles)
                .articlesByType(byType)
                .build();
    }
}
