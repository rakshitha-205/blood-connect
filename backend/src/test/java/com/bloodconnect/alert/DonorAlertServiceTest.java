package com.bloodconnect.alert;

import com.bloodconnect.donor.DonorProfile;
import com.bloodconnect.donor.DonorProfileRepository;
import com.bloodconnect.request.BloodRequest;
import com.bloodconnect.request.BloodRequestRepository;
import com.bloodconnect.request.RequestAcceptance;
import com.bloodconnect.request.RequestAcceptanceRepository;
import com.bloodconnect.request.RequestDecline;
import com.bloodconnect.request.RequestDeclineRepository;
import com.bloodconnect.request.RequestStatus;
import com.bloodconnect.review.ReviewService;
import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserRole;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DonorAlertServiceTest {

    private final DonorProfileRepository donorRepository = mock(DonorProfileRepository.class);
    private final BloodRequestRepository requestRepository = mock(BloodRequestRepository.class);
    private final RequestDeclineRepository declineRepository = mock(RequestDeclineRepository.class);
    private final RequestAcceptanceRepository acceptanceRepository = mock(RequestAcceptanceRepository.class);
    private final AlertNotifier alertNotifier = mock(AlertNotifier.class);
    private final DonorAlertService service = new DonorAlertService(donorRepository, requestRepository,
            declineRepository, acceptanceRepository, mock(ReviewService.class), alertNotifier);
    private final AppUser requester = new AppUser("patient@example.com", "Patient", "hash", UserRole.REQUESTER);

    @Test
    void listsCompatibleOpenRequestsMostUrgentThenNearestFirst() {
        UUID donorUserId = UUID.randomUUID();
        donor(donorUserId, "O-", 17.4483, 78.3915);

        BloodRequest farNormal = request("Far normal", "A+", 17.9689, 79.5941, "NORMAL");
        BloodRequest farCritical = request("Far critical", "O-", 17.9689, 79.5941, "CRITICAL");
        BloodRequest nearCritical = request("Near critical", "B+", 17.4401, 78.3489, "CRITICAL");
        when(requestRepository.findAllByStatus(RequestStatus.OPEN))
                .thenReturn(List.of(farNormal, farCritical, nearCritical));

        List<DonorAlertResponse> alerts = service.alertsFor(donorUserId);

        assertThat(alerts).extracting(DonorAlertResponse::patientName)
                .containsExactly("Near critical", "Far critical", "Far normal");
        assertThat(alerts.get(1).exactMatch()).isTrue();
        assertThat(alerts.get(0).distanceKm()).isLessThan(10.0);
    }

    @Test
    void skipsRequestsTheDonorCannotGiveTo() {
        UUID donorUserId = UUID.randomUUID();
        donor(donorUserId, "AB+", null, null);
        when(requestRepository.findAllByStatus(RequestStatus.OPEN))
                .thenReturn(List.of(request("Needs O-", "O-", 17.4, 78.4, "CRITICAL")));

        assertThat(service.alertsFor(donorUserId)).isEmpty();
    }

    @Test
    void hidesRequestsTheDonorHasDeclined() {
        UUID donorUserId = UUID.randomUUID();
        DonorProfile donor = donor(donorUserId, "O-", null, null);
        BloodRequest declined = request("Declined", "O-", 17.4, 78.4, "CRITICAL");
        BloodRequest open = request("Still open", "O-", 17.4, 78.4, "URGENT");
        when(requestRepository.findAllByStatus(RequestStatus.OPEN)).thenReturn(List.of(declined, open));
        when(declineRepository.findAllByDonorId(donor.getId()))
                .thenReturn(List.of(new RequestDecline(declined, donor, "Unwell")));

        assertThat(service.alertsFor(donorUserId)).extracting(DonorAlertResponse::patientName)
                .containsExactly("Still open");
    }

    @Test
    void savesTheTrimmedReasonWhenDeclining() {
        UUID donorUserId = UUID.randomUUID();
        DonorProfile donor = donor(donorUserId, "O-", null, null);
        BloodRequest request = request("Patient", "O-", 17.4, 78.4, "CRITICAL");
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        service.decline(donorUserId, request.getId(), "  Donated last month  ");

        ArgumentCaptor<RequestDecline> saved = ArgumentCaptor.forClass(RequestDecline.class);
        verify(declineRepository).save(saved.capture());
        assertThat(saved.getValue().getReason()).isEqualTo("Donated last month");
        assertThat(saved.getValue().getDonor()).isSameAs(donor);
        verify(alertNotifier).requestDeclined(saved.getValue());
    }

    @Test
    void rejectsDecliningTwiceOrAClosedRequest() {
        UUID donorUserId = UUID.randomUUID();
        DonorProfile donor = donor(donorUserId, "O-", null, null);
        BloodRequest request = request("Patient", "O-", 17.4, 78.4, "CRITICAL");
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(declineRepository.existsByRequestIdAndDonorId(request.getId(), donor.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.decline(donorUserId, request.getId(), "Busy"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already declined");

        request.setStatus(RequestStatus.FULFILLED);
        assertThatThrownBy(() -> service.decline(donorUserId, request.getId(), "Busy"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no longer open");
        verify(declineRepository, never()).save(any());
        verify(alertNotifier, never()).requestDeclined(any());
    }

    @Test
    void hidesRequestsTheDonorHasAccepted() {
        UUID donorUserId = UUID.randomUUID();
        DonorProfile donor = donor(donorUserId, "O-", null, null);
        BloodRequest accepted = request("Accepted", "O-", 17.4, 78.4, "CRITICAL");
        BloodRequest open = request("Still open", "O-", 17.4, 78.4, "URGENT");
        when(requestRepository.findAllByStatus(RequestStatus.OPEN)).thenReturn(List.of(accepted, open));
        when(acceptanceRepository.findAllByDonorIdOrderByCreatedAtDesc(donor.getId()))
                .thenReturn(List.of(new RequestAcceptance(accepted, donor)));

        assertThat(service.alertsFor(donorUserId)).extracting(DonorAlertResponse::patientName)
                .containsExactly("Still open");
    }

    @Test
    void acceptingSavesAndEmailsTheRequester() {
        UUID donorUserId = UUID.randomUUID();
        DonorProfile donor = donor(donorUserId, "O-", null, null);
        BloodRequest request = request("Patient", "A+", 17.4, 78.4, "CRITICAL");
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(acceptanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.accept(donorUserId, request.getId());

        ArgumentCaptor<RequestAcceptance> saved = ArgumentCaptor.forClass(RequestAcceptance.class);
        verify(acceptanceRepository).save(saved.capture());
        assertThat(saved.getValue().getDonor()).isSameAs(donor);
        assertThat(saved.getValue().getRequest()).isSameAs(request);
        verify(alertNotifier).requestAccepted(saved.getValue());
    }

    @Test
    void rejectsAcceptingTwiceAfterDecliningOrWithAnIncompatibleGroup() {
        UUID donorUserId = UUID.randomUUID();
        DonorProfile donor = donor(donorUserId, "AB+", null, null);
        BloodRequest needsO = request("Needs O-", "O-", 17.4, 78.4, "CRITICAL");
        when(requestRepository.findById(needsO.getId())).thenReturn(Optional.of(needsO));
        assertThatThrownBy(() -> service.accept(donorUserId, needsO.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("can't be given");

        BloodRequest request = request("Patient", "AB+", 17.4, 78.4, "CRITICAL");
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(acceptanceRepository.existsByRequestIdAndDonorId(request.getId(), donor.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.accept(donorUserId, request.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already accepted");
        assertThatThrownBy(() -> service.decline(donorUserId, request.getId(), "Busy"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already accepted");

        when(declineRepository.existsByRequestIdAndDonorId(request.getId(), donor.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.accept(donorUserId, request.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already declined");
        verify(acceptanceRepository, never()).save(any());
        verify(alertNotifier, never()).requestAccepted(any());
    }

    private DonorProfile donor(UUID donorUserId, String group, Double latitude, Double longitude) {
        AppUser donorUser = new AppUser("donor@example.com", "Donor", "hash", UserRole.DONOR);
        DonorProfile donor = new DonorProfile(donorUser, "Donor", "9000000000", group, "Madhapur",
                latitude, longitude, true);
        ReflectionTestUtils.setField(donor, "id", UUID.randomUUID());
        when(donorRepository.findByUserId(donorUserId)).thenReturn(Optional.of(donor));
        return donor;
    }

    private BloodRequest request(String patient, String group, Double latitude, Double longitude, String urgency) {
        BloodRequest request = new BloodRequest(requester, patient, group, 1, "Hospital", "Somewhere",
                latitude, longitude, urgency, "9111111111", null);
        ReflectionTestUtils.setField(request, "id", UUID.randomUUID());
        return request;
    }
}
