package banking.pipeline.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * File-based message protocol: creates/resets the shared/{input,processing,output,results}
 * directories and reads/writes JSON messages between agents.
 */
public class SharedDirectoryService {

    private final Path baseDir;
    private final ObjectMapper mapper;

    public SharedDirectoryService(Path baseDir) {
        this.baseDir = baseDir;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public Path inputDir() {
        return baseDir.resolve("input");
    }

    public Path processingDir() {
        return baseDir.resolve("processing");
    }

    public Path outputDir() {
        return baseDir.resolve("output");
    }

    public Path resultsDir() {
        return baseDir.resolve("results");
    }

    /** Recreates all shared/ subdirectories empty, so repeated runs never see stale data. */
    public void resetDirectories() throws IOException {
        for (Path dir : List.of(inputDir(), processingDir(), outputDir(), resultsDir())) {
            deleteRecursively(dir);
            Files.createDirectories(dir);
        }
    }

    public void write(Path dir, String fileName, Object value) throws IOException {
        Files.createDirectories(dir);
        mapper.writeValue(dir.resolve(fileName).toFile(), value);
    }

    public <T> T read(Path file, Class<T> type) throws IOException {
        return mapper.readValue(file.toFile(), type);
    }

    public <T> List<T> readAll(Path dir, Class<T> type) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<T> results = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> sorted = files
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
            for (Path file : sorted) {
                results.add(mapper.readValue(file.toFile(), type));
            }
        }
        return results;
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to delete " + p, e);
                }
            });
        }
    }
}
