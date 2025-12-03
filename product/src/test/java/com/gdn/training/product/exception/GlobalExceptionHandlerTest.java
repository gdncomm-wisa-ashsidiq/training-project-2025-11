package com.gdn.training.product.exception;

import com.gdn.training.common.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

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
    void handleDatabaseErrorsReturnsServiceUnavailable() {
        DataAccessException ex = new DataAccessException("DB connection lost") {};

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDatabaseErrors(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        assertThat(response.getBody().getError()).isEqualTo("Database Error");
        assertThat(response.getBody().getMessage()).contains("Database is temporarily unavailable");
        assertThat(response.getBody().getPath()).isEqualTo("/test-path");
    }

    @Test
    void handleGenericErrorReturnsInternalServerError() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericError(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).contains("unexpected error");
        assertThat(response.getBody().getPath()).isEqualTo("/test-path");
    }
}
