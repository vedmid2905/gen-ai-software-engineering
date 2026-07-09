package com.example.support.service;

import com.example.support.audit.ClassificationLogger;
import com.example.support.classifier.Classifier;
import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.TicketDto;
import com.example.support.importer.ImportParserFactory;
import com.example.support.model.Status;
import com.example.support.repository.TicketRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketServiceTest {

    @Test
    void createTicketSetsDefaultStatusAndTimestamps() {
        TicketRepository repo = mock(TicketRepository.class);
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TicketService service = new TicketService(
                repo,
                mock(Classifier.class),
                mock(ClassificationLogger.class),
                mock(TicketValidator.class),
                mock(ImportParserFactory.class)
        );

        CreateTicketRequest request = new CreateTicketRequest();
        request.setCustomerEmail("customer@example.com");
        request.setSubject("Login issue");
        request.setDescription("I cannot sign in to my account");

        TicketDto dto = service.createTicket(request, false);

        assertThat(dto.getStatus()).isEqualTo(Status.new_);
        assertThat(dto.getCreatedAt()).isNotNull();
        assertThat(dto.getUpdatedAt()).isEqualTo(dto.getCreatedAt());
        assertThat(dto.getId()).isNotNull();
    }
}
