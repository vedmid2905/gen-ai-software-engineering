package banking.controller;

import banking.dto.AccountSummaryResponse;
import banking.service.BalanceService;
import banking.service.SummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP endpoints for the {@code /accounts} resource.
 *
 * Requirements: 7.1-7.6, 8.1-8.4, 10.2, 10.6
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final BalanceService balanceService;
    private final SummaryService summaryService;

    public AccountController(BalanceService balanceService, SummaryService summaryService) {
        this.balanceService = balanceService;
        this.summaryService = summaryService;
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<Object> getBalance(@PathVariable String accountId) {
        return ResponseEntity.ok(balanceService.getBalance(accountId));
    }

    @GetMapping("/{accountId}/summary")
    public ResponseEntity<AccountSummaryResponse> getSummary(@PathVariable String accountId) {
        return ResponseEntity.ok(summaryService.getSummary(accountId));
    }
}
