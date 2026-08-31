package banking.controller;

import banking.dto.AccountSummaryResponse;
import banking.dto.CreateTransactionRequest;
import banking.dto.TransactionResponse;
import banking.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> create(@RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return transactionService.listTransactions(type, from, to);
    }

    @GetMapping("/transactions/{id}")
    public TransactionResponse get(@PathVariable String id) {
        return transactionService.getTransaction(id);
    }

    @GetMapping("/accounts/{accountId}/balance")
    public Object balance(@PathVariable String accountId) {
        return transactionService.getBalance(accountId);
    }

    @GetMapping("/accounts/{accountId}/summary")
    public AccountSummaryResponse summary(@PathVariable String accountId) {
        return transactionService.getSummary(accountId);
    }
}
