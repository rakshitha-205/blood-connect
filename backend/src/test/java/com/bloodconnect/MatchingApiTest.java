package com.bloodconnect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/bloodconnect_test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "bloodconnect.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "bloodconnect.jwt.expiration=PT1H",
        "bloodconnect.otp.required=false"
})
@AutoConfigureMockMvc
class MatchingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void requestMatchesAvailableCompatibleDonorsByDistance() throws Exception {
        String nearbyToken = register("nearby-donor@example.com", "DONOR");
        String distantToken = register("distant-donor@example.com", "DONOR");
        String unavailableToken = register("unavailable-donor@example.com", "DONOR");
        String incompatibleToken = register("incompatible-donor@example.com", "DONOR");
        String requesterToken = register("requester@example.com", "REQUESTER");

        saveDonor(nearbyToken, "Nearby A+", "A+", 12.9716, 77.5946, true);
        saveDonor(distantToken, "Distant O-", "O-", 13.0000, 77.5946, true);
        saveDonor(unavailableToken, "Unavailable A+", "A+", 12.9716, 77.5946, false);
        saveDonor(incompatibleToken, "Incompatible B+", "B+", 12.9716, 77.5946, true);

        MvcResult created = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientName":"Test Patient","bloodGroup":"A+","units":2,
                                "hospital":"City Hospital","location":"Bengaluru","latitude":12.9716,
                                "longitude":77.5946,"urgency":"Critical","contact":"9999999999","message":""}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        String requestId = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asText();

        MvcResult result = mockMvc.perform(get("/api/requests/{id}/matches", requestId)
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Nearby A+"))
                .andExpect(jsonPath("$[0].distanceKm").value(0.0))
                .andExpect(jsonPath("$[1].name").value("Distant O-"))
                .andExpect(jsonPath("$[1].phone").doesNotExist())
                .andReturn();

        JsonNode matches = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(matches).hasSize(2);

        mockMvc.perform(get("/api/donors/nearby")
                        .param("bloodGroup", "A+")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/donors/nearby")
                        .param("bloodGroup", "A+")
                        .header("Authorization", "Bearer " + nearbyToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/requests/{id}", requestId)
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId));

        mockMvc.perform(patch("/api/requests/{id}/status", requestId)
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FULFILLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));

        mockMvc.perform(get("/api/requests/{id}/matches", requestId)
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private String register(String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test User","email":"%s","phone":"9876543210","password":"strong-pass-123","role":"%s"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }

    private void saveDonor(String token, String name, String bloodGroup, double latitude,
                           double longitude, boolean available) throws Exception {
        mockMvc.perform(put("/api/donors/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","phone":"9999999999","bloodGroup":"%s",
                                "location":"Bengaluru","latitude":%s,"longitude":%s,"available":%s}
                                """.formatted(name, bloodGroup, latitude, longitude, available)))
                .andExpect(status().isOk());
    }
}
