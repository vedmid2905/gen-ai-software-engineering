package com.example.support.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportParserFactoryTest {

    private final ImportParserFactory factory = new ImportParserFactory(
            new CsvImportParser(), new JsonImportParser(new ObjectMapper()), new XmlImportParser());

    @Test
    void selectsParserByContentType() {
        assertThat(factory.forContentType("text/csv", "tickets.csv")).isInstanceOf(CsvImportParser.class);
        assertThat(factory.forContentType("application/json", "tickets.json")).isInstanceOf(JsonImportParser.class);
        assertThat(factory.forContentType("application/xml", "tickets.xml")).isInstanceOf(XmlImportParser.class);
    }

    @Test
    void fallsBackToFileExtensionWhenContentTypeIsNull() {
        assertThat(factory.forContentType(null, "tickets.csv")).isInstanceOf(CsvImportParser.class);
        assertThat(factory.forContentType(null, "tickets.json")).isInstanceOf(JsonImportParser.class);
        assertThat(factory.forContentType(null, "tickets.xml")).isInstanceOf(XmlImportParser.class);
    }

    @Test
    void throwsUnsupportedImportFormatForUnknownType() {
        assertThatThrownBy(() -> factory.forContentType(null, "tickets.unknown"))
                .isInstanceOf(com.example.support.exception.UnsupportedImportFormatException.class);
    }
}
