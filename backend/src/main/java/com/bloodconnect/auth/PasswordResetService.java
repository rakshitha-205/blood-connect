package com.bloodconnect.auth;

import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    static final String INVALID_CODE = "That code is incorrect or has expired. Ask for a new one.";

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpDelivery delivery;
    private final boolean exposeDevCode;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public PasswordResetService(UserRepository userRepository, PasswordResetCodeRepository codeRepository,
                                PasswordEncoder passwordEncoder, OtpDelivery delivery,
                                @Value("${bloodconnect.otp.expose-dev-code:false}") boolean exposeDevCode) {
        this(userRepository, codeRepository, passwordEncoder, delivery, exposeDevCode, Clock.systemUTC());
    }

    PasswordResetService(UserRepository userRepository, PasswordResetCodeRepository codeRepository,
                         PasswordEncoder passwordEncoder, OtpDelivery delivery, boolean exposeDevCode, Clock clock) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.passwordEncoder = passwordEncoder;
        this.delivery = delivery;
        this.exposeDevCode = exposeDevCode;
        this.clock = clock;
    }

    /** Answers the same way whether or not the email has an account, so it can't be used to find accounts. */
    @Transactional
    public OtpSendResponse sendCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        Optional<AppUser> account = userRepository.findByEmail(email);
        if (account.isEmpty()) {
            return response(email, null);
        }
        AppUser user = account.get();

        Instant now = clock.instant();
        codeRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .map(PasswordResetCode::getCreatedAt)
                .filter(sentAt -> sentAt.plus(OtpService.RESEND_COOLDOWN).isAfter(now))
                .ifPresent(sentAt -> {
                    long wait = Duration.between(now, sentAt.plus(OtpService.RESEND_COOLDOWN)).toSeconds() + 1;
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "Please wait " + wait + " seconds before asking for a new code.");
                });

        String code = String.format("%06d", random.nextInt(1_000_000));
        codeRepository.deleteAllByUserId(user.getId());
        codeRepository.flush();
        codeRepository.save(new PasswordResetCode(user, passwordEncoder.encode(code),
                now.plus(OtpService.CODE_LIFETIME)));

        boolean delivered = delivery.sendPasswordResetEmail(user.getEmail(), code);
        return response(email, !delivered && exposeDevCode ? code : null);
    }

    /** Checks the emailed code without using it up, so the user can then choose a new password. */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void verifyCode(String email, String code) {
        checkCode(email, code);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void resetPassword(ResetPasswordRequest request) {
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }
        AppUser user = checkCode(request.email(), request.code());

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        if (!user.isEmailVerified()) {
            user.markVerified(OtpChannel.EMAIL);
        }
        codeRepository.deleteAllByUserId(user.getId());
    }

    private AppUser checkCode(String email, String code) {
        AppUser user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE));
        PasswordResetCode resetCode = codeRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE));
        if (resetCode.getExpiresAt().isBefore(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This code has expired. Ask for a new one.");
        }
        if (resetCode.getAttempts() >= OtpService.MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong tries. Ask for a new code.");
        }
        if (!passwordEncoder.matches(code, resetCode.getCodeHash())) {
            resetCode.recordFailedAttempt();
            int left = OtpService.MAX_ATTEMPTS - resetCode.getAttempts();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, left > 0
                    ? "That code is incorrect. " + left + " tr" + (left == 1 ? "y" : "ies") + " left."
                    : "Too many wrong tries. Ask for a new code.");
        }
        return user;
    }

    private OtpSendResponse response(String email, String devCode) {
        return new OtpSendResponse(OtpChannel.EMAIL, OtpService.maskEmail(email),
                OtpService.CODE_LIFETIME.toSeconds(), OtpService.RESEND_COOLDOWN.toSeconds(), devCode);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
