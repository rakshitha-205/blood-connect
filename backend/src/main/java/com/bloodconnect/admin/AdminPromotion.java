package com.bloodconnect.admin;

import com.bloodconnect.user.UserRepository;
import com.bloodconnect.user.UserRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Promotes an already registered account to admin when started with
 * {@code --bloodconnect.admin.promote=email}. Never creates accounts.
 */
@Component
public class AdminPromotion implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminPromotion.class);

    private final UserRepository userRepository;
    private final String email;

    public AdminPromotion(UserRepository userRepository, @Value("${bloodconnect.admin.promote:}") String email) {
        this.userRepository = userRepository;
        this.email = email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isEmpty()) {
            return;
        }
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            user.setRole(UserRole.ADMIN);
            userRepository.save(user);
            log.info("{} is now an admin. Log in again to open the admin dashboard.", email);
        }, () -> log.warn("Cannot promote {} to admin: no account with that email. Register it first.", email));
    }
}
