package com.bloodconnect.eligibility;

import com.bloodconnect.user.AppUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eligibility")
public class EligibilityController {

    private final EligibilityService eligibilityService;
    private final EligibilityRateLimiter rateLimiter;

    public EligibilityController(EligibilityService eligibilityService, EligibilityRateLimiter rateLimiter) {
        this.eligibilityService = eligibilityService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/check")
    public EligibilityResponse check(@AuthenticationPrincipal AppUser user, @Valid @RequestBody EligibilityRequest request,
                                     HttpServletRequest httpRequest) {
        rateLimiter.acquire(httpRequest.getRemoteAddr());
        return eligibilityService.check(request, user);
    }
}
