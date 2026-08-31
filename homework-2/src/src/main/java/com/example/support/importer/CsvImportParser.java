package com.example.support.importer;

import com.example.support.dto.CreateTicketRequest;
import com.example.support.model.*;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Parses CSV import files into {@link ParseResult} instances.
 *
 * <p>The first row is treated as a header row. Column names are matched
 * case-insensitively to {@link CreateTicketRequest} fields using both
 * snake_case and camelCase variants. Per-row errors are caught individually
 * and stored in the {@link ParseResult}; processing continues for remaining
 * rows.
 */
@Component
public class CsvImportParser implements ImportParser {

    private static final Logger log = LoggerFactory.getLogger(CsvImportParser.class);

    @Override
    public List<ParseResult> parse(InputStream inputStream) {
        List<ParseResult> results = new ArrayList<>();

        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            Map<String, String> row;
            int rowIndex = 0; // 0-based index of data rows (after header)

            while (true) {
                try {
                    row = reader.readMap();
                    if (row == null) {
                        break; // EOF
                    }
                    CreateTicketRequest ticket = mapRowToRequest(row);
                    results.add(new ParseResult(ticket, rowIndex, null));
                } catch (CsvException e) {
                    String message = "Row " + rowIndex + ": CSV parse error — " + e.getMessage();
                    log.warn(message, e);
                    results.add(new ParseResult(null, rowIndex, message));
                } catch (IllegalArgumentException e) {
                    String message = "Row " + rowIndex + ": field mapping error — " + e.getMessage();
                    log.warn(message, e);
                    results.add(new ParseResult(null, rowIndex, message));
                } catch (RuntimeException e) {
                    // opencsv's CSVReaderHeaderAware can throw an unchecked
                    // exception (rather than IOException) when the underlying
                    // stream fails while it is still resolving the header row.
                    // The reader's internal state is unreliable after this, so
                    // stop processing rather than risk repeated failures.
                    String message = "Row " + rowIndex + ": failed to read CSV file — " + e.getMessage();
                    log.error(message, e);
                    results.add(new ParseResult(null, rowIndex, message));
                    break;
                }
                rowIndex++;
            }

        } catch (IOException e) {
            // Unrecoverable — wrap entire stream failure as a single error result
            log.error("Failed to read CSV input stream", e);
            results.add(new ParseResult(null, 0, "Failed to read CSV file: " + e.getMessage()));
        } catch (RuntimeException e) {
            // CSVReaderHeaderAware's constructor reads the header row eagerly
            // and can throw an unchecked exception (not IOException) if the
            // stream fails before the header is resolved.
            log.error("Failed to read CSV input stream", e);
            results.add(new ParseResult(null, 0, "Failed to read CSV file: " + e.getMessage()));
        }

        return results;
    }

    /**
     * Maps a header-keyed CSV row to a {@link CreateTicketRequest}.
     *
     * <p>Column names are normalised to lowercase with underscores removed so
     * that both {@code customer_email} and {@code customerEmail} resolve to the
     * same field.
     */
    private CreateTicketRequest mapRowToRequest(Map<String, String> row) {
        CreateTicketRequest req = new CreateTicketRequest();

        for (Map.Entry<String, String> entry : row.entrySet()) {
            String key = normalise(entry.getKey());
            String value = entry.getValue() == null ? null : entry.getValue().trim();
            if (value != null && value.isEmpty()) {
                value = null;
            }
            applyField(req, key, value);
        }

        return req;
    }

    /** Converts a header name to a comparable canonical form (lowercase, no underscores). */
    private static String normalise(String header) {
        if (header == null) return "";
        return header.toLowerCase().replace("_", "").replace("-", "").trim();
    }

    /**
     * Applies a single CSV column value to the appropriate field on the request object.
     *
     * @throws IllegalArgumentException if an enum value is unrecognised
     */
    private static void applyField(CreateTicketRequest req, String normalisedKey, String value) {
        switch (normalisedKey) {
            case "customerid":
                req.setCustomerId(value);
                break;
            case "customeremail":
                req.setCustomerEmail(value);
                break;
            case "customername":
                req.setCustomerName(value);
                break;
            case "subject":
                req.setSubject(value);
                break;
            case "description":
                req.setDescription(value);
                break;
            case "category":
                req.setCategory(value == null ? null : parseEnum(Category.class, value));
                break;
            case "priority":
                req.setPriority(value == null ? null : parseEnum(Priority.class, value));
                break;
            case "status":
                req.setStatus(value == null ? null : parseStatus(value));
                break;
            case "assignedto":
                req.setAssignedTo(value);
                break;
            case "tags":
                if (value != null) {
                    String separator = value.contains(";") ? ";" : (value.contains("|") ? "\\|" : ",");
                    String[] parts = value.split(separator);
                    req.setTags(Arrays.stream(parts)
                            .map(String::trim)
                            .filter(t -> !t.isEmpty())
                            .collect(java.util.stream.Collectors.toList()));
                }
                break;
            case "source":
                req.setSource(value == null ? null : parseEnum(Source.class, value));
                break;
            case "browser":
                req.setBrowser(value);
                break;
            case "devicetype":
                req.setDeviceType(value == null ? null : parseEnum(DeviceType.class, value));
                break;
            default:
                log.debug("Unknown CSV column '{}' — ignored", normalisedKey);
                break;
        }
    }

    /**
     * Parses an enum value case-insensitively.
     *
     * @throws IllegalArgumentException if the value is not a valid enum constant
     */
    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(
                "Invalid value '" + value + "' for enum " + enumClass.getSimpleName()
                + ". Valid values: " + Arrays.toString(enumClass.getEnumConstants()));
    }

    /**
     * Parses {@link com.example.support.model.Status} allowing the JSON alias
     * {@code "new"} as well as the Java enum constant {@code "new_"}.
     */
    private static com.example.support.model.Status parseStatus(String value) {
        if ("new".equalsIgnoreCase(value)) {
            return com.example.support.model.Status.new_;
        }
        return parseEnum(com.example.support.model.Status.class, value);
    }
}
