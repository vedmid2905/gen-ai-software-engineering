package banking.service;

import banking.dto.AccountSummaryResponse;
import banking.dto.BalanceResponse;
import banking.dto.CreateTransactionRequest;
import banking.dto.MultiCurrencyBalanceResponse;
import banking.dto.TransactionResponse;
import banking.exception.InvalidIdFormatException;
import banking.exception.NotFoundException;
import banking.exception.ValidationException;
import banking.store.TransactionStore;
import banking.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Account IDs in these tests must match the Validator's ACC-[A-Z0-9]{5} pattern
 * (exactly 5 characters after the hyphen).
 */
class TransactionServiceTest {

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(new TransactionStore(), new Validator());
    }

    @Test
    void createTransactionPersistsAndReturnsResponse() {
        TransactionResponse response = service.createTransaction(
                new CreateTransactionRequest("transfer", new BigDecimal("100.00"), "usd", "ACC-10001", "ACC-20001"));

        assertThat(response.id()).isNotBlank();
        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(service.getTransaction(response.id())).isEqualTo(response);
    }

    @Test
    void createTransactionWithInvalidAmountThrowsValidationException() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "deposit", new BigDecimal("-5.00"), "USD", null, "ACC-20001");

        assertThatThrownBy(() -> service.createTransaction(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void listTransactionsFiltersByType() {
        service.createTransaction(new CreateTransactionRequest("deposit", new BigDecimal("50.00"), "USD", null, "ACC-20001"));
        service.createTransaction(new CreateTransactionRequest("transfer", new BigDecimal("100.00"), "USD", "ACC-10001", "ACC-20001"));

        List<TransactionResponse> deposits = service.listTransactions("deposit", null, null);

        assertThat(deposits).hasSize(1);
        assertThat(deposits.get(0).type()).isEqualTo("deposit");
    }

    @Test
    void getTransactionWithUnknownIdThrowsNotFound() {
        String randomUuid = "123e4567-e89b-12d3-a456-426614174000";
        assertThatThrownBy(() -> service.getTransaction(randomUuid))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getTransactionWithMalformedIdThrowsInvalidIdFormat() {
        assertThatThrownBy(() -> service.getTransaction("not-a-uuid"))
                .isInstanceOf(InvalidIdFormatException.class);
    }

    @Test
    void getBalanceWithSingleCurrencyReturnsBalanceResponse() {
        service.createTransaction(new CreateTransactionRequest("deposit", new BigDecimal("500.00"), "USD", null, "ACC-20001"));
        service.createTransaction(new CreateTransactionRequest("withdrawal", new BigDecimal("120.00"), "USD", "ACC-20001", null));

        Object balance = service.getBalance("ACC-20001");

        assertThat(balance).isInstanceOf(BalanceResponse.class);
        BalanceResponse response = (BalanceResponse) balance;
        assertThat(response.balance()).isEqualByComparingTo("380.00");
        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void getBalanceWithMultipleCurrenciesReturnsMultiCurrencyResponse() {
        service.createTransaction(new CreateTransactionRequest("deposit", new BigDecimal("500.00"), "USD", null, "ACC-30001"));
        service.createTransaction(new CreateTransactionRequest("deposit", new BigDecimal("200.00"), "EUR", null, "ACC-30001"));

        Object balance = service.getBalance("ACC-30001");

        assertThat(balance).isInstanceOf(MultiCurrencyBalanceResponse.class);
        assertThat(((MultiCurrencyBalanceResponse) balance).balances()).hasSize(2);
    }

    @Test
    void getBalanceForUnknownAccountThrowsNotFound() {
        assertThatThrownBy(() -> service.getBalance("ACC-99999"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getSummaryAggregatesDepositsWithdrawalsAndCount() {
        service.createTransaction(new CreateTransactionRequest("deposit", new BigDecimal("500.00"), "USD", null, "ACC-40001"));
        service.createTransaction(new CreateTransactionRequest("withdrawal", new BigDecimal("50.00"), "USD", "ACC-40001", null));

        AccountSummaryResponse summary = service.getSummary("ACC-40001");

        assertThat(summary.totalDeposits()).isEqualByComparingTo("500.00");
        assertThat(summary.totalWithdrawals()).isEqualByComparingTo("50.00");
        assertThat(summary.transactionCount()).isEqualTo(2);
        assertThat(summary.mostRecentTransactionDate()).isNotBlank();
    }
}
