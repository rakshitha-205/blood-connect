package com.bloodconnect.auth;

import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, OtpService otpService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.otpService = otpService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/password/forgot")
    public OtpSendResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.sendCode(request.email());
    }

    @PostMapping("/password/verify")
    public ResponseEntity<Void> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        passwordResetService.verifyCode(request.email(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse currentUser(@AuthenticationPrincipal AppUser user) {
        return authService.currentUser(user);
    }

    @PostMapping("/otp/send")
    public OtpSendResponse sendOtp(@AuthenticationPrincipal AppUser user, @Valid @RequestBody OtpSendRequest request) {
        return otpService.send(user.getId(), request.channel(), request.phone());
    }

    @PostMapping("/otp/verify")
    public UserResponse verifyOtp(@AuthenticationPrincipal AppUser user, @Valid @RequestBody OtpVerifyRequest request) {
        return otpService.verify(user.getId(), request.channel(), request.code());
    }
}
