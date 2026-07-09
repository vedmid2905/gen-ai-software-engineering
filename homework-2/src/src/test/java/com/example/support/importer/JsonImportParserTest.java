package com.example.support.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonImportParser}. Uses both inline JSON strings and the
 * shared fixtures under {@code src/test/resources/fixtures}.
 */
class JsonImportParserTest {

    private final JsonImportParser parser = new JsonImportParser(new ObjectMapper());

    @Test
    void returnsPerElementResultsForMixOfValidAndInvalid() {
        String json = "[{\"customer_email\":\"ok@example.com\",\"customer_name\":\"Jane\",\"subject\":\"Login issue\","
                + "\"description\":\"I need help with my account immediately\",\"category\":\"account_access\","
                + "\"priority\":\"high\",\"status\":\"new\",\"assigned_to\":\"team\",\"tags\":[\"login\"],"
                + "\"source\":\"web_form\",\"browser\":\"Chrome\",\"device_type\":\"desktop\"},"
                + "{\"customer_email\":\"bad@example.com\",\"customer_name\":\"Bob\",\"subject\":\"Bad\","
                + "\"description\":\"short\",\"category\":\"unknown\",\"priority\":\"critical\",\"status\":\"new\"}]";

        List<ParseResult> results = parse(json);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(0).ticket().getCustomerEmail()).isEqualTo("ok@example.com");
        assertThat(results.get(1).isSuccess()).isFalse();
    }

    @Test
    void loadsValidFixtureFile() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/valid_tickets.json")) {
            assertThat(in).isNotNull();
            List<ParseResult> results = parser.parse(in);

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(ParseResult::isSuccess);
            assertThat(results.get(1).ticket().getCategory().name()).isEqualTo("feature_request");
        }
    }

    @Test
    void malformedJsonDocumentReturnsSingleErrorWithLocation() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/malformed_tickets.json")) {
            assertThat(in).isNotNull();
            List<ParseResult> results = parser.parse(in);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).isSuccess()).isFalse();
            assertThat(results.get(0).error()).contains("JSON parse error");
        }
    }

    @Test
    void emptyArrayReturnsEmptyResultList() {
        List<ParseResult> results = parse("[]");

        assertThat(results).isEmpty();
    }

    @Test
    void unreadableStreamProducesSingleErrorResult() {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated stream failure");
            }
        };

        List<ParseResult> results = parser.parse(broken);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccess()).isFalse();
        assertThat(results.get(0).error()).contains("Failed to read JSON input stream");
    }

    private List<ParseResult> parse(String json) {
        return parser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }
}
