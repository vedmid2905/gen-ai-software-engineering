package com.example.support;

import com.example.support.dto.BulkImportSummary;
import com.example.support.dto.ClassificationResult;
import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.ImportError;
import com.example.support.dto.MetadataDto;
import com.example.support.dto.TicketDto;
import com.example.support.dto.TicketFilter;
import com.example.support.dto.UpdateTicketRequest;
import com.example.support.importer.ParseResult;
import com.example.support.model.Category;
import com.example.support.model.ClassificationLogEntry;
import com.example.support.model.DeviceType;
import com.example.support.model.Priority;
import com.example.support.model.Source;
import com.example.support.model.Status;
import com.example.support.model.TicketEntity;
import com.example.support.service.FieldError;
import com.example.support.service.ValidationResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageSmokeTest {

    @Test
    void exercisesSimpleDtosAndModelsAcrossTheApplication() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setCustomerId("cust-1");
        createRequest.setCustomerEmail("customer@example.com");
        createRequest.setCustomerName("Jane Doe");
        createRequest.setSubject("Login issue");
        createRequest.setDescription("I cannot sign in to my account right now");
        createRequest.setCategory(Category.account_access);
        createRequest.setPriority(Priority.urgent);
        createRequest.setStatus(Status.new_);
        createRequest.setAssignedTo("support-team");
        createRequest.setTags(List.of("login", "security"));
        createRequest.setSource(Source.web_form);
        createRequest.setBrowser("Chrome");
        createRequest.setDeviceType(DeviceType.desktop);

        assertThat(createRequest.getCustomerEmail()).isEqualTo("customer@example.com");
        assertThat(createRequest.getTags()).contains("security");

        UpdateTicketRequest updateRequest = new UpdateTicketRequest();
        updateRequest.setCustomerEmail("updated@example.com");
        updateRequest.setCustomerName("Updated Name");
        updateRequest.setSubject("Updated subject");
        updateRequest.setDescription("This description is now long enough for validation.");
        updateRequest.setCategory(Category.bug_report);
        updateRequest.setPriority(Priority.high);
        updateRequest.setStatus(Status.in_progress);
        updateRequest.setAssignedTo("agent-7");
        updateRequest.setTags(List.of("bug"));
        updateRequest.setSource(Source.email);
        updateRequest.setBrowser("Firefox");
        updateRequest.setDeviceType(DeviceType.mobile);

        assertThat(updateRequest.getStatus()).isEqualTo(Status.in_progress);
        assertThat(updateRequest.getTags()).containsExactly("bug");

        TicketDto ticketDto = new TicketDto();
        UUID id = UUID.randomUUID();
        ticketDto.setId(id);
        ticketDto.setCustomerId("cust-2");
        ticketDto.setCustomerEmail("ticket@example.com");
        ticketDto.setCustomerName("Ticket Owner");
        ticketDto.setSubject("Subject");
        ticketDto.setDescription("A detailed description for the ticket.");
        ticketDto.setCategory(Category.technical_issue);
        ticketDto.setPriority(Priority.medium);
        ticketDto.setStatus(Status.waiting_customer);
        Instant now = Instant.now();
        ticketDto.setCreatedAt(now);
        ticketDto.setUpdatedAt(now);
        ticketDto.setResolvedAt(now);
        ticketDto.setAssignedTo("support");
        ticketDto.setTags(List.of("tag-a"));
        MetadataDto metadata = new MetadataDto();
        metadata.setSource(Source.chat);
        metadata.setBrowser("Safari");
        metadata.setDeviceType(DeviceType.tablet);
        ticketDto.setMetadata(metadata);

        assertThat(ticketDto.getId()).isEqualTo(id);
        assertThat(ticketDto.getMetadata().getBrowser()).isEqualTo("Safari");

        ClassificationResult classificationResult = new ClassificationResult();
        classificationResult.setTicketId(id);
        classificationResult.setCategory(Category.feature_request);
        classificationResult.setPriority(Priority.low);
        classificationResult.setConfidenceScore(0.42);
        classificationResult.setReasoning("suggestion");
        classificationResult.setKeywordsFound(List.of("feature", "suggestion"));

        assertThat(classificationResult.getKeywordsFound()).contains("suggestion");

        BulkImportSummary summary = new BulkImportSummary();
        summary.setTotalRecords(3);
        summary.setSuccessful(2);
        summary.setFailed(1);
        summary.getErrors().add(new ImportError(1, "bad row"));
        assertThat(summary.getErrors()).hasSize(1);

        TicketFilter filter = new TicketFilter();
        filter.setCategory(Category.billing_question);
        filter.setPriority(Priority.high);
        filter.setStatus(Status.resolved);
        assertThat(filter.getStatus()).isEqualTo(Status.resolved);

        ImportError importError = new ImportError(4, "boom");
        assertThat(importError.getMessage()).contains("boom");

        ParseResult parseResult = new ParseResult(createRequest, 0, null);
        assertThat(parseResult.isSuccess()).isTrue();
        assertThat(parseResult.ticket()).isEqualTo(createRequest);

        TicketEntity entity = new TicketEntity();
        entity.setId(id);
        entity.setCustomerId("cust-3");
        entity.setCustomerEmail("entity@example.com");
        entity.setCustomerName("Entity User");
        entity.setSubject("Entity subject");
        entity.setDescription("Entity description for the test.");
        entity.setCategory(Category.other);
        entity.setPriority(Priority.medium);
        entity.setStatus(Status.closed);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setResolvedAt(now);
        entity.setAssignedTo("ops");
        entity.setTags(List.of("one", "two"));
        entity.setSource(Source.phone);
        entity.setBrowser("Edge");
        entity.setDeviceType(DeviceType.desktop);
        assertThat(entity.getTags()).contains("two");

        ClassificationLogEntry logEntry = new ClassificationLogEntry();
        logEntry.setId(UUID.randomUUID());
        logEntry.setTicketId(id);
        logEntry.setCategory(Category.billing_question);
        logEntry.setPriority(Priority.high);
        logEntry.setConfidenceScore(0.99);
        logEntry.setReasoning("billing keywords found");
        logEntry.setTimestamp(now);
        assertThat(logEntry.getReasoning()).contains("billing");

        FieldError fieldError = new FieldError("subject", "must not be blank");
        ValidationResult validationResult = new ValidationResult(List.of(fieldError));
        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getSummary()).contains("subject");

        assertThat(Category.valueOf("other").name()).isEqualTo("other");
        assertThat(Priority.values()).contains(Priority.low);
        assertThat(Status.values()).contains(Status.closed);
        assertThat(Source.values()).contains(Source.api);
        assertThat(DeviceType.values()).contains(DeviceType.mobile);
    }
}
