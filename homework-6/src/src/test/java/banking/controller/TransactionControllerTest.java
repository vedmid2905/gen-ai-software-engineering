package banking.controller;

import banking.dto.CreateTransactionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the transaction endpoints (and, through them, GlobalExceptionHandler)
 * over real HTTP via MockMvc.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTransaction_returnsCreatedTransaction() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "deposit", new BigDecimal("250.00"), "usd", null, "ACC-51001");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createTransaction_withInvalidAmount_returns400WithFieldErrors() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "deposit", new BigDecimal("-1.00"), "USD", null, "ACC-51002");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("amount"));
    }

    @Test
    void getTransaction_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/transactions/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("not found")));
    }

    @Test
    void getTransaction_withMalformedId_returns400() throws Exception {
        mockMvc.perform(get("/transactions/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listTransactions_filtersByType() throws Exception {
        createViaApi("transfer", "42.00", "ACC-51003", "ACC-51004");

        mockMvc.perform(get("/transactions").param("type", "transfer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type", everyItem(org.hamcrest.Matchers.is("transfer"))))
                .andExpect(jsonPath("$[?(@.fromAccount == 'ACC-51003')]").exists());
    }

    @Test
    void getBalance_returnsComputedBalanceForSingleCurrencyAccount() throws Exception {
        createViaApi("deposit", "500.00", null, "ACC-51005");
        createViaApi("withdrawal", "120.00", "ACC-51005", null);

        mockMvc.perform(get("/accounts/ACC-51005/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-51005"))
                .andExpect(jsonPath("$.balance").value(380.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void getBalance_forUnknownAccount_returns404() throws Exception {
        mockMvc.perform(get("/accounts/ACC-59999/balance"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSummary_aggregatesDepositsAndWithdrawals() throws Exception {
        createViaApi("deposit", "500.00", null, "ACC-51006");
        createViaApi("withdrawal", "50.00", "ACC-51006", null);

        mockMvc.perform(get("/accounts/ACC-51006/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeposits").value(500.00))
                .andExpect(jsonPath("$.totalWithdrawals").value(50.00))
                .andExpect(jsonPath("$.transactionCount").value(2));
    }

    private void createViaApi(String type, String amount, String fromAccount, String toAccount) throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                type, new BigDecimal(amount), "USD", fromAccount, toAccount);
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
