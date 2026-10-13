package com.bloodconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/bloodconnect_test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.mail.host=",
        "bloodconnect.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "bloodconnect.jwt.expiration=PT1H",
        "bloodconnect.otp.required=false",
        "bloodconnect.ai.openrouter.api-key=",
        "bloodconnect.eligibility.max-checks-per-hour=3"
})
@AutoConfigureMockMvc
class EligibilityApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void anyoneCanCheckWithoutAnAccountAndGetsTheRulesAnswerWhenAiIsOff() throws Exception {
        mockMvc.perform(check("10.0.0.1", body(null, "I had a fever")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.aiUsed").value(false))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.noteSkipped").value(true))
                .andExpect(jsonPath("$.savedToProfile").value(false))
                .andExpect(jsonPath("$.explanation").value(org.hamcrest.Matchers.containsString("you can donate blood today")));

        mockMvc.perform(check("10.0.0.1", "{\"weightKg\":70,\"sex\":\"MALE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Enter your age"));
    }

    @Test
    void tooManyChecksFromOneAddressAreRefused() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(check("10.0.0.2", body(null, null))).andExpect(status().isOk());
        }
        mockMvc.perform(check("10.0.0.2", body(null, null)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void donorWhoMustWaitIsSavedAndLeftOutOfMatchesUntilTheDate() throws Exception {
        String donorToken = register("waiting-donor@example.com", "DONOR");
        String requesterToken = register("eligibility-requester@example.com", "REQUESTER");
        mockMvc.perform(put("/api/donors/me")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Waiting Donor","phone":"9999999999","bloodGroup":"O-",
                                "location":"Hyderabad","latitude":17.44,"longitude":78.39,"available":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/donors/nearby").param("bloodGroup", "O-")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        LocalDate tattoo = LocalDate.now().minusMonths(1);
        String expectedDate = tattoo.plusMonths(12).toString();
        mockMvc.perform(check("10.0.0.3", body(tattoo, null)).header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.deferredUntil").value(expectedDate))
                .andExpect(jsonPath("$.reasons[0].code").value("TATTOO_OR_PIERCING"))
                .andExpect(jsonPath("$.savedToProfile").value(true));

        mockMvc.perform(get("/api/donors/me").header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deferredUntil").value(expectedDate))
                .andExpect(jsonPath("$.eligibilityCheckedAt").isNotEmpty());

        mockMvc.perform(get("/api/donors/nearby").param("bloodGroup", "O-")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/donors/me/availability")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":true}"))
                .andExpect(status().isConflict());

        mockMvc.perform(check("10.0.0.3", body(null, null)).header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.savedToProfile").value(true));

        mockMvc.perform(get("/api/donors/nearby").param("bloodGroup", "O-")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private static MockHttpServletRequestBuilder check(String address, String body) {
        return post("/api/eligibility/check")
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static String body(LocalDate tattoo, String note) {
        return """
                {"language":"en","age":30,"weightKg":70,"sex":"MALE","tattooOrPiercingDate":%s,"note":%s}
                """.formatted(tattoo == null ? "null" : "\"" + tattoo + "\"", note == null ? "null" : "\"" + note + "\"");
    }

    private String register(String email, String role) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test User","email":"%s","phone":"9876543210","password":"strong-pass-123","role":"%s"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("accessToken").asText();
    }
}
