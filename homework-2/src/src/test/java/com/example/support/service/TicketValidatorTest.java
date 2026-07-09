package com.example.support.service;

import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.UpdateTicketRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

class TicketValidatorTest {

    private TicketValidator validator;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.afterPropertiesSet();
        validator = new TicketValidator(factory);
    }

    @Test
    void validateCreateRequestReturnsFieldErrorsForInvalidInput() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setCustomerEmail("not-an-email");
        request.setSubject(" ");
        request.setDescription("too short");

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(3);
        assertThat(result.getErrors()).extracting(FieldError::getField).contains("customerEmail", "subject", "description");
    }

    @Test
    void validateUpdateRequestSucceedsForValidInput() {
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setCustomerEmail("customer@example.com");
        request.setSubject("A valid subject");
        request.setDescription("This description is long enough for validation.");

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void validateCreateRequestRejectsBlankCustomerEmail() {
        CreateTicketRequest request = validCreateRequest();
        request.setCustomerEmail("   ");

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).extracting(FieldError::getField).contains("customerEmail");
    }

    @Test
    void validateCreateRequestAcceptsSubjectAtMaxBoundary() {
        CreateTicketRequest request = validCreateRequest();
        request.setSubject("A".repeat(200));

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateCreateRequestRejectsSubjectOverMaxBoundary() {
        CreateTicketRequest request = validCreateRequest();
        request.setSubject("A".repeat(201));

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).extracting(FieldError::getField).contains("subject");
    }

    @Test
    void validateCreateRequestAcceptsDescriptionAtMinBoundary() {
        CreateTicketRequest request = validCreateRequest();
        request.setDescription("A".repeat(10));

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateCreateRequestRejectsDescriptionUnderMinBoundary() {
        CreateTicketRequest request = validCreateRequest();
        request.setDescription("A".repeat(9));

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).extracting(FieldError::getField).contains("description");
    }

    @Test
    void validateCreateRequestAcceptsDescriptionAtMaxBoundary() {
        CreateTicketRequest request = validCreateRequest();
        request.setDescription("A".repeat(2000));

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateCreateRequestRejectsDescriptionOverMaxBoundary() {
        CreateTicketRequest request = validCreateRequest();
        request.setDescription("A".repeat(2001));

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).extracting(FieldError::getField).contains("description");
    }

    @Test
    void validateUpdateRequestWithAllFieldsNullIsValid() {
        UpdateTicketRequest request = new UpdateTicketRequest();

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateUpdateRequestRejectsInvalidEmail() {
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setCustomerEmail("not-an-email");

        ValidationResult result = validator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).extracting(FieldError::getField).contains("customerEmail");
    }

    private CreateTicketRequest validCreateRequest() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setCustomerEmail("customer@example.com");
        request.setSubject("A valid subject");
        request.setDescription("This description is long enough for validation.");
        return request;
    }
}
