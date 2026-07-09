package com.example.support.exception;

import com.example.support.dto.CreateTicketRequest;
import com.example.support.model.Category;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesTicketNotFound() {
        ResponseEntity<Map<String, String>> response = handler.handleNotFound(new TicketNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", response.getBody().get("message"));
    }

    @Test
    void handlesUnsupportedImportFormat() {
        ResponseEntity<Map<String, String>> response = handler.handleUnsupportedFormat(new UnsupportedImportFormatException(".bak"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).containsEntry("message", "Unsupported import format: .bak");
    }

    @Test
    void handlesLoggerUnavailable() {
        ResponseEntity<Map<String, String>> response = handler.handleLoggerUnavailable(new ClassificationLoggerUnavailableException("logger down", new IllegalStateException("boom")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("message", "logger down");
    }

    @Test
    void handlesConstraintViolation() {
        ConstraintViolation<CreateTicketRequest> violation = mock(ConstraintViolation.class);
        Path path = org.hibernate.validator.internal.engine.path.PathImpl.createPathFromString("customerEmail");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be a valid email");

        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));
        ResponseEntity<Map<String, String>> response = handler.handleConstraintViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "customerEmail: must be a valid email");
    }

    @Test
    void handlesIllegalArgument() {
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(new IllegalArgumentException("Import file cannot be empty"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Import file cannot be empty");
    }

    @Test
    void handlesIllegalState() {
        ResponseEntity<Map<String, String>> response = handler.handleIllegalState(new IllegalStateException("Failed to read import file"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Failed to read import file");
    }

    @Test
    void handlesGenericErrors() {
        ResponseEntity<Map<String, String>> response = handler.handleGeneric(new IllegalStateException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "An internal error occurred");
    }
}
