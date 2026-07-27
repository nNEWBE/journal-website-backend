package com.research.gbjournal.dto.submission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SubmissionCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500)
    private String title;

    @Size(max = 200)
    private String runningTitle;

    @NotBlank(message = "Article type is required")
    @Size(max = 80)
    private String type;

    @Size(max = 5000)
    private String abstractText;

    @Size(max = 500)
    private String keywords;

    @Size(max = 100)
    private String topic;

    @Size(max = 3000)
    private String coverLetter;

    @Valid
    private List<CoAuthorRequest> authors;

    // Declarations
    private String conflictOfInterest;
    private String fundingStatement;
    private String ethicsStatement;
    private String dataAvailability;
    private String aiDeclaration;
    private boolean copyrightAgreed;

    @Data
    public static class CoAuthorRequest {
        @NotBlank
        private String name;
        private String email;
        private String affiliation;
        private String orcid;
        private int authorOrder;
        private boolean corresponding;
    }
}
