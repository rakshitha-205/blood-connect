package com.bloodconnect.matching;

import com.bloodconnect.donor.DonorProfile;
import com.bloodconnect.donor.DonorProfileRepository;
import com.bloodconnect.review.ReviewService;
import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserRole;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchingServiceTest {

    @ParameterizedTest(name = "recipient {0} accepts donor {1}: {2}")
    @CsvSource({
            "O-, O-, true",
            "O-, O+, false",
            "O+, O-, true",
            "O+, O+, true",
            "O+, A+, false",
            "A-, O-, true",
            "A-, A-, true",
            "A-, B-, false",
            "A+, O-, true",
            "A+, O+, true",
            "A+, A-, true",
            "A+, A+, true",
            "A+, B+, false",
            "B-, O-, true",
            "B-, B-, true",
            "B-, A-, false",
            "B+, O+, true",
            "B+, B-, true",
            "B+, B+, true",
            "B+, A+, false",
            "AB-, O-, true",
            "AB-, A-, true",
            "AB-, B-, true",
            "AB-, AB-, true",
            "AB-, AB+, false",
            "AB+, O-, true",
            "AB+, O+, true",
            "AB+, A-, true",
            "AB+, A+, true",
            "AB+, B-, true",
            "AB+, B+, true",
            "AB+, AB-, true",
            "AB+, AB+, true",
            "AB+, X+, false"
    })
    void checksRedCellCompatibility(String recipientGroup, String donorGroup, boolean expected) {
        assertThat(MatchingService.isCompatible(recipientGroup, donorGroup)).isEqualTo(expected);
    }

    @Test
    void filtersIncompatibleDonorsAndOrdersMatchesByDistanceScoreAndName() {
        DonorProfile zulu = donor("zulu", "A+", 0.0, 0.1);
        DonorProfile alpha = donor("Alpha", "A+", 0.0, 0.1);
        DonorProfile universal = donor("O minus", "O-", 0.0, 0.1);
        DonorProfile far = donor("Far", "A+", 0.0, 1.0);
        DonorProfile withoutCoordinates = donor("No coordinates", "A+", null, null);
        DonorProfile incompatible = donor("B positive", "B+", 0.0, 0.0);
        DonorProfileRepository repository = mock(DonorProfileRepository.class);
        when(repository.findAllByAvailableTrue()).thenReturn(List.of(
                zulu, universal, incompatible, withoutCoordinates, far, alpha));

        List<DonorMatchResponse> matches = new MatchingService(repository, mock(ReviewService.class))
                .findMatches("A+", 0.0, 0.0);

        assertThat(matches).extracting(DonorMatchResponse::name)
                .containsExactly("Alpha", "zulu", "O minus", "Far", "No coordinates");
        assertThat(matches.get(0).distanceKm()).isCloseTo(11.1, within(0.1));
        assertThat(matches.get(3).distanceKm()).isCloseTo(111.2, within(0.1));
        assertThat(matches.get(4).distanceKm()).isNull();
        assertThat(matches.get(0).priorityScore()).isGreaterThan(matches.get(2).priorityScore());
    }

    private DonorProfile donor(String name, String bloodGroup, Double latitude, Double longitude) {
        AppUser user = new AppUser(name.toLowerCase() + "@example.com", "Test User", "encoded", UserRole.DONOR);
        return new DonorProfile(user, name, "555-0100", bloodGroup, "Test location",
                latitude, longitude, true);
    }
}
