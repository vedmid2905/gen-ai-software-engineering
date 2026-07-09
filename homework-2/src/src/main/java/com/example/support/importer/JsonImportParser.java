package com.example.support.importer;

import com.example.support.dto.CreateTicketRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses JSON import files into {@link ParseResult} instances.
 *
 * <p>The file must contain a top-level JSON array of ticket objects. If the
 * entire document is syntactically malformed, a single {@link ParseResult}
 * carrying the parse error location is returned. If the document is a valid
 * array but an individual element cannot be mapped, that element produces its
 * own error {@link ParseResult} and processing continues for the remaining
 * elements.
 *
 * <p>Implements requirements 8.1 and 8.2.
 */
@Component
public class JsonImportParser implements ImportParser {

    private static final Logger log = LoggerFactory.getLogger(JsonImportParser.class);

    private final ObjectMapper objectMapper;

    public JsonImportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ParseResult> parse(InputStream inputStream) {
        List<ParseResult> results = new ArrayList<>();

        // Step 1: parse the entire JSON document as a raw JSON array node.
        // If the document is malformed we cannot recover per-element, so we
        // return a single error result that includes the parse error location
        // (requirement 8.2).
        List<Object> rawElements;
        try {
            rawElements = objectMapper.readValue(inputStream,
                    new TypeReference<List<Object>>() {});
        } catch (JsonProcessingException e) {
            String location = buildLocation(e);
            String message = "JSON parse error" + location + ": " + e.getOriginalMessage();
            log.warn(message, e);
            results.add(new ParseResult(null, 0, message));
            return results;
        } catch (IOException e) {
            String message = "Failed to read JSON input stream: " + e.getMessage();
            log.error(message, e);
            results.add(new ParseResult(null, 0, message));
            return results;
        }

        // Step 2: convert each element individually so a bad element doesn't
        // prevent the remaining elements from being imported (requirement 8.1).
        for (int i = 0; i < rawElements.size(); i++) {
            try {
                // Re-convert the already-parsed Object (a Map) to the target type.
                // This avoids re-parsing raw JSON and catches field-mapping errors
                // (e.g. invalid enum values) per element.
                CreateTicketRequest ticket =
                        objectMapper.convertValue(rawElements.get(i), CreateTicketRequest.class);
                results.add(new ParseResult(ticket, i, null));
            } catch (IllegalArgumentException e) {
                // ObjectMapper.convertValue wraps conversion errors in
                // IllegalArgumentException (backed by a JsonMappingException).
                String message = "Element " + i + ": mapping error — " + rootMessage(e);
                log.warn(message, e);
                results.add(new ParseResult(null, i, message));
            }
        }

        return results;
    }

    /**
     * Extracts a human-readable location string from a {@link JsonProcessingException},
     * e.g. {@code " at line 3, column 7"}.
     */
    private static String buildLocation(JsonProcessingException e) {
        if (e.getLocation() == null) {
            return "";
        }
        return " at line " + e.getLocation().getLineNr()
                + ", column " + e.getLocation().getColumnNr();
    }

    /**
     * Walks the exception cause chain to find the most descriptive message,
     * avoiding the verbose Jackson wrapper messages when a root cause exists.
     */
    private static String rootMessage(Throwable t) {
        Throwable cause = t.getCause();
        if (cause != null && cause.getMessage() != null) {
            return cause.getMessage();
        }
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}
