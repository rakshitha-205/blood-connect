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
class ConnectApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void donorAcceptsThenBothSidesReviewOnceTheRequestIsFulfilled() throws Exception {
        JsonNode donor = register("accept-donor@example.com", "DONOR");
        JsonNode patient = register("accept-patient@example.com", "REQUESTER");
        String donorToken = donor.path("accessToken").asText();
        String patientToken = patient.path("accessToken").asText();
        String donorUserId = donor.path("user").path("id").asText();

        mockMvc.perform(put("/api/donors/me").header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ravi","phone":"9876501234","bloodGroup":"O-","location":"Bengaluru",
                                "latitude":12.97,"longitude":77.59,"available":true}"""))
                .andExpect(status().isOk());
        String requestId = createRequest(patientToken);

        mockMvc.perform(post("/api/donors/me/alerts/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/donors/me/alerts/{id}/accept", requestId)
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/donors/me/alerts").header("Authorization", "Bearer " + donorToken))
                .andExpect(jsonPath("$[?(@.requestId == '%s')]".formatted(requestId)).isEmpty());
        mockMvc.perform(get("/api/donors/me/accepted").header("Authorization", "Bearer " + donorToken))
                .andExpect(jsonPath("$[0].requestId").value(requestId))
                .andExpect(jsonPath("$[0].contact").value("9999999999"))
                .andExpect(jsonPath("$[0].reviewed").value(false));

        mockMvc.perform(get("/api/requests/{id}/acceptances", requestId)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ravi"))
                .andExpect(jsonPath("$[0].phone").value("9876501234"))
                .andExpect(jsonPath("$[0].donorUserId").value(donorUserId));

        String review = """
                {"requestId":"%s","revieweeId":"%s","rating":5,"comment":"Came within the hour"}"""
                .formatted(requestId, donorUserId);
        mockMvc.perform(post("/api/reviews").header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON).content(review))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/requests/{id}/status", requestId)
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FULFILLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reviews").header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON).content(review))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));
        mockMvc.perform(post("/api/reviews").header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON).content(review))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/reviews").header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"%s\",\"rating\":4}".formatted(requestId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/users/{id}", donorUserId).header("Authorization", "Bearer " + patientToken))
                .andExpect(jsonPath("$.average").value(5.0))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.reviews[0].comment").value("Came within the hour"));
        mockMvc.perform(get("/api/reviews/me").header("Authorization", "Bearer " + patientToken))
                .andExpect(jsonPath("$.average").value(4.0));
        mockMvc.perform(get("/api/requests/{id}/acceptances", requestId)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(jsonPath("$[0].rating").value(5.0))
                .andExpect(jsonPath("$[0].reviewed").value(true));
    }

    @Test
    void strangersCannotReview() throws Exception {
        String patientToken = register("stranger-patient@example.com", "REQUESTER").path("accessToken").asText();
        String strangerToken = register("stranger@example.com", "HOSPITAL").path("accessToken").asText();
        String requestId = createRequest(patientToken);

        mockMvc.perform(post("/api/reviews").header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"%s\",\"rating\":1}".formatted(requestId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void bloodBankPublishesStockThatAnyoneCanSearch() throws Exception {
        String bankToken = register("bank@example.com", "BLOOD_BANK").path("accessToken").asText();
        String donorToken = register("bank-donor@example.com", "DONOR").path("accessToken").asText();

        mockMvc.perform(put("/api/blood-banks/me/stock").header("Authorization", "Bearer " + bankToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stock\":{\"O-\":3}}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/blood-banks/me").header("Authorization", "Bearer " + bankToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"City Blood Bank","phone":"9876543210","location":"Indiranagar",
                                "latitude":12.97,"longitude":77.64,"openHours":"24 hours"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+919876543210"));
        mockMvc.perform(put("/api/blood-banks/me/stock").header("Authorization", "Bearer " + bankToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stock\":{\"XX\":3}}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/blood-banks/me/stock").header("Authorization", "Bearer " + bankToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stock\":{\"O-\":3,\"A+\":0}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock['O-']").value(3))
                .andExpect(jsonPath("$.totalUnits").value(3));
        mockMvc.perform(put("/api/blood-banks/me").header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"phone\":\"9876543210\",\"location\":\"x\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/blood-banks").param("bloodGroup", "O-")
                        .param("latitude", "12.97").param("longitude", "77.59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("City Blood Bank"))
                .andExpect(jsonPath("$[0].distanceKm").isNumber());
        mockMvc.perform(get("/api/blood-banks").param("bloodGroup", "A+"))
                .andExpect(jsonPath("$").isEmpty());
    }

    private String createRequest(String token) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientName":"Asha","bloodGroup":"A+","units":1,
                                "hospital":"City Hospital","location":"Bengaluru","latitude":12.97,
                                "longitude":77.59,"urgency":"Critical","contact":"9999999999","message":""}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asText();
    }

    private JsonNode register(String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test User","email":"%s","phone":"9876543210","password":"strong-pass-123","role":"%s"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
