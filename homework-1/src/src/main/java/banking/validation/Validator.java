package banking.validation;

import banking.dto.CreateTransactionRequest;
import banking.dto.FieldError;
import banking.exception.InvalidIdFormatException;
import banking.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stateless validation component responsible for all input validation.
 *
 * <p>For {@link #validateCreateTransaction}, errors are collected across all
 * fields before returning so callers receive a complete list in a single 400
 * response (no short-circuit on first failure).
 *
 * <p>Helper methods ({@link #validateAccountId}, {@link #validateTransactionId},
 * {@link #validateListFilters}) throw immediately on the first failure, since
 * they validate a single conceptual value.
 *
 * Requirements: 1.2–1.7, 2.1–2.5, 3.1–3.3, 4.1–4.4, 5.3, 6.6, 6.7, 7.3, 8.3
 */
@Component
public class Validator {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^ACC-[A-Z0-9]{5}$");

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final Set<String> VALID_TYPES = Set.of("deposit", "withdrawal", "transfer");

    /** Supported ISO 4217 currency codes (always stored and compared in uppercase). */
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY",
            "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "RON", "BGN",
            "HRK", "INR", "BRL", "MXN", "SGD", "HKD", "NZD", "ZAR",
            "RUB", "TRY", "KRW"
    );

    // -----------------------------------------------------------------------
    // validateCreateTransaction
    // -----------------------------------------------------------------------

    /**
     * Validate a {@link CreateTransactionRequest} and collect ALL field errors.
     *
     * <p>Returns an empty list when the request is fully valid. Never throws —
     * the caller is responsible for inspecting the returned list and raising
     * {@link ValidationException} when it is non-empty.
     *
     * @param request the incoming DTO; must not be {@code null}
     * @return list of {@link FieldError} records (empty ⟹ valid)
     * Requirements: 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 2.1, 2.2, 2.3, 2.4, 2.5,
     *               3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4
     */
    public List<FieldError> validateCreateTransaction(CreateTransactionRequest request) {
        List<FieldError> errors = new ArrayList<>();

        // ----- type -----
        String type = request.type();
        if (type == null || type.isBlank()) {
            errors.add(new FieldError("type", "Type is required"));
        } else if (!VALID_TYPES.contains(type)) {
            errors.add(new FieldError("type",
                    "Type must be one of: deposit, withdrawal, transfer"));
        }

        // ----- amount -----
        BigDecimal amount = request.amount();
        if (amount == null) {
            errors.add(new FieldError("amount", "Amount is required"));
        } else {
            if (amount.compareTo(ZERO) <= 0) {
                errors.add(new FieldError("amount", "Amount must be a positive number"));
            } else {
                // scale check (strip trailing zeros first to handle 1.10 → scale 1)
                if (amount.stripTrailingZeros().scale() > 2) {
                    errors.add(new FieldError("amount",
                            "Amount must have at most 2 decimal places"));
                }
                if (amount.compareTo(MAX_AMOUNT) > 0) {
                    errors.add(new FieldError("amount",
                            "Amount must not exceed 999,999,999.99"));
                }
            }
        }

        // ----- currency -----
        String currency = request.currency();
        if (currency == null || currency.isBlank()) {
            errors.add(new FieldError("currency", "Currency is required"));
        } else if (!SUPPORTED_CURRENCIES.contains(currency.toUpperCase())) {
            errors.add(new FieldError("currency",
                    "Currency code '" + currency + "' is not supported"));
        }

        // ----- account fields — only validate if type is known -----
        if (type != null && !type.isBlank() && VALID_TYPES.contains(type)) {
            String fromAccount = request.fromAccount();
            String toAccount   = request.toAccount();

            switch (type) {
                case "deposit" -> {
                    if (toAccount == null || toAccount.isBlank()) {
                        errors.add(new FieldError("toAccount",
                                "toAccount is required for deposit transactions"));
                    } else if (!ACCOUNT_PATTERN.matcher(toAccount).matches()) {
                        errors.add(new FieldError("toAccount",
                                "toAccount must match pattern ACC-[A-Z0-9]{5}"));
                    }
                }
                case "withdrawal" -> {
                    if (fromAccount == null || fromAccount.isBlank()) {
                        errors.add(new FieldError("fromAccount",
                                "fromAccount is required for withdrawal transactions"));
                    } else if (!ACCOUNT_PATTERN.matcher(fromAccount).matches()) {
                        errors.add(new FieldError("fromAccount",
                                "fromAccount must match pattern ACC-[A-Z0-9]{5}"));
                    }
                }
                case "transfer" -> {
                    if (fromAccount == null || fromAccount.isBlank()) {
                        errors.add(new FieldError("fromAccount",
                                "fromAccount is required for transfer transactions"));
                    } else if (!ACCOUNT_PATTERN.matcher(fromAccount).matches()) {
                        errors.add(new FieldError("fromAccount",
                                "fromAccount must match pattern ACC-[A-Z0-9]{5}"));
                    }

                    if (toAccount == null || toAccount.isBlank()) {
                        errors.add(new FieldError("toAccount",
                                "toAccount is required for transfer transactions"));
                    } else if (!ACCOUNT_PATTERN.matcher(toAccount).matches()) {
                        errors.add(new FieldError("toAccount",
                                "toAccount must match pattern ACC-[A-Z0-9]{5}"));
                    }
                }
            }
        }

        return errors;
    }

    // -----------------------------------------------------------------------
    // validateAccountId
    // -----------------------------------------------------------------------

    /**
     * Validate an account-ID path parameter.
     *
     * <p>Throws {@link InvalidIdFormatException} immediately if {@code accountId}
     * is {@code null}, blank, or does not match {@code ACC-[A-Z0-9]{5}}.
     *
     * @param accountId the path parameter to validate
     * @throws InvalidIdFormatException if the format is invalid
     * Requirements: 5.3, 7.3, 8.3
     */
    public void validateAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()
                || !ACCOUNT_PATTERN.matcher(accountId).matches()) {
            throw new InvalidIdFormatException("Invalid account ID format",
                    "Account ID must match pattern ACC-[A-Z0-9]{5}");
        }
    }

    // -----------------------------------------------------------------------
    // validateTransactionId
    // -----------------------------------------------------------------------

    /**
     * Validate a transaction-ID path parameter.
     *
     * <p>Throws {@link InvalidIdFormatException} immediately if {@code id} is
     * {@code null}, empty/blank, or not a valid UUID string.
     *
     * @param id the path parameter to validate
     * @throws InvalidIdFormatException if the format is invalid
     * Requirements: 5.3
     */
    public void validateTransactionId(String id) {
        if (id == null || id.isBlank()) {
            throw new InvalidIdFormatException("Invalid transaction ID format",
                    "Transaction ID must be a valid UUID");
        }
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new InvalidIdFormatException("Invalid transaction ID format",
                    "Transaction ID must be a valid UUID");
        }
    }

    // -----------------------------------------------------------------------
    // validateListFilters
    // -----------------------------------------------------------------------

    /**
     * Validate the query parameters for {@code GET /transactions}.
     *
     * <p>Rules (each check throws {@link ValidationException} on failure):
     * <ol>
     *   <li>If {@code type} is non-null and not one of {@code deposit},
     *       {@code withdrawal}, {@code transfer} → field error for "type".</li>
     *   <li>If {@code from} is non-null and cannot be parsed as an ISO 8601
     *       date ({@code LocalDate}) or datetime ({@code Instant}) → field
     *       error for "from".</li>
     *   <li>If {@code to} is non-null and cannot be parsed as an ISO 8601
     *       date ({@code LocalDate}) or datetime ({@code Instant}) → field
     *       error for "to".</li>
     *   <li>If both {@code from} and {@code to} are provided and parseable
     *       but {@code from} is chronologically after {@code to} → field
     *       error for "from".</li>
     * </ol>
     *
     * @param type the optional type filter value
     * @param from the optional start-date filter value (ISO 8601)
     * @param to   the optional end-date filter value (ISO 8601)
     * @throws ValidationException if any of the above rules are violated
     * Requirements: 6.6, 6.7
     */
    public void validateListFilters(String type, String from, String to) {
        // --- type ---
        if (type != null && !VALID_TYPES.contains(type)) {
            throw new ValidationException(List.of(
                    new FieldError("type",
                            "type must be one of: deposit, withdrawal, transfer")));
        }

        // --- from ---
        Instant fromInstant = null;
        if (from != null) {
            fromInstant = parseIso8601(from);
            if (fromInstant == null) {
                throw new ValidationException(List.of(
                        new FieldError("from",
                                "from must be a valid ISO 8601 date or datetime")));
            }
        }

        // --- to ---
        Instant toInstant = null;
        if (to != null) {
            toInstant = parseIso8601(to);
            if (toInstant == null) {
                throw new ValidationException(List.of(
                        new FieldError("to",
                                "to must be a valid ISO 8601 date or datetime")));
            }
        }

        // --- from after to ---
        if (fromInstant != null && toInstant != null && fromInstant.isAfter(toInstant)) {
            throw new ValidationException(List.of(
                    new FieldError("from",
                            "from must not be after to")));
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Try to parse {@code value} as an ISO 8601 date ({@code LocalDate}) or
     * datetime ({@code Instant}).  Returns the corresponding {@link Instant}
     * (midnight UTC for date-only values) or {@code null} if neither format
     * matches.
     */
    private Instant parseIso8601(String value) {
        // Try full datetime first (e.g. "2024-01-31T10:00:00Z")
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        // Try date-only (e.g. "2024-01-31") — treat as start of day UTC
        try {
            return LocalDate.parse(value).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        return null;
    }
}
