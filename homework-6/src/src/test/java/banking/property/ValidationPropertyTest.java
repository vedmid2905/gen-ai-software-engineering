package banking.property;

import banking.dto.CreateTransactionRequest;
import banking.dto.FieldError;
import banking.validation.Validator;
import net.jqwik.api.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;

import java.math.BigDecimal;
import java.util.List;

/**
 * Property-based tests for the Validator component.
 *
 * Validates: Requirements 2.1, 3.1
 */
class ValidationPropertyTest {

    private final Validator validator = new Validator();

    // -----------------------------------------------------------------------
    // Property 3: Non-positive amounts are always rejected
    // -----------------------------------------------------------------------

    /**
     * For any numeric value that is zero or negative,
     * {@link Validator#validateCreateTransaction} SHALL return a non-empty
     * error list with an entry for "amount".
     *
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    @Tag("Feature: banking-transactions-api, Property 3: Non-positive amounts are always rejected")
    void nonPositiveAmountsAreAlwaysRejected(
            @ForAll("nonPositiveAmounts") BigDecimal amount) {

        CreateTransactionRequest request = new CreateTransactionRequest(
                "deposit",   // valid type
                amount,      // invalid: zero or negative
                "USD",       // valid currency
                null,        // fromAccount — not required for deposit
                "ACC-AAAAA"  // valid toAccount
        );

        List<FieldError> errors = validator.validateCreateTransaction(request);

        // 1. The returned list must be non-empty
        Assertions.assertThat(errors).isNotEmpty();

        // 2. At least one FieldError must reference "amount"
        Assertions.assertThat(errors)
                .anyMatch(e -> "amount".equals(e.field()));
    }

    // -----------------------------------------------------------------------
    // Property 6: All validation errors are reported simultaneously
    // -----------------------------------------------------------------------

    /**
     * For any POST /transactions request where both {@code fromAccount} and
     * {@code toAccount} are present with invalid formats, the response SHALL
     * contain {@link FieldError} entries for BOTH fields in a single call
     * (no short-circuit validation).
     *
     * **Validates: Requirements 3.3**
     */
    @Property(tries = 100)
    @Tag("Feature: banking-transactions-api, Property 6: All validation errors are reported simultaneously")
    void allValidationErrorsAreReportedSimultaneously(
            @ForAll("invalidFromAccount") String fromAccount,
            @ForAll("invalidToAccount") String toAccount) {

        // Ensure the two invalid values are distinct so the test is meaningful
        net.jqwik.api.Assume.that(!fromAccount.equals(toAccount));

        CreateTransactionRequest request = new CreateTransactionRequest(
                "transfer",         // type that requires BOTH fromAccount and toAccount
                new BigDecimal("100.00"),  // valid amount
                "USD",              // valid currency
                fromAccount,        // invalid fromAccount
                toAccount           // invalid toAccount
        );

        List<FieldError> errors = validator.validateCreateTransaction(request);

        // 1. At least two errors (one per invalid field)
        Assertions.assertThat(errors.size()).isGreaterThanOrEqualTo(2);

        // 2. Must contain a FieldError for "fromAccount"
        Assertions.assertThat(errors)
                .anyMatch(e -> "fromAccount".equals(e.field()));

        // 3. Must contain a FieldError for "toAccount"
        Assertions.assertThat(errors)
                .anyMatch(e -> "toAccount".equals(e.field()));
    }

    // -----------------------------------------------------------------------
    // Generators
    // -----------------------------------------------------------------------

    /**
     * Generates BigDecimal values that are zero or negative.
     * Uses Arbitraries.bigDecimals() with an upper bound of 0.
     */
    @Provide("nonPositiveAmounts")
    Arbitrary<BigDecimal> nonPositiveAmounts() {
        return Arbitraries.bigDecimals()
                .lessOrEqual(BigDecimal.ZERO)
                .ofScale(2);
    }

    /**
     * Generates strings that do NOT match {@code ^ACC-[A-Z0-9]{5}$}.
     * Used as an invalid {@code fromAccount} value for Property 6.
     */
    @Provide("invalidFromAccount")
    Arbitrary<String> invalidFromAccount() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.matches("^ACC-[A-Z0-9]{5}$"));
    }

    /**
     * Generates strings that do NOT match {@code ^ACC-[A-Z0-9]{5}$}.
     * Used as an invalid {@code toAccount} value for Property 6.
     */
    @Provide("invalidToAccount")
    Arbitrary<String> invalidToAccount() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.matches("^ACC-[A-Z0-9]{5}$"));
    }
}
