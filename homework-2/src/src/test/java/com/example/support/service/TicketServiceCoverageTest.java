package com.example.support.service;

import com.example.support.audit.ClassificationLogger;
import com.example.support.classifier.Classifier;
import com.example.support.dto.ClassificationResult;
import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.TicketDto;
import com.example.support.dto.UpdateTicketRequest;
import com.example.support.exception.ClassificationLoggerUnavailableException;
import com.example.support.exception.TicketNotFoundException;
import com.example.support.importer.ImportParser;
import com.example.support.importer.ImportParserFactory;
import com.example.support.importer.ParseResult;
import com.example.support.model.Category;
import com.example.support.model.Priority;
import com.example.support.model.Status;
import com.example.support.model.TicketEntity;
import com.example.support.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketServiceCoverageTest {

    @Test
    void createTicketAutoClassifiesWhenRequested() {
        TicketRepository repo = mock(TicketRepository.class);
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Classifier classifier = mock(Classifier.class);
        ClassificationResult result = new ClassificationResult();
        result.setCategory(Category.account_access);
        result.setPriority(Priority.urgent);
        result.setConfidenceScore(0.87);
        result.setReasoning("urgent access issue");
        when(classifier.classify(any(), any())).thenReturn(result);

        TicketService service = new TicketService(
                repo,
                classifier,
                mock(ClassificationLogger.class),
                mock(TicketValidator.class),
                mock(ImportParserFactory.class)
        );

        CreateTicketRequest request = new CreateTicketRequest();
        request.setCustomerEmail("customer@example.com");
        request.setSubject("Login issue");
        request.setDescription("I cannot sign in to my account right now");

        TicketDto dto = service.createTicket(request, true);

        assertThat(dto.getCategory()).isEqualTo(Category.account_access);
        assertThat(dto.getPriority()).isEqualTo(Priority.urgent);
        verify(classifier).classify("Login issue", "I cannot sign in to my account right now");
    }

    @Test
    void getTicketByIdThrowsWhenMissing() {
        TicketRepository repo = mock(TicketRepository.class);
        when(repo.findById(any())).thenReturn(Optional.empty());
        TicketService service = new TicketService(
                repo,
                mock(Classifier.class),
                mock(ClassificationLogger.class),
                mock(TicketValidator.class),
                mock(ImportParserFactory.class)
        );

        assertThatThrownBy(() -> service.getTicketById(UUID.randomUUID()))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void updateTicketManualOverrideRejectsWhenLoggerUnavailable() {
        TicketRepository repo = mock(TicketRepository.class);
        TicketEntity entity = new TicketEntity();
        entity.setId(UUID.randomUUID());
        entity.setCategory(Category.other);
        entity.setPriority(Priority.low);
        entity.setStatus(Status.new_);
        when(repo.findById(any())).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ClassificationLogger logger = mock(ClassificationLogger.class);
        doThrow(new RuntimeException("boom")).when(logger).log(any(), any(), any(), anyDouble(), anyString(), any());

        TicketService service = new TicketService(
                repo,
                mock(Classifier.class),
                logger,
                mock(TicketValidator.class),
                mock(ImportParserFactory.class)
        );

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setCategory(Category.bug_report);
        request.setPriority(Priority.high);

        assertThatThrownBy(() -> service.updateTicket(entity.getId(), request))
                .isInstanceOf(ClassificationLoggerUnavailableException.class);
    }

    @Test
    void updateTicketMarksResolvedAtWhenResolved() {
        TicketRepository repo = mock(TicketRepository.class);
        TicketEntity entity = new TicketEntity();
        entity.setId(UUID.randomUUID());
        entity.setStatus(Status.new_);
        entity.setCategory(Category.other);
        entity.setPriority(Priority.medium);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        when(repo.findById(any())).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TicketService service = new TicketService(
                repo,
                mock(Classifier.class),
                mock(ClassificationLogger.class),
                mock(TicketValidator.class),
                mock(ImportParserFactory.class)
        );

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setStatus(Status.resolved);

        TicketDto dto = service.updateTicket(entity.getId(), request);

        assertThat(dto.getStatus()).isEqualTo(Status.resolved);
        assertThat(dto.getResolvedAt()).isNotNull();
    }

    @Test
    void classifyTicketPersistsUpdatedClassification() {
        TicketRepository repo = mock(TicketRepository.class);
        TicketEntity entity = new TicketEntity();
        entity.setId(UUID.randomUUID());
        entity.setSubject("Billing issue");
        entity.setDescription("My invoice contains duplicate charges");
        when(repo.findById(any())).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ClassificationLogger logger = mock(ClassificationLogger.class);
        Classifier classifier = mock(Classifier.class);
        ClassificationResult result = new ClassificationResult();
        result.setCategory(Category.billing_question);
        result.setPriority(Priority.high);
        result.setConfidenceScore(0.91);
        result.setReasoning("billing keywords found");
        when(classifier.classify("Billing issue", "My invoice contains duplicate charges")).thenReturn(result);

        TicketService service = new TicketService(repo, classifier, logger, mock(TicketValidator.class), mock(ImportParserFactory.class));

        ClassificationResult response = service.classifyTicket(entity.getId());

        assertThat(response.getCategory()).isEqualTo(Category.billing_question);
        assertThat(response.getPriority()).isEqualTo(Priority.high);
        verify(logger).log(any(), any(), any(), anyDouble(), anyString(), any());
    }

    @Test
    void importTicketsAggregatesSuccessfulAndFailedRows() throws Exception {
        TicketRepository repo = mock(TicketRepository.class);
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TicketValidator validator = mock(TicketValidator.class);
        when(validator.validate(any(CreateTicketRequest.class))).thenReturn(new ValidationResult(List.of()));
        ImportParserFactory parserFactory = mock(ImportParserFactory.class);
        ImportParser parser = mock(ImportParser.class);
        when(parserFactory.forContentType(any(), any())).thenReturn(parser);
        when(parser.parse(any())).thenReturn(List.of(
                new ParseResult(createRequest("ok@example.com"), 0, null),
                new ParseResult(null, 1, "row failed"),
                new ParseResult(createRequest("bad@example.com"), 2, null)
        ));
        when(validator.validate(any(CreateTicketRequest.class))).thenReturn(new ValidationResult(List.of()))
                .thenReturn(new ValidationResult(List.of(new FieldError("subject", "bad"))));

        TicketService service = new TicketService(
                repo,
                mock(Classifier.class),
                mock(ClassificationLogger.class),
                validator,
                parserFactory
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tickets.csv",
                "text/csv",
                "a,b".getBytes(StandardCharsets.UTF_8)
        );

        var summary = service.importTickets(file, false);

        assertThat(summary.getTotalRecords()).isEqualTo(3);
        assertThat(summary.getSuccessful()).isEqualTo(1);
        assertThat(summary.getFailed()).isEqualTo(2);
        assertThat(summary.getErrors()).hasSize(2);
    }

    private CreateTicketRequest createRequest(String email) {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setCustomerEmail(email);
        request.setSubject("Issue");
        request.setDescription("Need help with the current problem immediately");
        return request;
    }
}
