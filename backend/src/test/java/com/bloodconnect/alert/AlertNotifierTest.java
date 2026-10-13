package com.bloodconnect.alert;

import com.bloodconnect.donor.DonorProfile;
import com.bloodconnect.request.BloodRequest;
import com.bloodconnect.request.RequestDecline;
import com.bloodconnect.user.AppUser;
import com.bloodconnect.user.UserRole;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AlertNotifierTest {

    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AlertNotifier notifier = new AlertNotifier(events, "http://localhost:5500/");
    private final AppUser hospital = new AppUser("city@example.com", "City Hospital", "hash", UserRole.HOSPITAL);
    private final BloodRequest request = new BloodRequest(hospital, "Ravi", "O-", 2, "City Hospital", "Gachibowli",
            17.44, 78.35, "CRITICAL", "9123456701", "Surgery at 6 pm");
    private final DonorProfile donor = new DonorProfile(new AppUser("dina@example.com", "Dina", "hash", UserRole.DONOR),
            "Dina", "9123456700", "O-", "Kondapur", 17.46, 78.36, true);

    @Test
    void newRequestEmailsEachDonorAndConfirmsToTheRequester() {
        notifier.requestCreated(request, List.of(donor));

        List<AlertEmail> emails = published();
        assertThat(emails).extracting(AlertEmail::to).containsExactly("dina@example.com", "city@example.com");
        assertThat(emails.get(0).subject()).isEqualTo("Critical: O- blood needed at City Hospital");
        assertThat(emails.get(0).text()).contains("Ravi", "2 units", "9123456701", "km from you",
                "Surgery at 6 pm", "http://localhost:5500/notifications");
        assertThat(emails.get(1).text()).contains("We emailed 1 available donor who can give to O-.",
                "http://localhost:5500/emergency-request?tab=track");
    }

    @Test
    void declineEmailsTheRequesterWithTheReason() {
        notifier.requestDeclined(new RequestDecline(request, donor, "Donated last month"));

        List<AlertEmail> emails = published();
        assertThat(emails).extracting(AlertEmail::to).containsExactly("city@example.com");
        assertThat(emails.get(0).subject()).isEqualTo("Dina can't donate for Ravi");
        assertThat(emails.get(0).text()).contains("\"Donated last month\"");
    }

    private List<AlertEmail> published() {
        ArgumentCaptor<AlertEmail.Batch> batch = ArgumentCaptor.forClass(AlertEmail.Batch.class);
        verify(events).publishEvent(batch.capture());
        return batch.getValue().emails();
    }
}
