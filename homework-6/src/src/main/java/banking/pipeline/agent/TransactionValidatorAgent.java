package banking.pipeline.agent;

import banking.pipeline.domain.AuditInfo;
import banking.pipeline.domain.ProcessingResult;
import banking.pipeline.domain.TransactionRecord;
import banking.pipeline.validation.IsoCurrencyCodes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates required fields, positive amounts, ISO 4217 currency codes, and
 * account reference format before a transaction is allowed further into the pipeline.
 */
public class TransactionValidatorAgent {

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^ACC-[A-Za-z0-9]{3,}$");
    static final String AGENT_NAME = "transaction_validator";

    public ProcessingResult validateTransaction(TransactionRecord transaction) {
        List<String> errors = new ArrayList<>();

        if (isBlank(transaction.transactionId())) {
            errors.add("transaction_id is required");
        }
        if (transaction.timestamp() == null) {
            errors.add("timestamp is required");
        }
        if (!isValidAccount(transaction.sourceAccount())) {
            errors.add("source_account is missing or malformed");
        }
        if (!isValidAccount(transaction.destinationAccount())) {
            errors.add("destination_account is missing or malformed");
        }
        if (transaction.amount() == null) {
            errors.add("amount is required");
        } else if (transaction.amount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("amount must be greater than zero");
        }
        if (!IsoCurrencyCodes.isSupported(transaction.currency())) {
            errors.add("currency '" + transaction.currency() + "' is not a supported ISO 4217 code");
        }

        AuditInfo audit = new AuditInfo(Instant.now(), AGENT_NAME);

        if (errors.isEmpty()) {
            return new ProcessingResult(transaction.transactionId(), "validated", "valid", null, null, audit);
        }
        return new ProcessingResult(transaction.transactionId(), "rejected", "invalid", null,
                String.join("; ", errors), audit);
    }

    private boolean isValidAccount(String account) {
        return account != null && ACCOUNT_PATTERN.matcher(account).matches();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
