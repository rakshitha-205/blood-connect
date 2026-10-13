package com.bloodconnect.alert;

import com.bloodconnect.donor.DonorProfile;
import com.bloodconnect.matching.MatchingService;
import com.bloodconnect.request.BloodRequest;
import com.bloodconnect.request.RequestAcceptance;
import com.bloodconnect.request.RequestDecline;
import com.bloodconnect.user.AppUser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertNotifier {

    private final ApplicationEventPublisher events;
    private final String appUrl;

    public AlertNotifier(ApplicationEventPublisher events, @Value("${bloodconnect.app-url}") String appUrl) {
        this.events = events;
        this.appUrl = appUrl.replaceAll("/+$", "");
    }

    public void requestCreated(BloodRequest request, List<DonorProfile> donors) {
        List<AlertEmail> emails = new ArrayList<>();
        donors.forEach(donor -> emails.add(new AlertEmail(donor.getUser().getEmail(),
                "%s: %s blood needed at %s".formatted(urgency(request), request.getBloodGroup(), request.getHospital()),
                donorText(request, donor))));
        AppUser requester = request.getRequester();
        emails.add(new AlertEmail(requester.getEmail(),
                "Your blood request for %s is live".formatted(request.getPatientName()),
                requesterText(request, requester, donors.size())));
        events.publishEvent(new AlertEmail.Batch(emails));
    }

    public void requestDeclined(RequestDecline decline) {
        BloodRequest request = decline.getRequest();
        DonorProfile donor = decline.getDonor();
        AppUser requester = request.getRequester();
        String text = """
                Hello %s,

                %s (%s donor) can't donate for %s's request.

                Their message: "%s"

                Other matching donors can still respond. See all updates:
                %s/notifications

                - BloodConnect
                """.formatted(displayName(requester), donor.getName(), donor.getBloodGroup(),
                request.getPatientName(), decline.getReason(), appUrl);
        events.publishEvent(new AlertEmail.Batch(List.of(new AlertEmail(requester.getEmail(),
                "%s can't donate for %s".formatted(donor.getName(), request.getPatientName()), text))));
    }

    public void requestAccepted(RequestAcceptance acceptance) {
        BloodRequest request = acceptance.getRequest();
        DonorProfile donor = acceptance.getDonor();
        AppUser requester = request.getRequester();
        String text = """
                Hello %s,

                Good news! %s (%s donor) has accepted %s's request and can donate.

                Phone: %s
                WhatsApp: %s

                Call or message them now to arrange the donation at %s. See every donor who accepted:
                %s/emergency-request?tab=track

                - BloodConnect
                """.formatted(displayName(requester), donor.getName(), donor.getBloodGroup(),
                request.getPatientName(), donor.getPhone(), whatsappLink(donor.getPhone()), request.getHospital(), appUrl);
        events.publishEvent(new AlertEmail.Batch(List.of(new AlertEmail(requester.getEmail(),
                "%s can donate for %s".formatted(donor.getName(), request.getPatientName()), text))));
    }

    static String whatsappLink(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        return "https://wa.me/" + (digits.length() == 10 ? "91" + digits : digits);
    }

    private String donorText(BloodRequest request, DonorProfile donor) {
        Double distance = MatchingService.distanceKm(donor.getLatitude(), donor.getLongitude(),
                request.getLatitude(), request.getLongitude());
        String message = request.getMessage() == null || request.getMessage().isBlank()
                ? ""
                : "\nMessage: \"%s\"\n".formatted(request.getMessage().trim());
        return """
                Hello %s,

                A patient near you needs blood and your %s group can help.

                Patient: %s
                Blood group: %s (%d unit%s)
                Urgency: %s
                Hospital: %s, %s%s
                Contact: %s
                %s
                Open your alerts to call, get directions or reply:
                %s/notifications

                - BloodConnect
                """.formatted(donor.getName(), donor.getBloodGroup(), request.getPatientName(),
                request.getBloodGroup(), request.getUnits(), request.getUnits() == 1 ? "" : "s",
                urgency(request), request.getHospital(), request.getLocation(),
                distance == null ? "" : " (about %s km from you)".formatted(distance),
                request.getContact(), message, appUrl);
    }

    private String requesterText(BloodRequest request, AppUser requester, int donorCount) {
        String alerted = donorCount == 0
                ? "No available donors match %s right now. We'll keep the request open so new donors can see it."
                        .formatted(request.getBloodGroup())
                : "We emailed %d available donor%s who can give to %s."
                        .formatted(donorCount, donorCount == 1 ? "" : "s", request.getBloodGroup());
        return """
                Hello %s,

                Your request for %s (%s, %d unit%s at %s) is now open.

                %s

                You'll get an email when a donor replies. Track the request here:
                %s/request-tracking

                - BloodConnect
                """.formatted(displayName(requester), request.getPatientName(), request.getBloodGroup(),
                request.getUnits(), request.getUnits() == 1 ? "" : "s", request.getHospital(), alerted, appUrl);
    }

    private static String urgency(BloodRequest request) {
        String value = request.getUrgency().toLowerCase();
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String displayName(AppUser user) {
        return user.getName() == null || user.getName().isBlank() ? "there" : user.getName();
    }
}
