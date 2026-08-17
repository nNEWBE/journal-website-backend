package com.research.gbjournal.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private String id;
    private String eventType; // "USER_REGISTERED", "SUBMISSION_CREATED", "STATUS_CHANGE", "ROLE_UPDATED", "MAIL_DISPATCHED", "ISSUE_PUBLISHED"
    private String description;
    private String actor;
    private String target;
    private Instant timestamp;
    private String level; // "INFO", "WARN", "SUCCESS"
}
