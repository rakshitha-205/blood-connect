package com.bloodconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/bloodconnect_test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.mail.host=",
        "bloodconnect.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "bloodconnect.jwt.expiration=PT1H",
        "bloodconnect.otp.expose-dev-code=true"
})
@AutoConfigureMockMvc
class PasswordResetApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void resetCodeFromEmailLetsTheUserLogInWithANewPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ravi","email":"ravi.reset@example.com","phone":"9123456782",
                                "password":"old-password-123","role":"DONOR"}
                                """))
                .andExpect(status().isCreated());

        String code = objectMapper.readTree(mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Ravi.Reset@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("r••••••••t@example.com"))
                .andReturn().getResponse().getContentAsString()).path("devCode").asText();

        String wrongCode = code.equals("000000") ? "111111" : "000000";
        mockMvc.perform(post("/api/auth/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody(wrongCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("That code is incorrect. 4 tries left."));

        mockMvc.perform(post("/api/auth/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody(code)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody(wrongCode, "new-password-456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("That code is incorrect. 3 tries left."));

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody(code, "new-password-456")))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("old-password-123")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("new-password-456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.emailVerified").value(true));

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody(code, "another-password-789")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownEmailGetsTheSameAnswerWithoutACode() throws Exception {
        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("n••••y@example.com"))
                .andExpect(jsonPath("$.devCode").doesNotExist());
    }

    private static String verifyBody(String code) {
        return "{\"email\":\"ravi.reset@example.com\",\"code\":\"" + code + "\"}";
    }

    private static String resetBody(String code, String newPassword) {
        return "{\"email\":\"ravi.reset@example.com\",\"code\":\"" + code + "\",\"newPassword\":\"" + newPassword + "\"}";
    }

    private static String loginBody(String password) {
        return "{\"email\":\"ravi.reset@example.com\",\"password\":\"" + password + "\"}";
    }
}
