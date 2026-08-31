package com.example.support.controller;

import com.example.support.dto.*;
import com.example.support.model.Category;
import com.example.support.model.Priority;
import com.example.support.model.Status;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final com.example.support.service.TicketService ticketService;

    public TicketController(com.example.support.service.TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketDto> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @RequestParam(value = "autoClassify", required = false, defaultValue = "false") boolean autoClassify) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request, autoClassify));
    }

    @GetMapping
    public List<TicketDto> getTickets(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Status status) {
        TicketFilter filter = new TicketFilter();
        filter.setCategory(category);
        filter.setPriority(priority);
        filter.setStatus(status);
        return ticketService.getTickets(filter);
    }

    @GetMapping("/{id}")
    public TicketDto getTicketById(@PathVariable UUID id) {
        return ticketService.getTicketById(id);
    }

    @PutMapping("/{id}")
    public TicketDto updateTicket(@PathVariable UUID id, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.updateTicket(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable UUID id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public BulkImportSummary importTickets(
            @RequestParam MultipartFile file,
            @RequestParam(value = "autoClassify", required = false, defaultValue = "false") boolean autoClassify) {
        return ticketService.importTickets(file, autoClassify);
    }

    @PostMapping("/{id}/auto-classify")
    public ClassificationResult classifyTicket(@PathVariable UUID id) {
        return ticketService.classifyTicket(id);
    }
}
