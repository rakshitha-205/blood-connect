package com.bloodconnect.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class AlertMailer {

    private static final Logger log = LoggerFactory.getLogger(AlertMailer.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean mailConfigured;
    private final String mailFrom;

    public AlertMailer(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${spring.mail.host:}") String mailHost,
                       @Value("${spring.mail.username:}") String mailUsername,
                       @Value("${bloodconnect.otp.mail-from}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailConfigured = !mailHost.isBlank() && !mailUsername.isBlank();
        this.mailFrom = mailFrom.isBlank() ? mailUsername : mailFrom;
    }

    /** Runs after the database commit on a background thread, so mail problems never fail the request. */
    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void send(AlertEmail.Batch batch) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !mailConfigured) {
            batch.emails().forEach(email -> log.info("No mail server configured. Alert email for {}: {}",
                    email.to(), email.subject()));
            return;
        }
        batch.emails().forEach(email -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email.to());
            message.setSubject(email.subject());
            message.setText(email.text());
            try {
                sender.send(message);
            } catch (MailException exception) {
                log.warn("Could not send alert email to {}: {}", email.to(), exception.getMessage());
            }
        });
    }
}
