package com.example.support.integration;

import com.example.support.repository.ClassificationLogRepository;
import com.example.support.repository.TicketRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests that exercise the full Spring context (real HTTP
 * layer, real controller advice, real H2-backed repositories) via
 * {@link TestRestTemplate}, as opposed to the mocked-controller unit tests in
 * {@code TicketControllerTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TicketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ClassificationLogRepository classificationLogRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        classificationLogRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private Map<String, Object> ticketPayload(String email, String name, String subject, String description) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customer_id", "cust-" + UUID.randomUUID());
        payload.put("customer_email", email);
        payload.put("customer_name", name);
        payload.put("subject", subject);
        payload.put("description", description);
        return payload;
    }

    private ResponseEntity<String> postJson(String path, Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);
        return restTemplate.postForEntity(url(path), entity, String.class);
    }

    private ResponseEntity<String> putJson(String path, Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);
        return restTemplate.exchange(url(path), HttpMethod.PUT, entity, String.class);
    }

    // =========================================================================
    // 1. Complete ticket lifecycle workflow
    // =========================================================================

    @Test
    void completeTicketLifecycleWorkflow() throws Exception {
        Map<String, Object> createPayload = ticketPayload(
                "lifecycle@example.com", "Lifecycle User", "Cannot log in",
                "I am unable to log in to my account since this morning.");

        ResponseEntity<String> createResponse = postJson("/tickets", createPayload);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = mapper.readTree(createResponse.getBody());
        String id = created.get("id").asText();
        assertThat(created.get("status").asText()).isEqualTo("new");

        ResponseEntity<String> getResponse = restTemplate.getForEntity(url("/tickets/" + id), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(getResponse.getBody()).get("subject").asText()).isEqualTo("Cannot log in");

        Map<String, Object> updatePayload = new LinkedHashMap<>();
        updatePayload.put("status", "resolved");
        ResponseEntity<String> updateResponse = putJson("/tickets/" + id, updatePayload);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode updated = mapper.readTree(updateResponse.getBody());
        assertThat(updated.get("status").asText()).isEqualTo("resolved");
        assertThat(updated.get("resolved_at").isNull()).isFalse();

        ResponseEntity<String> listResponse = restTemplate.getForEntity(url("/tickets"), String.class);
        JsonNode list = mapper.readTree(listResponse.getBody());
        assertThat(list.findValuesAsText("id")).contains(id);

        restTemplate.delete(url("/tickets/" + id));

        ResponseEntity<String> afterDelete = restTemplate.getForEntity(url("/tickets/" + id), String.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // =========================================================================
    // 2. Bulk import with auto-classification verification
    // =========================================================================

    @Test
    void bulkImportWithAutoClassifyAssignsCategoryPriorityAndLogsDecisions() throws Exception {
        String csv = String.join("\n",
                "customer_email,customer_name,subject,description",
                "acct@example.com,Acct User,Cannot login,I have a critical login error and cannot access my account",
                "bill@example.com,Bill User,Invoice question,I was charged for an invoice I do not recognize");

        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "tickets.csv";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> importResponse = restTemplate.postForEntity(
                url("/tickets/import?autoClassify=true"), request, String.class);

        assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode summary = mapper.readTree(importResponse.getBody());
        assertThat(summary.get("total_records").asInt()).isEqualTo(2);
        assertThat(summary.get("successful").asInt()).isEqualTo(2);
        assertThat(summary.get("failed").asInt()).isEqualTo(0);

        ResponseEntity<String> filtered = restTemplate.getForEntity(
                url("/tickets?category=account_access"), String.class);
        JsonNode accountAccessTickets = mapper.readTree(filtered.getBody());
        assertThat(accountAccessTickets).anyMatch(t -> t.get("customer_email").asText().equals("acct@example.com"));

        assertThat(classificationLogRepository.count()).isGreaterThanOrEqualTo(2);
    }

    // =========================================================================
    // 3. Concurrent operations (20+ simultaneous requests)
    // =========================================================================

    @Test
    void concurrentTicketCreation_twentyFiveSimultaneousRequestsAllSucceed() throws Exception {
        int requestCount = 25;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Callable<HttpStatusCode>> tasks = IntStream.range(0, requestCount)
                    .<Callable<HttpStatusCode>>mapToObj(i -> () -> {
                        Map<String, Object> payload = ticketPayload(
                                "concurrent" + i + "@example.com", "Concurrent User " + i,
                                "Concurrent ticket " + i,
                                "This ticket was created as part of a concurrency test run number " + i);
                        return postJson("/tickets", payload).getStatusCode();
                    })
                    .collect(Collectors.toList());

            List<Future<HttpStatusCode>> futures = executor.invokeAll(tasks, 30, TimeUnit.SECONDS);
            List<HttpStatusCode> statuses = new ArrayList<>();
            for (Future<HttpStatusCode> future : futures) {
                statuses.add(future.get());
            }

            assertThat(statuses).hasSize(requestCount);
            assertThat(statuses).allMatch(status -> status == HttpStatus.CREATED);
        } finally {
            executor.shutdown();
        }

        ResponseEntity<String> listResponse = restTemplate.getForEntity(url("/tickets"), String.class);
        JsonNode list = mapper.readTree(listResponse.getBody());
        long concurrentTicketCount = 0;
        for (JsonNode node : list) {
            if (node.get("customer_email").asText().startsWith("concurrent")) {
                concurrentTicketCount++;
            }
        }
        assertThat(concurrentTicketCount).isEqualTo(requestCount);
    }

    // =========================================================================
    // 4. Combined filtering by category and priority
    // =========================================================================

    @Test
    void combinedCategoryAndPriorityFilterReturnsOnlyMatchingTickets() throws Exception {
        createTicketWithClassification("match@example.com", "billing_question", "high");
        createTicketWithClassification("wrong-priority@example.com", "billing_question", "low");
        createTicketWithClassification("wrong-category@example.com", "technical_issue", "high");
        createTicketWithClassification("match2@example.com", "billing_question", "high");

        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/tickets?category=billing_question&priority=high"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode results = mapper.readTree(response.getBody());
        List<String> emails = new ArrayList<>();
        results.forEach(t -> emails.add(t.get("customer_email").asText()));

        assertThat(emails).containsExactlyInAnyOrder("match@example.com", "match2@example.com");
    }

    private void createTicketWithClassification(String email, String category, String priority) throws Exception {
        Map<String, Object> payload = ticketPayload(email, "Filter User", "Filter test ticket",
                "This ticket is used to verify combined category and priority filtering behaviour.");
        payload.put("category", category);
        payload.put("priority", priority);
        ResponseEntity<String> response = postJson("/tickets", payload);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // =========================================================================
    // 5. Malformed / empty import file → 400 (end-to-end regression for the
    //    500→400 fix in GlobalExceptionHandler)
    // =========================================================================

    @Test
    void malformedImportFile_endToEnd_returns400BadRequest() {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(new byte[0]) {
            @Override
            public String getFilename() {
                return "empty.csv";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url("/tickets/import"), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 6. Auto-classify endpoint persists classification and writes an audit log
    // =========================================================================

    @Test
    void autoClassifyEndpointPersistsClassificationAndAuditLog() throws Exception {
        Map<String, Object> payload = ticketPayload("classify@example.com", "Classify User",
                "Production down", "This is a critical production down incident affecting all customers.");
        ResponseEntity<String> createResponse = postJson("/tickets", payload);
        String id = mapper.readTree(createResponse.getBody()).get("id").asText();

        long logCountBefore = classificationLogRepository.count();

        ResponseEntity<String> classifyResponse = restTemplate.postForEntity(
                url("/tickets/" + id + "/auto-classify"), null, String.class);

        assertThat(classifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode result = mapper.readTree(classifyResponse.getBody());
        assertThat(result.get("priority").asText()).isEqualTo("urgent");
        assertThat(result.get("confidence_score").asDouble()).isGreaterThan(0.0);

        ResponseEntity<String> getResponse = restTemplate.getForEntity(url("/tickets/" + id), String.class);
        assertThat(mapper.readTree(getResponse.getBody()).get("priority").asText()).isEqualTo("urgent");

        assertThat(classificationLogRepository.count()).isEqualTo(logCountBefore + 1);
    }
}
