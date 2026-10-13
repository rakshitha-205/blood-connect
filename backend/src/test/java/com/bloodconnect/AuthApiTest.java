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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/bloodconnect_test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "bloodconnect.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "bloodconnect.jwt.expiration=PT1H",
        "bloodconnect.otp.expose-dev-code=true",
        "bloodconnect.otp.phone-required=true"
})
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAndGetCurrentUser() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Asha Rao","email":"donor@example.com","phone":"98765 43210",
                                "password":"strong-pass-123","role":"DONOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.fullName").value("Asha Rao"))
                .andExpect(jsonPath("$.user.email").value("donor@example.com"))
                .andExpect(jsonPath("$.user.phone").value("+919876543210"))
                .andExpect(jsonPath("$.user.role").value("DONOR"))
                .andExpect(jsonPath("$.user.emailVerified").value(false))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();

        JsonNode registrationBody = objectMapper.readTree(registration.getResponse().getContentAsString());
        String registrationToken = registrationBody.path("accessToken").asText();
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + registrationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("donor@example.com"));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"DONOR@example.com","password":"strong-pass-123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode loginBody = objectMapper.readTree(login.getResponse().getContentAsString());
        assertThat(loginBody.path("accessToken").asText()).isNotBlank();
    }

    @Test
    void featuresStayLockedUntilEmailAndPhoneAreVerified() throws Exception {
        String token = objectMapper.readTree(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ravi","email":"ravi@example.com","phone":"9123456780",
                                "password":"strong-pass-123","role":"DONOR"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("accessToken").asText();

        mockMvc.perform(get("/api/donors/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VERIFICATION_REQUIRED"));

        String emailCode = sendCode(token, "EMAIL");
        mockMvc.perform(verify(token, "EMAIL", emailCode.equals("000000") ? "111111" : "000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("That code is incorrect. 4 tries left."));
        mockMvc.perform(verify(token, "EMAIL", emailCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.phoneVerified").value(false));

        MvcResult phoneSend = mockMvc.perform(post("/api/auth/otp/send").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"channel\":\"PHONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("•••••••••6780"))
                .andReturn();
        String phoneCode = objectMapper.readTree(phoneSend.getResponse().getContentAsString()).path("devCode").asText();
        mockMvc.perform(post("/api/auth/otp/send").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"channel\":\"PHONE\"}"))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/api/donors/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(verify(token, "PHONE", phoneCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneVerified").value(true));
        mockMvc.perform(get("/api/donors/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String sendCode(String token, String channel) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/otp/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"" + channel + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("r••i@example.com"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("devCode").asText();
    }

    private org.springframework.test.web.servlet.RequestBuilder verify(String token, String channel, String code) {
        return post("/api/auth/otp/verify")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"" + channel + "\",\"code\":\"" + code + "\"}");
    }
}
