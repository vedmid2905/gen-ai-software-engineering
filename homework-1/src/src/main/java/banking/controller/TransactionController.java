package banking.controller;

import banking.dto.CreateTransactionRequest;
import banking.dto.TransactionResponse;
import banking.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HTTP endpoints for the {@code /transactions} resource.
 *
 * Requirements: 1.1, 5.1, 5.2, 5.3, 6.1-6.7, 10.1, 10.2, 10.6
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> listTransactions(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(transactionService.listTransactions(accountId, type, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }
}
