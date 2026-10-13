package com.bloodconnect.eligibility;

import com.bloodconnect.ai.OpenRouterClient;
import com.bloodconnect.donor.DonorService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class EligibilityServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @Test
    void readsOnlyAllowedCodesAndValidDatesFromTheModelReply() throws Exception {
        String reply = """
                Sure! {"conditions":[
                  {"code":"TATTOO_OR_PIERCING","date":"2026-06-01"},
                  {"code":"ANTIBIOTICS","date":null},
                  {"code":"DENTAL_WORK","date":"2027-01-01"},
                  {"code":"AGE_OUT_OF_RANGE","date":null},
                  {"code":"MADE_UP","date":"2026-01-01"},
                  {"code":"FEVER_OR_ILLNESS","date":"last week"}]}
                """;

        List<NoteCondition> conditions = service("", "http://127.0.0.1:9").parseConditions(reply, TODAY);

        assertThat(conditions).containsExactly(
                new NoteCondition(EligibilityCondition.TATTOO_OR_PIERCING, LocalDate.of(2026, 6, 1)),
                new NoteCondition(EligibilityCondition.ANTIBIOTICS, null),
                new NoteCondition(EligibilityCondition.DENTAL_WORK, TODAY),
                new NoteCondition(EligibilityCondition.FEVER_OR_ILLNESS, null));
    }

    @Test
    void rejectsReplyWithoutJson() {
        assertThatThrownBy(() -> service("", "http://127.0.0.1:9").parseConditions("no idea", TODAY))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsExplanationInTheChosenScriptAndRejectsOtherScripts() {
        assertThat(EligibilityService.cleanExplanation("మీరు 12 ఆగస్టు 2027 నుండి రక్తదానం చేయవచ్చు. BloodConnect", "te"))
                .startsWith("మీరు");
        assertThatThrownBy(() -> EligibilityService.cleanExplanation("12 నెలల 기다లి అవసరం", "te"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> EligibilityService.cleanExplanation("మీరు రక్తदान చేయలేరు", "te"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> EligibilityService.cleanExplanation("You can donate. धन्यवाद", "en"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unreachableModelStillGivesARulesBasedAnswer() {
        EligibilityRequest request = new EligibilityRequest("te", 30, 70.0, "MALE", null, null, null, null, null,
                null, null, null, true, false, "I had a cold last week");

        EligibilityResponse response = service("test-key", "http://127.0.0.1:9").check(request, null);

        assertThat(response.eligible()).isFalse();
        assertThat(response.deferredUntil()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(response.aiUsed()).isFalse();
        assertThat(response.language()).isEqualTo("en");
        assertThat(response.noteSkipped()).isTrue();
        assertThat(response.savedToProfile()).isFalse();
        assertThat(response.explanation()).contains("you can donate again from");
    }

    private static EligibilityService service(String apiKey, String baseUrl) {
        return new EligibilityService(new OpenRouterClient(apiKey, "test/model:free", baseUrl, Duration.ofSeconds(2)),
                new ObjectMapper(), mock(DonorService.class));
    }
}
