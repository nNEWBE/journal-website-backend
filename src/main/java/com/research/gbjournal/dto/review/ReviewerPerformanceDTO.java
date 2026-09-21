package com.research.gbjournal.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerPerformanceDTO {
    private Long reviewerId;
    private String reviewerName;
    private String email;
    private int activeReviews;
    private int completedReviews;
    private int totalInvitations;
    private int maxCapacity;
    private Double onTimeTargetRate;
    private Double avgTurnaroundDays;
    private Double rating;
    private String stressLevel;
    private String stressVariant;
    private String currentActivityText;
    private List<TurnaroundItemDTO> turnaroundHistory;
    private List<MonthlyActivityDTO> monthlyActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TurnaroundItemDTO {
        private String paper;
        private double days;
        private int target;
        private String variance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyActivityDTO {
        private String month;
        private int completed;
        private int onTime;
    }
}
