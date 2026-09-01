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

    /** Sender address shown to recipients, loaded from MAIL_FROM environment variable */
    private String from;

    /** Display name shown in e-mail clients, loaded from MAIL_FROM_NAME */
    private String fromName;

    /** Public URL of the frontend journal website, loaded from JOURNAL_URL */
    private String journalUrl;
}
