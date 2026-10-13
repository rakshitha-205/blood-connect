package com.bloodconnect.config;

import com.bloodconnect.user.AppUser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class VerificationRequiredFilter extends OncePerRequestFilter {

    private final boolean phoneRequired;

    public VerificationRequiredFilter(boolean phoneRequired) {
        this.phoneRequired = phoneRequired;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUser user && !isVerified(user)
                && !request.getRequestURI().startsWith("/api/auth/")) {
            String detail = phoneRequired
                    ? "Verify your email and phone number to continue."
                    : "Verify your email to continue.";
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"status\":403,\"title\":\"Forbidden\",\"code\":\"VERIFICATION_REQUIRED\","
                    + "\"detail\":\"" + detail + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isVerified(AppUser user) {
        return user.isEmailVerified() && (!phoneRequired || user.isPhoneVerified());
    }
}
