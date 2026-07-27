package com.research.gbjournal.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDTO {

    private long activeSubmissions;
    private long underReview;
    private long accepted;
    private long publishedArticles;
    private long activeReviewers;
    private long publishedIssues;
    private long registeredUsers;
}
