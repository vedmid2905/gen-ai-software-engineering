package banking.pipeline.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises /api/pipeline/* over real HTTP, pointed at an isolated temp
 * shared-dir and input file so it never touches the real project shared/.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void pipelineProperties(DynamicPropertyRegistry registry) throws IOException {
        Path inputFile = Files.createTempFile("pipeline-controller-test", ".json");
        Files.writeString(inputFile, """
                [
                  {
                    "transaction_id": "TXN100",
                    "timestamp": "2026-01-01T12:00:00Z",
                    "source_account": "ACC-90001",
                    "destination_account": "ACC-90002",
                    "amount": "100.00",
                    "currency": "USD",
                    "transaction_type": "transfer",
                    "description": "controller test fixture",
                    "metadata": { "channel": "online", "country": "US" }
                  }
                ]
                """);
        Path sharedDir = Files.createTempDirectory("pipeline-controller-test-shared");

        registry.add("pipeline.input-file", inputFile::toString);
        registry.add("pipeline.shared-dir", sharedDir::toString);
    }

    @Test
    @Order(1)
    void summaryReturns404BeforeAnyRun() throws Exception {
        mockMvc.perform(get("/api/pipeline/summary"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(2)
    void runProcessesFixtureTransactionAndPersistsResults() throws Exception {
        mockMvc.perform(post("/api/pipeline/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessed").value(1))
                .andExpect(jsonPath("$.approved").value(1))
                .andExpect(jsonPath("$.flagged").value(0))
                .andExpect(jsonPath("$.rejected").value(0));

        mockMvc.perform(get("/api/pipeline/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessed").value(1));

        mockMvc.perform(get("/api/pipeline/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value("TXN100"))
                .andExpect(jsonPath("$[0].status").value("approved"));
    }
}
