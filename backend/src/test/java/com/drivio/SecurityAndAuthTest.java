package com.drivio;

import com.drivio.dto.LoginRequest;
import com.drivio.dto.RegisterRequest;
import com.drivio.enums.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class SecurityAndAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testUnauthenticatedProtectedEndpointRejected() throws Exception {
        mockMvc.perform(get("/api/bookings/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCustomerCannotAccessAdminEndpoints() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("sec_cust_" + System.nanoTime() + "@drivio.com");
        reg.setPassword("Password123!");
        reg.setFullName("Sec Customer");
        reg.setPhoneNumber("+919900001111");
        reg.setRole(Role.CUSTOMER);

        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        String token = json.get("token").asText();

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDriverCannotAccessAdminEndpoints() throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("sec_drv_" + System.nanoTime() + "@drivio.com");
        reg.setPassword("Password123!");
        reg.setFullName("Sec Driver");
        reg.setPhoneNumber("+919900002222");
        reg.setRole(Role.DRIVER);

        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        String token = json.get("token").asText();

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testInvalidLoginReturnsBadRequest() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setEmail("nonexistent@drivio.com");
        login.setPassword("wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest());
    }
}
