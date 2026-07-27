package com.research.gbjournal.service;

import com.research.gbjournal.config.MailProperties;
import jakarta.mail.MessagingException;
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

/**
 * Low-level email dispatch service.
 *
 * <p>All sending methods are annotated with {@code @Async} so they execute on a background
 * thread and never block the HTTP request. If SMTP is not configured (e.g. missing credentials),
 * the error is logged as a warning rather than bubbling up to the user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;

    /**
     * Render a Thymeleaf template and send it as an HTML email on the async thread pool.
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
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_NO,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            log.info("Email sent to {} — subject: {}", to, subject);

        } catch (MailException ex) {
            // SMTP connection issue, auth failure, etc. — log but don't crash the application
            log.warn("Failed to send email to {} ({}): {}", to, subject, ex.getMessage());
        } catch (MessagingException ex) {
            log.warn("Failed to build email message for {}: {}", to, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error while sending email to {}: {}", to, ex.getMessage(), ex);
        }
    }
}
