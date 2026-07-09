package com.example.support.importer;

import com.example.support.exception.UnsupportedImportFormatException;
import org.springframework.stereotype.Component;

@Component
public class ImportParserFactory {

    private final CsvImportParser csvParser;
    private final JsonImportParser jsonParser;
    private final XmlImportParser xmlParser;

    public ImportParserFactory(CsvImportParser csvParser,
                               JsonImportParser jsonParser,
                               XmlImportParser xmlParser) {
        this.csvParser = csvParser;
        this.jsonParser = jsonParser;
        this.xmlParser = xmlParser;
    }

    public ImportParser forContentType(String contentType, String fileName) {
        if (contentType != null) {
            String ct = contentType.toLowerCase();
            if (ct.contains("text/csv") || ct.contains("application/csv")) {
                return csvParser;
            }
            if (ct.contains("application/json") || ct.contains("text/json")) {
                return jsonParser;
            }
            if (ct.contains("application/xml") || ct.contains("text/xml")) {
                return xmlParser;
            }
        }
        if (fileName != null) {
            String name = fileName.toLowerCase();
            if (name.endsWith(".csv")) return csvParser;
            if (name.endsWith(".json")) return jsonParser;
            if (name.endsWith(".xml")) return xmlParser;
        }
        String type = contentType != null ? contentType : (fileName != null ? fileName : "unknown");
        throw new UnsupportedImportFormatException(type);
    }
}
