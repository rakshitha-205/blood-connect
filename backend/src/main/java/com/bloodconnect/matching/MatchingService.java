package com.bloodconnect.matching;

import com.bloodconnect.donor.DonorProfile;
import com.bloodconnect.donor.DonorProfileRepository;
import com.bloodconnect.request.BloodRequest;
import com.bloodconnect.review.RatingSummary;
import com.bloodconnect.review.ReviewService;
import com.bloodconnect.user.AppUser;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private final DonorProfileRepository donorRepository;
    private final ReviewService reviewService;

    public MatchingService(DonorProfileRepository donorRepository, ReviewService reviewService) {
        this.donorRepository = donorRepository;
        this.reviewService = reviewService;
    }

    public List<DonorMatchResponse> match(BloodRequest request) {
        return findMatches(request.getBloodGroup(), request.getLatitude(), request.getLongitude());
    }

    public List<DonorProfile> availableDonorsFor(String recipientGroup) {
        LocalDate today = LocalDate.now();
        return donorRepository.findAllByAvailableTrue().stream()
                .filter(donor -> !donor.isDeferredOn(today))
                .filter(donor -> isCompatible(recipientGroup, donor.getBloodGroup()))
                .toList();
    }

    public List<DonorMatchResponse> findMatches(String recipientGroup, Double latitude, Double longitude) {
        List<DonorProfile> donors = availableDonorsFor(recipientGroup);
        Map<UUID, RatingSummary> ratings = reviewService.ratingsFor(donors.stream()
                .map(DonorProfile::getUser)
                .filter(Objects::nonNull)
                .map(AppUser::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return donors.stream()
                .map(donor -> toMatch(recipientGroup, donor, latitude, longitude,
                        donor.getUser() == null ? RatingSummary.NONE
                                : ratings.getOrDefault(donor.getUser().getId(), RatingSummary.NONE)))
                .sorted(Comparator
                        .comparing(DonorMatchResponse::distanceKm, Comparator.nullsLast(Double::compareTo))
                        .thenComparing(Comparator.comparingInt(DonorMatchResponse::priorityScore).reversed())
                        .thenComparing(DonorMatchResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public static boolean isCompatible(String recipientGroup, String donorGroup) {
        return switch (recipientGroup) {
            case "O-" -> donorGroup.equals("O-");
            case "O+" -> donorGroup.equals("O-") || donorGroup.equals("O+");
            case "A-" -> donorGroup.equals("O-") || donorGroup.equals("A-");
            case "A+" -> List.of("O-", "O+", "A-", "A+").contains(donorGroup);
            case "B-" -> donorGroup.equals("O-") || donorGroup.equals("B-");
            case "B+" -> List.of("O-", "O+", "B-", "B+").contains(donorGroup);
            case "AB-" -> List.of("O-", "A-", "B-", "AB-").contains(donorGroup);
            case "AB+" -> List.of("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+")
                    .contains(donorGroup);
            default -> false;
        };
    }

    private DonorMatchResponse toMatch(String recipientGroup, DonorProfile donor, Double latitude, Double longitude,
                                       RatingSummary rating) {
        Double distance = null;
        if (latitude != null && longitude != null && donor.getLatitude() != null && donor.getLongitude() != null) {
            distance = distanceKm(latitude, longitude, donor.getLatitude(), donor.getLongitude());
        }
        int score = (donor.isAvailable() ? 20 : 0)
            + (donor.getBloodGroup().equals(recipientGroup) ? 50 : 30)
                + (distance == null ? 0 : Math.max(0, 30 - (int) Math.round(distance)));
        return DonorMatchResponse.from(donor, distance, score, rating);
    }

    public static Double distanceKm(Double latitude1, Double longitude1, Double latitude2, Double longitude2) {
        if (latitude1 == null || longitude1 == null || latitude2 == null || longitude2 == null) {
            return null;
        }
        return distanceKm(latitude1.doubleValue(), longitude1.doubleValue(), latitude2.doubleValue(),
                longitude2.doubleValue());
    }

    private static double distanceKm(double latitude1, double longitude1, double latitude2, double longitude2) {
        double earthRadiusKm = 6371.0088;
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double value = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        value = Math.max(0, Math.min(1, value));
        return Math.round(earthRadiusKm * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value)) * 10) / 10.0;
    }
}