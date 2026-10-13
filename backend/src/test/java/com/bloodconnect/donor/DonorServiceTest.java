package com.bloodconnect.donor;

import com.bloodconnect.matching.DonorMatchResponse;
import com.bloodconnect.matching.MatchingService;
import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DonorServiceTest {

    @Test
    void saveCreatesProfileForNewDonor() {
        UUID userId = UUID.randomUUID();
        AppUser user = userWithId(userId);
        DonorProfileRequest request = profileRequest("New Donor", true);
        DonorProfileRepository repository = mock(DonorProfileRepository.class);
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(repository.save(any(DonorProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DonorProfileResponse response = new DonorService(repository, mock(MatchingService.class)).save(user, request);

        assertThat(response.name()).isEqualTo("New Donor");
        assertThat(response.bloodGroup()).isEqualTo("A+");
        assertThat(response.available()).isTrue();
        assertThat(response.updatedAt()).isNotNull();
        verify(repository).save(any(DonorProfile.class));
    }

    @Test
    void saveUpdatesExistingProfile() {
        UUID userId = UUID.randomUUID();
        AppUser user = userWithId(userId);
        DonorProfile existing = new DonorProfile(user, "Old Name", "555-0100", "O+", "Old location",
                null, null, false);
        DonorProfileRequest request = profileRequest("Updated Name", true);
        DonorProfileRepository repository = mock(DonorProfileRepository.class);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        DonorProfileResponse response = new DonorService(repository, mock(MatchingService.class)).save(user, request);

        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.available()).isTrue();
        assertThat(existing.getLatitude()).isEqualTo(12.5);
    }

    @Test
    void getReturnsNotFoundWhenProfileDoesNotExist() {
        UUID userId = UUID.randomUUID();
        DonorProfileRepository repository = mock(DonorProfileRepository.class);
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DonorService(repository, mock(MatchingService.class)).get(userId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void setAvailabilityUpdatesExistingProfile() {
        UUID userId = UUID.randomUUID();
        DonorProfile profile = new DonorProfile(userWithId(userId), "Donor", "555-0100", "O+", "Location",
                null, null, true);
        DonorProfileRepository repository = mock(DonorProfileRepository.class);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));

        DonorProfileResponse response = new DonorService(repository, mock(MatchingService.class))
                .setAvailability(userId, false);

        assertThat(response.available()).isFalse();
        assertThat(profile.getUpdatedAt()).isNotNull();
    }

    @Test
    void setAvailabilityReturnsNotFoundWhenProfileDoesNotExist() {
        UUID userId = UUID.randomUUID();
        DonorProfileRepository repository = mock(DonorProfileRepository.class);
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DonorService(repository, mock(MatchingService.class))
                .setAvailability(userId, true))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void nearbyDelegatesMatchingCriteria() {
        DonorProfileRepository repository = mock(DonorProfileRepository.class);
        MatchingService matchingService = mock(MatchingService.class);
        List<DonorMatchResponse> expected = List.of();
        when(matchingService.findMatches("O-", 1.0, 2.0)).thenReturn(expected);

        List<DonorMatchResponse> actual = new DonorService(repository, matchingService)
                .nearby("O-", 1.0, 2.0);

        assertThat(actual).isSameAs(expected);
        verify(matchingService).findMatches("O-", 1.0, 2.0);
    }

    private AppUser userWithId(UUID userId) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(userId);
        when(user.getRole()).thenReturn(UserRole.DONOR);
        return user;
    }

    private DonorProfileRequest profileRequest(String name, boolean available) {
        return new DonorProfileRequest(name, "555-0100", "A+", "Test location", 12.5, 77.6, available);
    }
}
