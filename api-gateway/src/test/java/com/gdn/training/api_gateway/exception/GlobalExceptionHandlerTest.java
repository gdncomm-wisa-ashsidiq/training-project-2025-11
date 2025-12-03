package com.gdn.training.api_gateway.exception;

import com.gdn.training.common.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private MethodArgumentNotValidException validationException;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/test-path");
    }

    @Test
    void handleIllegalArgumentReturnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input");
        assertThat(response.getBody().getPath()).isEqualTo("/test-path");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleIllegalStateReturnsServiceUnavailable() {
        IllegalStateException ex = new IllegalStateException("Service down");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalState(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        assertThat(response.getBody().getError()).isEqualTo("Service Unavailable");
        assertThat(response.getBody().getMessage()).contains("try again shortly");
        assertThat(response.getBody().getPath()).isEqualTo("/test-path");
    }

    @Test
    void handleValidationReturnsBadRequest() {
        FieldError fieldError1 = new FieldError("object", "field1", "must not be null");
        FieldError fieldError2 = new FieldError("object", "field2", "must be valid");

        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidation(validationException, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
        assertThat(response.getBody().getMessage()).contains("field1: must not be null");
        assertThat(response.getBody().getMessage()).contains("field2: must be valid");
        assertThat(response.getBody().getPath()).isEqualTo("/test-path");
    }

    @Test
    void handleGenericReturnsInternalServerError() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).contains("unexpected error");
        assertThat(response.getBody().getPath()).isEqualTo("/test-path");
    }
}
