package com.example.support.importer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link XmlImportParser}. Uses both inline XML strings and the
 * shared fixtures under {@code src/test/resources/fixtures}.
 */
class XmlImportParserTest {

    private final XmlImportParser parser = new XmlImportParser();

    @Test
    void mapsElementsAndReportsInvalidElements() {
        String xml = "<tickets><ticket><customer_email>ok@example.com</customer_email><customer_name>Jane</customer_name>"
                + "<subject>Login issue</subject><description>I need help with my account immediately</description>"
                + "<category>account_access</category><priority>high</priority><status>new</status>"
                + "<assigned_to>team</assigned_to><tags>login,bug</tags><source>web_form</source>"
                + "<browser>Chrome</browser><device_type>desktop</device_type></ticket>"
                + "<ticket><customer_email>bad@example.com</customer_email><customer_name>Bob</customer_name>"
                + "<subject>Bad</subject><description>short</description><category>unknown</category>"
                + "<priority>critical</priority><status>new</status></ticket></tickets>";

        List<ParseResult> results = parse(xml);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(0).ticket().getCustomerEmail()).isEqualTo("ok@example.com");
        assertThat(results.get(1).isSuccess()).isFalse();
        assertThat(results.get(1).error()).contains("Invalid value");
    }

    @Test
    void loadsValidFixtureFile() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/valid_tickets.xml")) {
            assertThat(in).isNotNull();
            List<ParseResult> results = parser.parse(in);

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(ParseResult::isSuccess);
            assertThat(results.get(1).ticket().getCategory().name()).isEqualTo("billing_question");
        }
    }

    @Test
    void malformedXmlReturnsSingleErrorResult() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/malformed_tickets.xml")) {
            assertThat(in).isNotNull();
            List<ParseResult> results = parser.parse(in);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).isSuccess()).isFalse();
            assertThat(results.get(0).error()).contains("XML parse error");
        }
    }

    @Test
    void missingOptionalTagsElementProducesEmptyTagList() {
        String xml = "<tickets><ticket><customer_email>ok@example.com</customer_email>"
                + "<subject>No tags here</subject><description>This ticket has no tags element at all</description>"
                + "<category>other</category><priority>low</priority><status>new</status></ticket></tickets>";

        List<ParseResult> results = parse(xml);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(0).ticket().getTags()).isEmpty();
    }

    @Test
    void doctypeDeclarationIsRejectedAsUnsuccessfulResultNotThrown() {
        String xxe = "<?xml version=\"1.0\"?>"
                + "<!DOCTYPE tickets [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<tickets><ticket><customer_email>a@example.com</customer_email>"
                + "<subject>XXE attempt</subject><description>Trying to read &xxe; via entity expansion</description>"
                + "<category>other</category><priority>low</priority><status>new</status></ticket></tickets>";

        List<ParseResult> results = parse(xxe);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccess()).isFalse();
        assertThat(results.get(0).error()).contains("XML parse error");
    }

    private List<ParseResult> parse(String xml) {
        return parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
