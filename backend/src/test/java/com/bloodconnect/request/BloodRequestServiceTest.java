package com.bloodconnect.request;

import com.bloodconnect.alert.AlertNotifier;
import com.bloodconnect.donor.DonorProfile;
import com.bloodconnect.matching.DonorMatchResponse;
import com.bloodconnect.matching.MatchingService;
import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BloodRequestServiceTest {

    @Test
    void createNormalizesUrgencyAndStartsRequestOpen() {
        BloodRequestRepository repository = mock(BloodRequestRepository.class);
        when(repository.save(any(BloodRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BloodRequestCreateRequest request = new BloodRequestCreateRequest("Patient", "A+", 2,
                "City Hospital", "City", 12.0, 77.0, "urgent", "555-0100", "Need donors");

        BloodRequestResponse response = new BloodRequestService(repository, mock(MatchingService.class), mock(RequestDeclineRepository.class), mock(AlertNotifier.class))
                .create(mock(AppUser.class), request);

        assertThat(response.urgency()).isEqualTo("URGENT");
        assertThat(response.status()).isEqualTo(RequestStatus.OPEN);
        verify(repository).save(any(BloodRequest.class));
    }

    @Test
    void createEmailsCompatibleDonorsButNotTheRequesterThemselves() {
        BloodRequestRepository repository = mock(BloodRequestRepository.class);
        when(repository.save(any(BloodRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppUser requester = new AppUser("hospital@example.com", "City Hospital", "hash", UserRole.HOSPITAL);
        ReflectionTestUtils.setField(requester, "id", UUID.randomUUID());
        AppUser otherUser = new AppUser("dina@example.com", "Dina", "hash", UserRole.DONOR);
        ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());
        DonorProfile self = new DonorProfile(requester, "Self", "9000000001", "O-", "City", null, null, true);
        DonorProfile other = new DonorProfile(otherUser, "Dina", "9000000002", "O-", "City", null, null, true);
        MatchingService matchingService = mock(MatchingService.class);
        when(matchingService.availableDonorsFor("A+")).thenReturn(List.of(self, other));
        AlertNotifier notifier = mock(AlertNotifier.class);

        new BloodRequestService(repository, matchingService, mock(RequestDeclineRepository.class), notifier)
                .create(requester, new BloodRequestCreateRequest("Patient", "A+", 2,
                        "City Hospital", "City", 12.0, 77.0, "urgent", "555-0100", null));

        verify(notifier).requestCreated(any(BloodRequest.class), eq(List.of(other)));
    }

    @Test
    void matchesDelegatesOnlyForOpenOwnedRequestAndSkipsDonorsWhoDeclined() {
        UUID requestId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        BloodRequest request = request();
        DonorProfile declinedDonor = new DonorProfile(
                new AppUser("busy@example.com", "Busy", "hash", UserRole.DONOR),
                "Busy", "9000000000", "O-", "Madhapur", null, null, true);
        ReflectionTestUtils.setField(declinedDonor, "id", UUID.randomUUID());
        DonorMatchResponse willing = new DonorMatchResponse(UUID.randomUUID(), "Willing", "O-", "Kondapur",
                null, null, 2.0, 90, true, null, 0);
        DonorMatchResponse declined = new DonorMatchResponse(declinedDonor.getId(), "Busy", "O-", "Madhapur",
                null, null, 1.0, 95, true, null, 0);
        BloodRequestRepository repository = mock(BloodRequestRepository.class);
        MatchingService matchingService = mock(MatchingService.class);
        RequestDeclineRepository declineRepository = mock(RequestDeclineRepository.class);
        when(repository.findByIdAndRequesterId(requestId, requesterId)).thenReturn(Optional.of(request));
        when(matchingService.match(request)).thenReturn(List.of(declined, willing));
        when(declineRepository.findAllByRequestIdOrderByCreatedAtDesc(request.getId()))
                .thenReturn(List.of(new RequestDecline(request, declinedDonor, "Unwell")));

        List<DonorMatchResponse> actual = new BloodRequestService(repository, matchingService, declineRepository, mock(AlertNotifier.class))
                .matches(requestId, requesterId);

        assertThat(actual).containsExactly(willing);
        verify(matchingService).match(request);
    }

    @Test
    void matchesReturnsEmptyForClosedRequest() {
        UUID requestId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        BloodRequest request = request();
        request.setStatus(RequestStatus.FULFILLED);
        BloodRequestRepository repository = mock(BloodRequestRepository.class);
        MatchingService matchingService = mock(MatchingService.class);
        when(repository.findByIdAndRequesterId(requestId, requesterId)).thenReturn(Optional.of(request));

        List<DonorMatchResponse> matches = new BloodRequestService(repository, matchingService, mock(RequestDeclineRepository.class), mock(AlertNotifier.class))
                .matches(requestId, requesterId);

        assertThat(matches).isEmpty();
        verifyNoInteractions(matchingService);
    }

    @Test
    void getRejectsRequestNotOwnedByRequester() {
        UUID requestId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        BloodRequestRepository repository = mock(BloodRequestRepository.class);
        when(repository.findByIdAndRequesterId(requestId, requesterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new BloodRequestService(repository, mock(MatchingService.class), mock(RequestDeclineRepository.class), mock(AlertNotifier.class))
                .get(requestId, requesterId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateStatusChangesOwnedRequestStatus() {
        UUID requestId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        BloodRequest request = request();
        BloodRequestRepository repository = mock(BloodRequestRepository.class);
        when(repository.findByIdAndRequesterId(requestId, requesterId)).thenReturn(Optional.of(request));

        BloodRequestResponse response = new BloodRequestService(repository, mock(MatchingService.class), mock(RequestDeclineRepository.class), mock(AlertNotifier.class))
                .updateStatus(requestId, requesterId, RequestStatus.CANCELLED);

        assertThat(response.status()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        verify(repository).findByIdAndRequesterId(requestId, requesterId);
    }

    @Test
    void updateStatusRejectsRequestNotOwnedByRequester() {
        UUID requestId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        BloodRequestRepository repository = mock(BloodRequestRepository.class);
        when(repository.findByIdAndRequesterId(requestId, requesterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new BloodRequestService(repository, mock(MatchingService.class), mock(RequestDeclineRepository.class), mock(AlertNotifier.class))
                .updateStatus(requestId, requesterId, RequestStatus.FULFILLED))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private BloodRequest request() {
        return new BloodRequest(null, "Patient", "A+", 2, "City Hospital", "City",
                12.0, 77.0, "URGENT", "555-0100", "Need donors");
    }
}
