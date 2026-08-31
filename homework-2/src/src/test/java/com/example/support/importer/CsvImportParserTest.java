package com.example.support.importer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CsvImportParser}. Uses both inline CSV strings and the
 * shared fixtures under {@code src/test/resources/fixtures}.
 */
class CsvImportParserTest {

    private final CsvImportParser parser = new CsvImportParser();

    @Test
    void mapsValidRowsAndCapturesFieldMappingErrors() {
        String csv = String.join("\n",
                "customer_email,subject,description,category,priority,status,assigned_to,tags,source,browser,device_type",
                "user@example.com,Login issue,Need help with the account,account_access,urgent,new,team,login;bug,web_form,Chrome,desktop",
                "bad-email,Short,Needs more chars,unknown,critical,new,team,login,web_form,Chrome,desktop");

        List<ParseResult> results = parse(csv);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(0).ticket().getCustomerEmail()).isEqualTo("user@example.com");
        assertThat(results.get(0).ticket().getTags()).containsExactly("login", "bug");
        assertThat(results.get(1).isSuccess()).isFalse();
        assertThat(results.get(1).error()).contains("field mapping error");
    }

    @Test
    void loadsValidFixtureFile() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/valid_tickets.csv")) {
            assertThat(in).isNotNull();
            List<ParseResult> results = parser.parse(in);

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(ParseResult::isSuccess);
            assertThat(results.get(0).ticket().getCustomerEmail()).isEqualTo("user@example.com");
            assertThat(results.get(1).ticket().getCategory().name()).isEqualTo("billing_question");
        }
    }

    @Test
    void headerNamesAreCaseInsensitiveAndAcceptCamelCase() {
        String csv = String.join("\n",
                "CustomerEmail,Subject,Description",
                "camel@example.com,Camel case headers,This description is definitely long enough");

        List<ParseResult> results = parse(csv);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(0).ticket().getCustomerEmail()).isEqualTo("camel@example.com");
    }

    @Test
    void supportsSemicolonAndPipeSeparatedTags() {
        String csv = String.join("\n",
                "customer_email,subject,description,tags",
                "a@example.com,Subject one,Long enough description here,alpha;beta;gamma",
                "b@example.com,Subject two,Another long enough description,delta|epsilon");

        List<ParseResult> results = parse(csv);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).ticket().getTags()).containsExactly("alpha", "beta", "gamma");
        assertThat(results.get(1).ticket().getTags()).containsExactly("delta", "epsilon");
    }

    @Test
    void blankOptionalFieldsAreTreatedAsNull() {
        String csv = String.join("\n",
                "customer_email,customer_name,subject,description,assigned_to",
                "a@example.com,,Subject here,Long enough description text,");

        List<ParseResult> results = parse(csv);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(0).ticket().getCustomerName()).isNull();
        assertThat(results.get(0).ticket().getAssignedTo()).isNull();
    }

    @Test
    void unreadableStreamProducesSingleErrorResultInsteadOfThrowing() {
        // opencsv's CSVReaderHeaderAware can throw an unchecked exception
        // (not IOException) when the stream fails while resolving the header
        // row — verified empirically against opencsv 5.9. The parser must
        // degrade gracefully rather than let that propagate uncaught.
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated stream failure");
            }
        };

        List<ParseResult> results = parser.parse(broken);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccess()).isFalse();
        assertThat(results.get(0).error()).containsIgnoringCase("failed to read CSV file");
    }

    private List<ParseResult> parse(String csv) {
        return parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }
}
