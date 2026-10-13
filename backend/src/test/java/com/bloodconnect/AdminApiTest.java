package com.bloodconnect;

import com.bloodconnect.request.RequestStatus;
import com.bloodconnect.user.UserRepository;
import com.bloodconnect.user.UserRole;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
class AdminApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void adminSeesHospitalsDonorsAndHelpedPatients() throws Exception {
        String hospital = register("City Hospital", "city@example.com", "HOSPITAL");
        String donor = register("Dina", "dina@example.com", "DONOR");
        register("Pat", "pat@example.com", "REQUESTER");
        register("Ada", "ada@example.com", "REQUESTER");

        mockMvc.perform(put("/api/donors/me").header("Authorization", bearer(donor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dina","phone":"9123456700","bloodGroup":"O-","location":"Gachibowli",
                                "latitude":17.44,"longitude":78.35,"available":true}
                                """))
                .andExpect(status().isOk());

        String requestId = objectMapper.readTree(mockMvc.perform(post("/api/requests")
                        .header("Authorization", bearer(hospital))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientName":"Ravi","bloodGroup":"O-","units":2,"hospital":"City Hospital",
                                "location":"Gachibowli","latitude":17.44,"longitude":78.35,"urgency":"critical",
                                "contact":"9123456701"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asText();
        mockMvc.perform(patch("/api/requests/" + requestId + "/status").header("Authorization", bearer(hospital))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + RequestStatus.FULFILLED + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/overview").header("Authorization", bearer(hospital)))
                .andExpect(status().isForbidden());

        String admin = register("Ann Admin", "ann@example.com", "REQUESTER");
        userRepository.findByEmail("ann@example.com").ifPresent(user -> {
            user.setRole(UserRole.ADMIN);
            userRepository.save(user);
        });

        mockMvc.perform(get("/api/admin/overview").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitals").value(1))
                .andExpect(jsonPath("$.donors").value(1))
                .andExpect(jsonPath("$.patients").value(2))
                .andExpect(jsonPath("$.helped").value(1));
        mockMvc.perform(get("/api/admin/hospitals").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[0].name").value("City Hospital"))
                .andExpect(jsonPath("$[0].requests").value(1))
                .andExpect(jsonPath("$[0].helped").value(1));
        mockMvc.perform(get("/api/admin/donors").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[0].bloodGroup").value("O-"))
                .andExpect(jsonPath("$[0].available").value(true));
        mockMvc.perform(get("/api/admin/helped").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[0].patientName").value("Ravi"))
                .andExpect(jsonPath("$[0].requestedBy").value("City Hospital"))
                .andExpect(jsonPath("$[0].requesterRole").value("HOSPITAL"));
    }

    @Test
    void nobodyCanRegisterAsAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mallory","email":"mallory@example.com","phone":"9123456702",
                                "password":"strong-pass-123","role":"ADMIN"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private String register(String name, String email, String role) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "name", name, "email", email, "phone", "9123456789",
                                "password", "strong-pass-123", "role", role))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
