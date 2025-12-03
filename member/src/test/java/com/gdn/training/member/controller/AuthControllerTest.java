package com.gdn.training.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdn.training.member.dto.LoginRequest;
import com.gdn.training.member.dto.RegisterRequest;
import com.gdn.training.member.dto.UserInfoResponse;
import com.gdn.training.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @Test
    void registerReturnsSuccessResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("Secret123!");

        mockMvc.perform(post("/internal/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("User registered successfully")));

        verify(memberService).register(any(RegisterRequest.class));
    }

    @Test
    void validateCredentialsReturnsUserInfo() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("Secret123!");

        when(memberService.validateCredentials(any(LoginRequest.class))).thenReturn(UserInfoResponse.builder()
                .id(1L)
                .email(request.getEmail())
                .name("Alice")
                .role("ROLE_USER")
                .build());

        mockMvc.perform(post("/internal/auth/validate-credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is("alice@example.com")))
                .andExpect(jsonPath("$.message", is("Credentials validated")));

        verify(memberService).validateCredentials(any(LoginRequest.class));
    }
}

