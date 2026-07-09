package banking.pipeline.validation;

import java.util.Set;

/** Supported ISO 4217 currency codes for the pipeline. */
public final class IsoCurrencyCodes {

    public static final Set<String> SUPPORTED = Set.of(
            "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY",
            "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "RON", "BGN",
            "INR", "BRL", "MXN", "SGD", "HKD", "NZD", "ZAR", "KRW"
    );

    private IsoCurrencyCodes() {
    }

    public static boolean isSupported(String code) {
        return code != null && SUPPORTED.contains(code.toUpperCase());
    }
}
