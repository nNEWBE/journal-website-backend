package com.research.gbjournal.service;

import com.research.gbjournal.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Enterprise-grade email dispatch service engineered for maximum inbox deliverability and anti-spam compliance.
 *
 * <p>Key anti-spam & deliverability safeguards implemented:
 * <ul>
 *   <li><b>MIME multipart/alternative:</b> Always bundles a clean plain-text counterpart with the HTML
 *       to satisfy SpamAssassin's strict {@code MIME_HTML_ONLY} and {@code NO_PART_TEXT} checks.</li>
 *   <li><b>RFC 3834 Transactional Headers:</b> Sets {@code Auto-Submitted: auto-generated} and
 *       {@code X-Auto-Response-Suppress: All} to signal legitimate transactional notifications.</li>
 *   <li><b>Legitimate Priority:</b> Explicitly sets normal priority (never Urgent/High) to avoid spam scoring.</li>
 *   <li><b>Google & Yahoo Compliance:</b> Includes {@code List-Unsubscribe} and {@code List-Unsubscribe-Post} headers.</li>
 *   <li><b>RFC 5322 Message-ID:</b> Generates unique fully qualified message IDs adhering to sender domain.</li>
 *   <li><b>Explicit Reply-To:</b> Configures legitimate reply-to handling.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;

    /**
     * Render a Thymeleaf template and send it as a high-deliverability multipart email on the async thread pool.
     *
     * @param to           recipient email address
     * @param subject      email subject line
     * @param templateName template path relative to {@code resources/templates/}, e.g. {@code "email/submission-confirmation"}
     * @param variables    context variables available in the template
     */
    @Async
    public void sendHtml(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // Build Thymeleaf context
            Context ctx = new Context();
            ctx.setVariables(variables);
            // Always inject the journal URL so templates can use ${journalUrl}
            ctx.setVariable("journalUrl", mailProperties.getJournalUrl());

            String htmlContent = templateEngine.process(templateName, ctx);

            MimeMessage message = mailSender.createMimeMessage();

            // true flag enables multipart/alternative mode (RFC 2046 compliant)
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name());

            String fromAddress = (mailProperties.getFrom() != null && !mailProperties.getFrom().isBlank())
                    ? mailProperties.getFrom().trim()
                    : "no-reply@localhost";
            String fromName = (mailProperties.getFromName() != null && !mailProperties.getFromName().isBlank())
                    ? mailProperties.getFromName().trim()
                    : "Gono Bishwabidyalay Journal";

            // 1. Authenticated Sender & Recipient
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(to.trim());
            helper.setSubject(subject);

            // 2. Explicit Reply-To address
            String replyToAddress = (mailProperties.getReplyTo() != null && !mailProperties.getReplyTo().isBlank())
                    ? mailProperties.getReplyTo().trim()
                    : fromAddress;
            helper.setReplyTo(new InternetAddress(replyToAddress, fromName, StandardCharsets.UTF_8.name()));

            // 3. Multi-part Alternative (Plain text + HTML)
            // Critical for anti-spam: Spam engines penalize emails lacking a plain-text counterpart
            String plainTextContent = htmlToPlainText(htmlContent);
            helper.setText(plainTextContent, htmlContent);

            // 4. Anti-Spam & Deliverability Headers
            // A. RFC 3834 Automated Transactional Dispatch (signals system email, not promotional spam)
            message.setHeader("Auto-Submitted", "auto-generated");
            message.setHeader("X-Auto-Response-Suppress", "All");
            message.setHeader("Precedence", "bulk");

            // B. Standard Normal Priority (Urgent/High priority triggers spam heuristics)
            message.setHeader("X-Priority", "3");
            message.setHeader("Priority", "Normal");
            message.setHeader("Importance", "Normal");

            // C. Identification & Organization
            message.setHeader("Organization", "Gono Bishwabidyalay Journal of Science & Technology");
            message.setHeader("X-Mailer", "GBJournal-Editorial-Engine/1.0");

            // D. RFC-compliant Unique Message-ID
            String domain = fromAddress.contains("@") ? fromAddress.substring(fromAddress.indexOf("@") + 1) : "gonouniversity.edu.bd";
            message.setHeader("Message-ID", "<" + UUID.randomUUID() + "@" + domain + ">");

            // E. Google & Yahoo 2024 Sender Compliance: List-Unsubscribe
            String journalUrl = (mailProperties.getJournalUrl() != null && !mailProperties.getJournalUrl().isBlank())
                    ? mailProperties.getJournalUrl().trim()
                    : "https://gonouniversity-journal.vercel.app";
            String unsubUrl = journalUrl + "/contact";
            message.setHeader("List-Unsubscribe", "<" + unsubUrl + ">, <mailto:" + fromAddress + "?subject=unsubscribe>");
            message.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");

            mailSender.send(message);
            log.info("Email successfully dispatched to {} — subject: '{}' [multipart/alternative]", to, subject);

        } catch (MailException ex) {
            log.warn("SMTP delivery failed for recipient '{}' (Subject: '{}'). Error: {}", to, subject, ex.getMessage());
        } catch (MessagingException ex) {
            log.warn("Failed to construct email message for recipient '{}': {}", to, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error while dispatching email to '{}': {}", to, ex.getMessage(), ex);
        }
    }

    /**
     * Converts HTML email content into clean, readable plain text for the multipart/alternative body.
     * Spam filters require a genuine text/plain alternative; missing it flags MIME_HTML_ONLY.
     */
    private String htmlToPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        // Normalize structural tags into clean newlines
        String text = html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("(?i)</tr>", "\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("(?i)<li[^>]*>", "• ")
                .replaceAll("(?i)<h[1-6][^>]*>", "\n\n")
                .replaceAll("(?i)</h[1-6]>", "\n\n");

        // Convert links: <a href="url">anchor text</a> -> anchor text (url)
        text = text.replaceAll("(?i)<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", "$2 ($1)");

        // Strip remaining HTML tags
        text = text.replaceAll("<[^>]+>", "");

        // Decode common HTML entities
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&bull;", "•");

        // Collapse multiple whitespace/blank lines
        text = text.replaceAll("(?m)^[ \t]*\r?\n", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();

        return text;
    }
}
