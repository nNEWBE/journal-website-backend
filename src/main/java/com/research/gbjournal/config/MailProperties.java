package com.research.gbjournal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code app.mail.*} properties from application yaml.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** Sender address shown to recipients, e.g. no-reply@gonouniversity.edu.bd */
    private String from = "no-reply@gonouniversity.edu.bd";

    /** Display name shown in e-mail clients */
    private String fromName = "GBJ — Gono Bishwabidyalay Journal";

    /** Public URL of the frontend journal website */
    private String journalUrl = "http://localhost:3000";
}
