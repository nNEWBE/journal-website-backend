package com.research.gbjournal.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReviewRequest {

    @NotBlank(message = "Review comments are required")
    private String reviewComments;

    private String confidentialComments;

    @NotBlank(message = "Recommendation is required")
    private String recommendation; // ACCEPT, MINOR_REVISION, MAJOR_REVISION, REJECT

    @NotNull
    @Min(1) @Max(100)
    private Integer score;
}
