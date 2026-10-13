package com.bloodconnect.auth;

import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Test
    void createsSignedTokenWithSubjectRoleAndConfiguredExpiration() {
        JwtService service = new JwtService(SECRET, Duration.ofSeconds(90));
        AppUser user = new AppUser("donor@example.com", "Test User", "encoded-password", UserRole.DONOR);

        String token = service.createToken(user);
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(service.getSubject(token)).isEqualTo("donor@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("DONOR");
        assertThat(service.getExpirationSeconds()).isEqualTo(90);
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(90_000L);
    }

    @Test
    void rejectsTokenSignedByDifferentKey() {
        String token = new JwtService(SECRET, Duration.ofMinutes(1))
                .createToken(new AppUser("donor@example.com", "Test User", "encoded-password", UserRole.DONOR));
        String otherSecret = Base64.getEncoder().encodeToString(
                "abcdefghijklmnopqrstuvwxyzABCDEF".getBytes(StandardCharsets.UTF_8));
        JwtService verifier = new JwtService(otherSecret, Duration.ofMinutes(1));

        assertThatThrownBy(() -> verifier.getSubject(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsSigningKeyShorterThan256Bits() {
        String weakSecret = Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new JwtService(weakSecret, Duration.ofMinutes(1)))
                .isInstanceOf(WeakKeyException.class);
    }
}
