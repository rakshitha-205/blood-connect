package com.bloodconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/bloodconnect_test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "bloodconnect.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "bloodconnect.jwt.expiration=PT1H",
        "bloodconnect.otp.expose-dev-code=true"
})
@AutoConfigureMockMvc
class EmailOnlyVerificationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void verifyingEmailAloneUnlocksTheApp() throws Exception {
        String token = objectMapper.readTree(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Asha","email":"asha@example.com","phone":"9123456781",
                                "password":"strong-pass-123","role":"DONOR"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("accessToken").asText();

        mockMvc.perform(get("/api/donors/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Verify your email to continue."));

        String code = objectMapper.readTree(mockMvc.perform(post("/api/auth/otp/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"EMAIL\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("devCode").asText();

        mockMvc.perform(post("/api/auth/otp/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"EMAIL\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneVerified").value(false));

        mockMvc.perform(get("/api/donors/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
