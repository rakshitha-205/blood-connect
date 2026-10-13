package com.bloodconnect.auth;

import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserRepository;
import com.bloodconnect.user.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void registerNormalizesEmailAndReturnsTokenWithoutPasswordHash() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        when(users.existsByEmail("donor@example.com")).thenReturn(false);
        when(encoder.encode("long-enough-password")).thenReturn("encoded-password");
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.createToken(any(AppUser.class))).thenReturn("signed-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = new AuthService(users, encoder, authenticationManager, jwtService)
                .register(new RegisterRequest("Test User", "  DONOR@Example.COM ", "9876543210", "long-enough-password", UserRole.DONOR));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
        assertThat(response.user().email()).isEqualTo("donor@example.com");
        assertThat(response.user().role()).isEqualTo(UserRole.DONOR);
        verify(users).existsByEmail("donor@example.com");
        verify(encoder).encode("long-enough-password");
    }

    @Test
    void registerRejectsDuplicateNormalizedEmail() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.existsByEmail("donor@example.com")).thenReturn(true);

        assertThatThrownBy(() -> new AuthService(users, encoder, mock(AuthenticationManager.class),
                mock(JwtService.class)).register(
                new RegisterRequest("Test User", " DONOR@EXAMPLE.COM ", "9876543210", "long-enough-password", UserRole.DONOR)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(encoder, never()).encode(any());
    }

    @Test
    void registerRejectsPasswordLongerThan72Utf8Bytes() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.existsByEmail("donor@example.com")).thenReturn(false);
        String password = "\u00e9".repeat(37);

        assertThat(password).hasSize(37);
        assertThat(password.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(74);
        assertThatThrownBy(() -> new AuthService(users, encoder, mock(AuthenticationManager.class),
                mock(JwtService.class)).register(
                new RegisterRequest("Test User", "donor@example.com", "9876543210", password, UserRole.DONOR)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(encoder, never()).encode(any());
    }

    @Test
    void registerAcceptsPasswordAt72Utf8Bytes() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        when(users.existsByEmail("donor@example.com")).thenReturn(false);
        when(encoder.encode(any())).thenReturn("encoded-password");
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.createToken(any(AppUser.class))).thenReturn("signed-token");
        String password = "\u00e9".repeat(36);

        AuthResponse response = new AuthService(users, encoder, mock(AuthenticationManager.class), jwtService)
                .register(new RegisterRequest("Test User", "donor@example.com", "9876543210", password, UserRole.DONOR));

        assertThat(password.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(72);
        assertThat(response.accessToken()).isEqualTo("signed-token");
        verify(encoder).encode(password);
    }

    @Test
    void loginNormalizesEmailBeforeAuthentication() {
        UserRepository users = mock(UserRepository.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        AppUser user = new AppUser("donor@example.com", "Test User", "encoded-password", UserRole.DONOR);
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtService.createToken(user)).thenReturn("signed-token");

        AuthResponse response = new AuthService(users, mock(PasswordEncoder.class), authenticationManager, jwtService)
                .login(new LoginRequest(" DONOR@Example.COM ", "plain-password"));

        assertThat(response.accessToken()).isEqualTo("signed-token");
        verify(authenticationManager).authenticate(org.mockito.ArgumentMatchers.argThat(token ->
                "donor@example.com".equals(token.getPrincipal())
                        && "plain-password".equals(token.getCredentials())));
    }

    @Test
    void currentUserReturnsOnlyPublicUserFields() {
        AppUser user = new AppUser("donor@example.com", "Test User", "secret-hash", UserRole.DONOR);

        var response = new AuthService(mock(UserRepository.class), mock(PasswordEncoder.class),
                mock(AuthenticationManager.class), mock(JwtService.class)).currentUser(user);

        assertThat(response.email()).isEqualTo("donor@example.com");
        assertThat(response.role()).isEqualTo(UserRole.DONOR);
        assertThat(response).hasNoNullFieldsOrPropertiesExcept("id", "createdAt", "phone");
        assertThat(response.emailVerified()).isFalse();
    }
}
