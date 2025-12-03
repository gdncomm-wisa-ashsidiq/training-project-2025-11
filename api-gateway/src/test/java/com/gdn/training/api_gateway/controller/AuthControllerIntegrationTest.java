package com.gdn.training.api_gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdn.training.api_gateway.client.MemberClient;
import com.gdn.training.api_gateway.dto.LoginRequest;
import com.gdn.training.api_gateway.dto.RegisterRequest;
import com.gdn.training.api_gateway.dto.UserInfoDTO;
import com.gdn.training.api_gateway.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberClient memberClient;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        reset(memberClient, tokenBlacklistService);
    }

    @Test
    void registerEndpointReturnsSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setName("User");
        request.setPassword("Secret123!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        verify(memberClient).register(any(RegisterRequest.class));
    }

    @Test
    void loginEndpointIssuesCookie() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Secret123!");

        when(memberClient.validateCredentials(any(LoginRequest.class))).thenReturn(UserInfoDTO.builder()
                .id(123456789L)
                .email("user@example.com")
                .name("User")
                .role("ROLE_USER")
                .build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("ACCESS_TOKEN=")))
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void logoutEndpointBlacklistsToken() throws Exception {
        when(memberClient.validateCredentials(any(LoginRequest.class))).thenReturn(UserInfoDTO.builder()
                .id(123456789L)
                .email("user@example.com")
                .name("User")
                .role("ROLE_USER")
                .build());

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest("user@example.com", "Secret123!"))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("ACCESS_TOKEN=")));

        verify(tokenBlacklistService).blacklist(any(), any());
    }
}

