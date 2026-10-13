package com.bloodconnect.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OtpDelivery {

    private static final Logger log = LoggerFactory.getLogger(OtpDelivery.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean mailConfigured;
    private final String mailFrom;
    private final String twilioAccountSid;
    private final String twilioAuthToken;
    private final String twilioFromNumber;
    private final RestClient restClient;

    public OtpDelivery(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${spring.mail.host:}") String mailHost,
                       @Value("${spring.mail.username:}") String mailUsername,
                       @Value("${bloodconnect.otp.mail-from}") String mailFrom,
                       @Value("${bloodconnect.otp.twilio.account-sid:}") String twilioAccountSid,
                       @Value("${bloodconnect.otp.twilio.auth-token:}") String twilioAuthToken,
                       @Value("${bloodconnect.otp.twilio.from-number:}") String twilioFromNumber,
                       RestClient.Builder restClientBuilder) {
        this.mailSender = mailSender;
        this.mailConfigured = !mailHost.isBlank() && !mailUsername.isBlank();
        this.mailFrom = mailFrom.isBlank() ? mailUsername : mailFrom;
        this.twilioAccountSid = twilioAccountSid;
        this.twilioAuthToken = twilioAuthToken;
        this.twilioFromNumber = twilioFromNumber;
        this.restClient = restClientBuilder.baseUrl("https://api.twilio.com/2010-04-01").build();
    }

    /** Returns false when no mail server is configured and the code was only logged. */
    public boolean sendEmail(String to, String code) {
        return sendMail(to, code, "Your BloodConnect verification code", body(code));
    }

    /** Returns false when no mail server is configured and the code was only logged. */
    public boolean sendPasswordResetEmail(String to, String code) {
        return sendMail(to, code, "Reset your BloodConnect password", code
                + " is your BloodConnect password reset code. It expires in 10 minutes.\n\n"
                + "If you didn't ask to reset your password, you can ignore this email. Your password stays the same.");
    }

    private boolean sendMail(String to, String code, String subject, String text) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !mailConfigured) {
            log.info("No mail server configured. Email code for {}: {}", to, code);
            return false;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            sender.send(message);
            return true;
        } catch (MailException exception) {
            log.warn("Could not send OTP email to {}: {}", to, exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "We couldn't send the email right now. Please try again in a minute.");
        }
    }

    /** Returns false when no SMS provider is configured and the code was only logged. */
    public boolean sendSms(String to, String code) {
        if (twilioAccountSid.isBlank() || twilioAuthToken.isBlank() || twilioFromNumber.isBlank()) {
            log.info("No SMS provider configured. Phone OTP for {}: {}", to, code);
            return false;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", to);
        form.add("From", twilioFromNumber);
        form.add("Body", body(code));
        try {
            restClient.post()
                    .uri("/Accounts/{sid}/Messages.json", twilioAccountSid)
                    .headers(headers -> headers.setBasicAuth(twilioAccountSid, twilioAuthToken))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            log.warn("Could not send OTP SMS to {}: {}", to, exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "We couldn't send the SMS right now. Check the number or try again in a minute.");
        }
    }

    private static String body(String code) {
        return code + " is your BloodConnect verification code. It expires in 10 minutes. Don't share it with anyone.";
    }
}
