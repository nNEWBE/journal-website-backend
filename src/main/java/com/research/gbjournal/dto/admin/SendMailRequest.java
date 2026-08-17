package com.research.gbjournal.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMailRequest {
    /** Target audience: "INDIVIDUAL", "ALL_AUTHORS", "ALL_REVIEWERS", "ALL_EDITORS", "ALL_USERS" */
    @NotBlank(message = "Audience type is required")
    private String audience;

    /** If audience is "INDIVIDUAL", specific email or comma-separated emails */
    private String recipientEmail;

    /** Specific user IDs if custom targeted */
    private List<Long> userIds;

    @NotBlank(message = "Email subject is required")
    private String subject;

    @NotBlank(message = "Email content body is required")
    private String messageBody;

    /** Optional template key */
    private String templateKey;
}
