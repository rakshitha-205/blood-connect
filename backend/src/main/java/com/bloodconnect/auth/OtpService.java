package com.bloodconnect.auth;

import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserRepository;
import com.bloodconnect.user.UserResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class OtpService {

    static final Duration CODE_LIFETIME = Duration.ofMinutes(10);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final OtpCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpDelivery delivery;
    private final boolean exposeDevCode;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public OtpService(UserRepository userRepository, OtpCodeRepository codeRepository, PasswordEncoder passwordEncoder,
                      OtpDelivery delivery, @Value("${bloodconnect.otp.expose-dev-code:false}") boolean exposeDevCode) {
        this(userRepository, codeRepository, passwordEncoder, delivery, exposeDevCode, Clock.systemUTC());
    }

    OtpService(UserRepository userRepository, OtpCodeRepository codeRepository, PasswordEncoder passwordEncoder,
               OtpDelivery delivery, boolean exposeDevCode, Clock clock) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.passwordEncoder = passwordEncoder;
        this.delivery = delivery;
        this.exposeDevCode = exposeDevCode;
        this.clock = clock;
    }

    @Transactional
    public OtpSendResponse send(UUID userId, OtpChannel channel, String newPhone) {
        AppUser user = findUser(userId);
        if (channel == OtpChannel.EMAIL ? user.isEmailVerified() : user.isPhoneVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This is already verified.");
        }
        if (channel == OtpChannel.PHONE && newPhone != null && !newPhone.isBlank()) {
            String normalized = PhoneNumbers.normalize(newPhone);
            if (!normalized.equals(user.getPhone())) {
                user.setPhone(normalized);
            }
        }
        if (channel == OtpChannel.PHONE && (user.getPhone() == null || user.getPhone().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter your phone number first.");
        }

        Instant now = clock.instant();
        codeRepository.findTopByUserIdAndChannelOrderByCreatedAtDesc(userId, channel)
                .map(OtpCode::getCreatedAt)
                .filter(sentAt -> sentAt.plus(RESEND_COOLDOWN).isAfter(now))
                .ifPresent(sentAt -> {
                    long wait = Duration.between(now, sentAt.plus(RESEND_COOLDOWN)).toSeconds() + 1;
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "Please wait " + wait + " seconds before asking for a new code.");
                });

        String code = String.format("%06d", random.nextInt(1_000_000));
        codeRepository.deleteAllByUserIdAndChannel(userId, channel);
        codeRepository.flush();
        codeRepository.save(new OtpCode(user, channel, passwordEncoder.encode(code), now.plus(CODE_LIFETIME)));

        boolean delivered = channel == OtpChannel.EMAIL
                ? delivery.sendEmail(user.getEmail(), code)
                : delivery.sendSms(user.getPhone(), code);
        String destination = channel == OtpChannel.EMAIL ? maskEmail(user.getEmail()) : PhoneNumbers.mask(user.getPhone());
        return new OtpSendResponse(channel, destination, CODE_LIFETIME.toSeconds(), RESEND_COOLDOWN.toSeconds(),
                !delivered && exposeDevCode ? code : null);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public UserResponse verify(UUID userId, OtpChannel channel, String code) {
        AppUser user = findUser(userId);
        OtpCode otp = codeRepository.findTopByUserIdAndChannelOrderByCreatedAtDesc(userId, channel)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ask for a code first."));
        if (otp.getExpiresAt().isBefore(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This code has expired. Ask for a new one.");
        }
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong tries. Ask for a new code.");
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.recordFailedAttempt();
            int left = MAX_ATTEMPTS - otp.getAttempts();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, left > 0
                    ? "That code is incorrect. " + left + " tr" + (left == 1 ? "y" : "ies") + " left."
                    : "Too many wrong tries. Ask for a new code.");
        }
        user.markVerified(channel);
        codeRepository.delete(otp);
        return UserResponse.from(user);
    }

    private AppUser findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in again."));
    }

    static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "•".repeat(Math.max(1, at - 2)) + email.charAt(at - 1) + email.substring(at);
    }
}
