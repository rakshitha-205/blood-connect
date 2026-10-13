package com.bloodconnect.review;

import com.bloodconnect.request.BloodRequest;
import com.bloodconnect.request.BloodRequestRepository;
import com.bloodconnect.request.RequestAcceptanceRepository;
import com.bloodconnect.request.RequestStatus;
import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BloodRequestRepository requestRepository;
    private final RequestAcceptanceRepository acceptanceRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, BloodRequestRepository requestRepository,
                         RequestAcceptanceRepository acceptanceRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.requestRepository = requestRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse create(AppUser reviewer, ReviewCreateRequest body) {
        BloodRequest request = requestRepository.findById(body.requestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found"));
        UUID requesterId = request.getRequester().getId();
        UUID revieweeId;
        if (requesterId.equals(reviewer.getId())) {
            if (body.revieweeId() == null
                    || !acceptanceRepository.existsByRequestIdAndDonorUserId(request.getId(), body.revieweeId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You can only rate donors who accepted this request.");
            }
            revieweeId = body.revieweeId();
        } else if (acceptanceRepository.existsByRequestIdAndDonorUserId(request.getId(), reviewer.getId())) {
            revieweeId = requesterId;
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the patient and donors who accepted this request can leave a review.");
        }
        if (request.getStatus() != RequestStatus.FULFILLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reviews open once the request is marked fulfilled.");
        }
        if (reviewRepository.existsByRequestIdAndReviewerIdAndRevieweeId(request.getId(), reviewer.getId(), revieweeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this person for this request.");
        }
        AppUser reviewee = userRepository.findById(revieweeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String comment = body.comment() == null || body.comment().isBlank() ? null : body.comment().trim();
        return ReviewResponse.from(reviewRepository.save(new Review(request, reviewer, reviewee, body.rating(), comment)));
    }

    @Transactional(readOnly = true)
    public UserReviewsResponse reviewsFor(UUID userId) {
        RatingSummary summary = ratingFor(userId);
        return new UserReviewsResponse(summary.average(), summary.count(),
                reviewRepository.findTop20ByRevieweeIdOrderByCreatedAtDesc(userId).stream()
                        .map(ReviewResponse::from)
                        .toList());
    }

    @Transactional(readOnly = true)
    public RatingSummary ratingFor(UUID userId) {
        return ratingsFor(Set.of(userId)).getOrDefault(userId, RatingSummary.NONE);
    }

    @Transactional(readOnly = true)
    public Map<UUID, RatingSummary> ratingsFor(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return reviewRepository.ratingsFor(userIds).stream()
                .collect(Collectors.toMap(ReviewRepository.RatingRow::getUserId,
                        row -> RatingSummary.of(row.getAverage(), row.getTotal())));
    }

    @Transactional(readOnly = true)
    public Set<UUID> revieweesOf(UUID requestId, UUID reviewerId) {
        return reviewRepository.findAllByRequestIdAndReviewerId(requestId, reviewerId).stream()
                .map(review -> review.getReviewee().getId())
                .collect(Collectors.toSet());
    }
}
