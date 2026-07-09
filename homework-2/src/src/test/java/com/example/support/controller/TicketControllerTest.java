package com.example.support.controller;

import com.example.support.dto.*;
import com.example.support.exception.ClassificationLoggerUnavailableException;
import com.example.support.exception.GlobalExceptionHandler;
import com.example.support.exception.TicketNotFoundException;
import com.example.support.model.Category;
import com.example.support.model.Priority;
import com.example.support.model.Source;
import com.example.support.model.Status;
import com.example.support.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring MVC tests for {@link TicketController}.
 * Uses {@code @ExtendWith(MockitoExtension.class)} + {@code MockMvcBuilders.standaloneSetup}
 * so no Spring context is required — tests are fast and focused on HTTP behaviour.
 */
@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController controller;

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private TicketDto sampleTicketDto(UUID id) {
        TicketDto dto = new TicketDto();
        dto.setId(id);
        dto.setCustomerId("cust-1");
        dto.setCustomerEmail("user@example.com");
        dto.setCustomerName("Alice");
        dto.setSubject("Login issue");
        dto.setDescription("Cannot log in to my account since yesterday");
        dto.setCategory(Category.technical_issue);
        dto.setPriority(Priority.high);
        dto.setStatus(Status.new_);
        dto.setCreatedAt(Instant.now());
        dto.setUpdatedAt(Instant.now());
        dto.setTags(List.of("login", "auth"));
        MetadataDto meta = new MetadataDto();
        meta.setSource(Source.web_form);
        meta.setBrowser("Chrome");
        dto.setMetadata(meta);
        return dto;
    }

    private CreateTicketRequest validCreateRequest() {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setCustomerId("cust-1");
        req.setCustomerEmail("user@example.com");
        req.setCustomerName("Alice");
        req.setSubject("Login issue");
        req.setDescription("Cannot log in to my account since yesterday");
        req.setCategory(Category.technical_issue);
        req.setPriority(Priority.high);
        return req;
    }

    private UpdateTicketRequest validUpdateRequest() {
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setSubject("Updated subject");
        req.setDescription("Updated description with enough text here.");
        req.setStatus(Status.in_progress);
        return req;
    }

    // =========================================================================
    // POST /tickets
    // =========================================================================

    @Nested
    @DisplayName("POST /tickets")
    class CreateTicket {

        @Test
        @DisplayName("valid request → 201 with created ticket body")
        void createTicket_validPayload_returns201() throws Exception {
            UUID id = UUID.randomUUID();
            TicketDto dto = sampleTicketDto(id);
            when(ticketService.createTicket(any(CreateTicketRequest.class), eq(false))).thenReturn(dto);

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.subject").value("Login issue"))
                    .andExpect(jsonPath("$.status").value("new"));

            verify(ticketService).createTicket(any(CreateTicketRequest.class), eq(false));
        }

        @Test
        @DisplayName("autoClassify=true is passed through to service")
        void createTicket_withAutoClassify_passesFlag() throws Exception {
            UUID id = UUID.randomUUID();
            when(ticketService.createTicket(any(CreateTicketRequest.class), eq(true))).thenReturn(sampleTicketDto(id));

            mockMvc.perform(post("/tickets")
                            .param("autoClassify", "true")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isCreated());

            verify(ticketService).createTicket(any(CreateTicketRequest.class), eq(true));
        }

        @Test
        @DisplayName("missing required fields → 400 with message")
        void createTicket_invalidPayload_returns400() throws Exception {
            CreateTicketRequest bad = new CreateTicketRequest();
            // customerEmail and subject are @NotBlank — leave them null

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());

            verifyNoInteractions(ticketService);
        }

        @Test
        @DisplayName("invalid email → 400")
        void createTicket_invalidEmail_returns400() throws Exception {
            CreateTicketRequest req = validCreateRequest();
            req.setCustomerEmail("not-an-email");

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("subject too long (>200 chars) → 400")
        void createTicket_subjectTooLong_returns400() throws Exception {
            CreateTicketRequest req = validCreateRequest();
            req.setSubject("A".repeat(201));

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("description too short (<10 chars) → 400")
        void createTicket_descriptionTooShort_returns400() throws Exception {
            CreateTicketRequest req = validCreateRequest();
            req.setDescription("Short");

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // GET /tickets
    // =========================================================================

    @Nested
    @DisplayName("GET /tickets")
    class GetTickets {

        @Test
        @DisplayName("no filter → 200 with list of tickets")
        void getTickets_noFilter_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            when(ticketService.getTickets(any(TicketFilter.class))).thenReturn(List.of(sampleTicketDto(id)));

            mockMvc.perform(get("/tickets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(id.toString()));
        }

        @Test
        @DisplayName("empty result set → 200 with empty array")
        void getTickets_empty_returns200EmptyList() throws Exception {
            when(ticketService.getTickets(any(TicketFilter.class))).thenReturn(List.of());

            mockMvc.perform(get("/tickets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("category filter is passed to service")
        void getTickets_withCategoryFilter_delegatesToService() throws Exception {
            when(ticketService.getTickets(any(TicketFilter.class))).thenReturn(List.of());

            mockMvc.perform(get("/tickets").param("category", "technical_issue"))
                    .andExpect(status().isOk());

            verify(ticketService).getTickets(argThat(f -> f.getCategory() == Category.technical_issue));
        }

        @Test
        @DisplayName("priority filter is passed to service")
        void getTickets_withPriorityFilter_delegatesToService() throws Exception {
            when(ticketService.getTickets(any(TicketFilter.class))).thenReturn(List.of());

            mockMvc.perform(get("/tickets").param("priority", "high"))
                    .andExpect(status().isOk());

            verify(ticketService).getTickets(argThat(f -> f.getPriority() == Priority.high));
        }

        @Test
        @DisplayName("status filter is passed to service")
        void getTickets_withStatusFilter_delegatesToService() throws Exception {
            when(ticketService.getTickets(any(TicketFilter.class))).thenReturn(List.of());

            mockMvc.perform(get("/tickets").param("status", "in_progress"))
                    .andExpect(status().isOk());

            verify(ticketService).getTickets(argThat(f -> f.getStatus() == Status.in_progress));
        }
    }

    // =========================================================================
    // GET /tickets/{id}
    // =========================================================================

    @Nested
    @DisplayName("GET /tickets/{id}")
    class GetTicketById {

        @Test
        @DisplayName("existing ticket → 200 with ticket body")
        void getTicketById_found_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            when(ticketService.getTicketById(id)).thenReturn(sampleTicketDto(id));

            mockMvc.perform(get("/tickets/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.subject").value("Login issue"));
        }

        @Test
        @DisplayName("non-existent ticket → 404 with message")
        void getTicketById_notFound_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            when(ticketService.getTicketById(id)).thenThrow(new TicketNotFoundException(id));

            mockMvc.perform(get("/tickets/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString(id.toString())));
        }
    }

    // =========================================================================
    // PUT /tickets/{id}
    // =========================================================================

    @Nested
    @DisplayName("PUT /tickets/{id}")
    class UpdateTicket {

        @Test
        @DisplayName("valid update → 200 with updated ticket")
        void updateTicket_valid_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            TicketDto dto = sampleTicketDto(id);
            dto.setStatus(Status.in_progress);
            when(ticketService.updateTicket(eq(id), any(UpdateTicketRequest.class))).thenReturn(dto);

            mockMvc.perform(put("/tickets/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.status").value("in_progress"));
        }

        @Test
        @DisplayName("non-existent ticket → 404")
        void updateTicket_notFound_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            when(ticketService.updateTicket(eq(id), any(UpdateTicketRequest.class)))
                    .thenThrow(new TicketNotFoundException(id));

            mockMvc.perform(put("/tickets/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("invalid email in update → 400")
        void updateTicket_invalidEmail_returns400() throws Exception {
            UUID id = UUID.randomUUID();
            UpdateTicketRequest bad = new UpdateTicketRequest();
            bad.setCustomerEmail("not-an-email");

            mockMvc.perform(put("/tickets/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());

            verifyNoInteractions(ticketService);
        }

        @Test
        @DisplayName("classifier logger unavailable → 503")
        void updateTicket_loggerUnavailable_returns503() throws Exception {
            UUID id = UUID.randomUUID();
            when(ticketService.updateTicket(eq(id), any(UpdateTicketRequest.class)))
                    .thenThrow(new ClassificationLoggerUnavailableException("Logger down", new RuntimeException()));

            UpdateTicketRequest req = new UpdateTicketRequest();
            req.setCategory(Category.billing_question);

            mockMvc.perform(put("/tickets/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // =========================================================================
    // DELETE /tickets/{id}
    // =========================================================================

    @Nested
    @DisplayName("DELETE /tickets/{id}")
    class DeleteTicket {

        @Test
        @DisplayName("existing ticket → 204 no content")
        void deleteTicket_exists_returns204() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(ticketService).deleteTicket(id);

            mockMvc.perform(delete("/tickets/{id}", id))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("non-existent ticket → 404 with message")
        void deleteTicket_notFound_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new TicketNotFoundException(id)).when(ticketService).deleteTicket(id);

            mockMvc.perform(delete("/tickets/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString(id.toString())));
        }
    }

    // =========================================================================
    // POST /tickets/import
    // =========================================================================

    @Nested
    @DisplayName("POST /tickets/import")
    class ImportTickets {

        @Test
        @DisplayName("valid CSV file → 200 with import summary")
        void importTickets_csv_returns200() throws Exception {
            BulkImportSummary summary = new BulkImportSummary();
            summary.setTotalRecords(3);
            summary.setSuccessful(3);
            summary.setFailed(0);
            when(ticketService.importTickets(any(), eq(false))).thenReturn(summary);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "tickets.csv", "text/csv", "id,subject\n1,test".getBytes());

            mockMvc.perform(multipart("/tickets/import").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total_records").value(3))
                    .andExpect(jsonPath("$.successful").value(3))
                    .andExpect(jsonPath("$.failed").value(0));
        }

        @Test
        @DisplayName("autoClassify=true is forwarded to service")
        void importTickets_withAutoClassify_passesFlag() throws Exception {
            BulkImportSummary summary = new BulkImportSummary();
            when(ticketService.importTickets(any(), eq(true))).thenReturn(summary);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "tickets.json", "application/json", "[]".getBytes());

            mockMvc.perform(multipart("/tickets/import")
                            .file(file)
                            .param("autoClassify", "true"))
                    .andExpect(status().isOk());

            verify(ticketService).importTickets(any(), eq(true));
        }

        @Test
        @DisplayName("partial import (mix of valid/invalid) → 200 with error details")
        void importTickets_partial_returns200WithErrors() throws Exception {
            BulkImportSummary summary = new BulkImportSummary();
            summary.setTotalRecords(2);
            summary.setSuccessful(1);
            summary.setFailed(1);
            summary.getErrors().add(new ImportError(1, "Invalid email"));
            when(ticketService.importTickets(any(), eq(false))).thenReturn(summary);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "tickets.csv", "text/csv", "data".getBytes());

            mockMvc.perform(multipart("/tickets/import").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total_records").value(2))
                    .andExpect(jsonPath("$.failed").value(1))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].record_index").value(1));
        }

        @Test
        @DisplayName("unsupported format → 415")
        void importTickets_unsupportedFormat_returns415() throws Exception {
            when(ticketService.importTickets(any(), anyBoolean()))
                    .thenThrow(new com.example.support.exception.UnsupportedImportFormatException("pdf"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "tickets.pdf", "application/pdf", "data".getBytes());

            mockMvc.perform(multipart("/tickets/import").file(file))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("empty file → service throws IllegalArgumentException → 400")
        void importTickets_emptyFile_serviceThrows_returns400() throws Exception {
            when(ticketService.importTickets(any(), anyBoolean()))
                    .thenThrow(new IllegalArgumentException("Import file cannot be empty"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.csv", "text/csv", new byte[0]);

            mockMvc.perform(multipart("/tickets/import").file(file))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Import file cannot be empty"));
        }

        @Test
        @DisplayName("unreadable file → service throws IllegalStateException → 400")
        void importTickets_unreadableFile_serviceThrows_returns400() throws Exception {
            when(ticketService.importTickets(any(), anyBoolean()))
                    .thenThrow(new IllegalStateException("Failed to read import file"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "broken.csv", "text/csv", "data".getBytes());

            mockMvc.perform(multipart("/tickets/import").file(file))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Failed to read import file"));
        }
    }

    // =========================================================================
    // POST /tickets/{id}/auto-classify
    // =========================================================================

    @Nested
    @DisplayName("POST /tickets/{id}/auto-classify")
    class ClassifyTicket {

        @Test
        @DisplayName("existing ticket → 200 with classification result")
        void classifyTicket_found_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            ClassificationResult result = new ClassificationResult();
            result.setTicketId(id);
            result.setCategory(Category.technical_issue);
            result.setPriority(Priority.urgent);
            result.setConfidenceScore(0.9);
            result.setReasoning("Matched urgent keyword");
            result.setKeywordsFound(List.of("production down"));
            when(ticketService.classifyTicket(id)).thenReturn(result);

            mockMvc.perform(post("/tickets/{id}/auto-classify", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticket_id").value(id.toString()))
                    .andExpect(jsonPath("$.category").value("technical_issue"))
                    .andExpect(jsonPath("$.priority").value("urgent"))
                    .andExpect(jsonPath("$.confidence_score").value(0.9))
                    .andExpect(jsonPath("$.keywords_found[0]").value("production down"));
        }

        @Test
        @DisplayName("non-existent ticket → 404 with message")
        void classifyTicket_notFound_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            when(ticketService.classifyTicket(id)).thenThrow(new TicketNotFoundException(id));

            mockMvc.perform(post("/tickets/{id}/auto-classify", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString(id.toString())));
        }
    }

    // =========================================================================
    // Error handling — generic 500
    // =========================================================================

    @Nested
    @DisplayName("Generic error handling")
    class ErrorHandling {

        @Test
        @DisplayName("unexpected RuntimeException from service → 500 without stack trace")
        void unexpectedException_returns500() throws Exception {
            UUID id = UUID.randomUUID();
            when(ticketService.getTicketById(id)).thenThrow(new RuntimeException("Unexpected"));

            mockMvc.perform(get("/tickets/{id}", id))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("An internal error occurred"))
                    // stack trace must NOT be exposed
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }
    }
}
