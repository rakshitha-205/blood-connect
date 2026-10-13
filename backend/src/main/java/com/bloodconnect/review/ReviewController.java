package com.bloodconnect.review;

import com.bloodconnect.user.AppUser;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal AppUser user, @Valid @RequestBody ReviewCreateRequest request) {
        return reviewService.create(user, request);
    }

    @GetMapping("/me")
    public UserReviewsResponse mine(@AuthenticationPrincipal AppUser user) {
        return reviewService.reviewsFor(user.getId());
    }

    @GetMapping("/users/{userId}")
    public UserReviewsResponse forUser(@PathVariable UUID userId) {
        return reviewService.reviewsFor(userId);
    }
}
