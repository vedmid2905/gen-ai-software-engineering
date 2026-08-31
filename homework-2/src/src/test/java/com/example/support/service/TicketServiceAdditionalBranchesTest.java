package com.example.support.service;

import com.example.support.audit.ClassificationLogger;
import com.example.support.classifier.Classifier;
import com.example.support.dto.ClassificationResult;
import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.TicketDto;
import com.example.support.dto.UpdateTicketRequest;
import com.example.support.exception.TicketNotFoundException;
import com.example.support.importer.ImportParserFactory;
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

class TicketServiceAdditionalBranchesTest {

    @Test
    void deleteTicketRemovesExistingEntity() {
        TicketRepository repo = mock(TicketRepository.class);
        TicketEntity entity = new TicketEntity();
        entity.setId(UUID.randomUUID());
        when(repo.findById(any())).thenReturn(Optional.of(entity));

        TicketService service = new TicketService(repo, mock(Classifier.class), mock(ClassificationLogger.class), mock(TicketValidator.class), mock(ImportParserFactory.class));
        service.deleteTicket(entity.getId());

        verify(repo).delete(entity);
    }

    @Test
    void getTicketsUsesFilterAndReturnsDtos() {
        TicketRepository repo = mock(TicketRepository.class);
        TicketEntity entity = new TicketEntity();
        entity.setId(UUID.randomUUID());
        entity.setSubject("A subject");
        entity.setDescription("A description long enough to pass");
        entity.setCustomerEmail("tester@example.com");
        entity.setCategory(Category.billing_question);
        entity.setPriority(Priority.high);
        entity.setStatus(Status.new_);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        when(repo.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(entity));

        TicketService service = new TicketService(repo, mock(Classifier.class), mock(ClassificationLogger.class), mock(TicketValidator.class), mock(ImportParserFactory.class));
        List<TicketDto> tickets = service.getTickets(new com.example.support.dto.TicketFilter());

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getSubject()).isEqualTo("A subject");
    }

    @Test
    void createTicketUsesDefaultStatusWhenStatusUnset() {
        TicketRepository repo = mock(TicketRepository.class);
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TicketService service = new TicketService(repo, mock(Classifier.class), mock(ClassificationLogger.class), mock(TicketValidator.class), mock(ImportParserFactory.class));

        CreateTicketRequest request = new CreateTicketRequest();
        request.setCustomerEmail("customer@example.com");
        request.setSubject("Subject");
        request.setDescription("This description is definitely long enough.");

        TicketDto dto = service.createTicket(request, false);
        assertThat(dto.getStatus()).isEqualTo(Status.new_);
    }

    @Test
    void importTicketsRejectsEmptyFiles() {
        TicketService service = new TicketService(mock(TicketRepository.class), mock(Classifier.class), mock(ClassificationLogger.class), mock(TicketValidator.class), mock(ImportParserFactory.class));
        MockMultipartFile empty = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);
        assertThatThrownBy(() -> service.importTickets(empty, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void importTicketsSurfaceParserFailures() throws Exception {
        TicketRepository repo = mock(TicketRepository.class);
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TicketValidator validator = mock(TicketValidator.class);
        when(validator.validate(any(CreateTicketRequest.class))).thenReturn(new ValidationResult(List.of()));
        ImportParserFactory parserFactory = mock(ImportParserFactory.class);
        com.example.support.importer.ImportParser parser = mock(com.example.support.importer.ImportParser.class);
        when(parserFactory.forContentType(any(), any())).thenReturn(parser);
        when(parser.parse(any())).thenReturn(List.of(new com.example.support.importer.ParseResult(null, 0, "bad parse")));
        TicketService service = new TicketService(repo, mock(Classifier.class), mock(ClassificationLogger.class), validator, parserFactory);

        MockMultipartFile file = new MockMultipartFile("file", "tickets.csv", "text/csv", "x".getBytes(StandardCharsets.UTF_8));
        var summary = service.importTickets(file, false);
        assertThat(summary.getFailed()).isEqualTo(1);
        assertThat(summary.getErrors()).hasSize(1);
    }

    @Test
    void classifyTicketThrowsWhenEntityMissing() {
        TicketRepository repo = mock(TicketRepository.class);
        when(repo.findById(any())).thenReturn(Optional.empty());
        TicketService service = new TicketService(repo, mock(Classifier.class), mock(ClassificationLogger.class), mock(TicketValidator.class), mock(ImportParserFactory.class));

        assertThatThrownBy(() -> service.classifyTicket(UUID.randomUUID())).isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void updateTicketAppliesManualOverride() {
        TicketRepository repo = mock(TicketRepository.class);
        TicketEntity entity = new TicketEntity();
        entity.setId(UUID.randomUUID());
        entity.setCategory(Category.other);
        entity.setPriority(Priority.low);
        entity.setStatus(Status.new_);
        when(repo.findById(any())).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ClassificationLogger logger = mock(ClassificationLogger.class);

        TicketService service = new TicketService(repo, mock(Classifier.class), logger, mock(TicketValidator.class), mock(ImportParserFactory.class));
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setCategory(Category.feature_request);
        request.setPriority(Priority.medium);

        TicketDto dto = service.updateTicket(entity.getId(), request);
        assertThat(dto.getCategory()).isEqualTo(Category.feature_request);
        assertThat(dto.getPriority()).isEqualTo(Priority.medium);
        verify(logger).log(any(), any(), any(), anyDouble(), anyString(), any());
    }
}
