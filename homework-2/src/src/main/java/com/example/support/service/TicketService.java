package com.example.support.service;

import com.example.support.audit.ClassificationLogger;
import com.example.support.classifier.Classifier;
import com.example.support.dto.*;
import com.example.support.exception.ClassificationLoggerUnavailableException;
import com.example.support.exception.TicketNotFoundException;
import com.example.support.importer.ImportParser;
import com.example.support.importer.ImportParserFactory;
import com.example.support.importer.ParseResult;
import com.example.support.model.*;
import com.example.support.repository.TicketRepository;
import com.example.support.repository.TicketSpecification;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final Classifier classifier;
    private final ClassificationLogger classificationLogger;
    private final TicketValidator ticketValidator;
    private final ImportParserFactory importParserFactory;

    public TicketService(TicketRepository ticketRepository,
                         Classifier classifier,
                         ClassificationLogger classificationLogger,
                         TicketValidator ticketValidator,
                         ImportParserFactory importParserFactory) {
        this.ticketRepository = ticketRepository;
        this.classifier = classifier;
        this.classificationLogger = classificationLogger;
        this.ticketValidator = ticketValidator;
        this.importParserFactory = importParserFactory;
    }

    @Transactional
    public TicketDto createTicket(CreateTicketRequest request, boolean autoClassify) {
        TicketEntity entity = new TicketEntity();
        entity.setId(UUID.randomUUID());
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setStatus(Status.new_);

        applyRequest(entity, request);
        entity = ticketRepository.save(entity);

        if (autoClassify) {
            try {
                ClassificationResult classification = classifyEntity(entity);
                entity.setCategory(classification.getCategory());
                entity.setPriority(classification.getPriority());
                ticketRepository.save(entity);
                classificationLogger.log(entity.getId(), classification.getCategory(), classification.getPriority(),
                        classification.getConfidenceScore(), classification.getReasoning(), Instant.now());
            } catch (RuntimeException ex) {
                log.warn("Auto-classification failed for ticket {}", entity.getId(), ex);
            }
        }

        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<TicketDto> getTickets(TicketFilter filter) {
        Specification<TicketEntity> spec = TicketSpecification.withFilter(
                filter != null ? filter.getCategory() : null,
                filter != null ? filter.getPriority() : null,
                filter != null ? filter.getStatus() : null);
        return ticketRepository.findAll(spec).stream().map(this::toDto).collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketDto getTicketById(UUID id) {
        return ticketRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Transactional
    public TicketDto updateTicket(UUID id, UpdateTicketRequest request) {
        TicketEntity entity = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        boolean hasManualOverride = request.getCategory() != null || request.getPriority() != null;
        if (hasManualOverride) {
            if (request.getCategory() != null) {
                entity.setCategory(request.getCategory());
            }
            if (request.getPriority() != null) {
                entity.setPriority(request.getPriority());
            }
            try {
                classificationLogger.log(
                        entity.getId(),
                        entity.getCategory(),
                        entity.getPriority(),
                        1.0,
                        "Manual override applied",
                        Instant.now());
            } catch (RuntimeException ex) {
                throw new ClassificationLoggerUnavailableException("Classification logger unavailable; override rejected", ex);
            }
        }

        applyRequest(entity, request);
        Instant now = Instant.now();
        entity.setUpdatedAt(now);
        if ((entity.getStatus() == Status.resolved || entity.getStatus() == Status.closed) && entity.getResolvedAt() == null) {
            entity.setResolvedAt(now);
        }
        entity = ticketRepository.save(entity);
        return toDto(entity);
    }

    @Transactional
    public void deleteTicket(UUID id) {
        TicketEntity entity = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticketRepository.delete(entity);
    }

    @Transactional
    public ClassificationResult classifyTicket(UUID id) {
        TicketEntity entity = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ClassificationResult result = classifyEntity(entity);
        entity.setCategory(result.getCategory());
        entity.setPriority(result.getPriority());
        entity.setUpdatedAt(Instant.now());
        ticketRepository.save(entity);
        classificationLogger.log(entity.getId(), result.getCategory(), result.getPriority(),
                result.getConfidenceScore(), result.getReasoning(), Instant.now());
        return result;
    }

    @Transactional
    public BulkImportSummary importTickets(MultipartFile file, boolean autoClassify) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Import file cannot be empty");
        }

        BulkImportSummary summary = new BulkImportSummary();
        summary.setTotalRecords(0);
        summary.setSuccessful(0);
        summary.setFailed(0);

        try {
            ImportParser parser = importParserFactory.forContentType(file.getContentType(), file.getOriginalFilename());
            List<ParseResult> parseResults = parser.parse(file.getInputStream());
            summary.setTotalRecords(parseResults.size());

            for (ParseResult parseResult : parseResults) {
                if (!parseResult.isSuccess()) {
                    summary.setFailed(summary.getFailed() + 1);
                    summary.getErrors().add(new ImportError(parseResult.recordIndex(), parseResult.error()));
                    continue;
                }

                CreateTicketRequest request = parseResult.ticket();
                ValidationResult validation = ticketValidator.validate(request);
                if (!validation.isValid()) {
                    summary.setFailed(summary.getFailed() + 1);
                    summary.getErrors().add(new ImportError(parseResult.recordIndex(), validation.getSummary()));
                    continue;
                }

                try {
                    TicketDto created = createTicket(request, autoClassify);
                    summary.setSuccessful(summary.getSuccessful() + 1);
                } catch (RuntimeException ex) {
                    summary.setFailed(summary.getFailed() + 1);
                    summary.getErrors().add(new ImportError(parseResult.recordIndex(), ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read import file", ex);
        }

        return summary;
    }

    private ClassificationResult classifyEntity(TicketEntity entity) {
        ClassificationResult result = classifier.classify(entity.getSubject(), entity.getDescription());
        result.setTicketId(entity.getId());
        return result;
    }

    private void applyRequest(TicketEntity entity, CreateTicketRequest request) {
        entity.setCustomerId(request.getCustomerId());
        entity.setCustomerEmail(request.getCustomerEmail());
        entity.setCustomerName(request.getCustomerName());
        entity.setSubject(request.getSubject());
        entity.setDescription(request.getDescription());
        entity.setCategory(request.getCategory());
        entity.setPriority(request.getPriority());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : Status.new_);
        entity.setAssignedTo(request.getAssignedTo());
        entity.setTags(request.getTags() == null ? new ArrayList<>() : new ArrayList<>(request.getTags()));
        entity.setSource(request.getSource());
        entity.setBrowser(request.getBrowser());
        entity.setDeviceType(request.getDeviceType());
    }

    private void applyRequest(TicketEntity entity, UpdateTicketRequest request) {
        if (request.getCustomerEmail() != null) {
            entity.setCustomerEmail(request.getCustomerEmail());
        }
        if (request.getCustomerName() != null) {
            entity.setCustomerName(request.getCustomerName());
        }
        if (request.getSubject() != null) {
            entity.setSubject(request.getSubject());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getPriority() != null) {
            entity.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getAssignedTo() != null) {
            entity.setAssignedTo(request.getAssignedTo());
        }
        if (request.getTags() != null) {
            entity.setTags(new ArrayList<>(request.getTags()));
        }
        if (request.getSource() != null) {
            entity.setSource(request.getSource());
        }
        if (request.getBrowser() != null) {
            entity.setBrowser(request.getBrowser());
        }
        if (request.getDeviceType() != null) {
            entity.setDeviceType(request.getDeviceType());
        }
    }

    private TicketDto toDto(TicketEntity entity) {
        TicketDto dto = new TicketDto();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setCustomerEmail(entity.getCustomerEmail());
        dto.setCustomerName(entity.getCustomerName());
        dto.setSubject(entity.getSubject());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setPriority(entity.getPriority());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setResolvedAt(entity.getResolvedAt());
        dto.setAssignedTo(entity.getAssignedTo());
        dto.setTags(entity.getTags());

        MetadataDto metadata = new MetadataDto();
        metadata.setSource(entity.getSource());
        metadata.setBrowser(entity.getBrowser());
        metadata.setDeviceType(entity.getDeviceType());
        dto.setMetadata(metadata);
        return dto;
    }
}
