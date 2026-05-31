package service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import model.Job;

/**
 *
 * @author carlos
 */
public class YAMLReader {

    public List<Job> loadJobs(Path jobsDirectory) throws IOException {
        List<Job> jobs = new ArrayList<>();

        if (!Files.exists(jobsDirectory)) {
            throw new IOException("No existe la carpeta de jobs: " + jobsDirectory.toAbsolutePath());
        }

        try (var paths = Files.list(jobsDirectory)) {
            List<Path> yamlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isYaml)
                    .sorted()
                    .toList();

            for (Path file : yamlFiles) {
                try {
                    jobs.add(loadJob(file));
                } catch (IllegalArgumentException e) {
                    System.err.println("[YAML] Rechazado " + file.getFileName() + ": " + e.getMessage());
                }
            }
        }

        return jobs;
    }

    public Job loadJob(Path file) throws IOException {
        Map<String, String> values = readYamlValues(file);

        String id = getRequiredText(values, "id");
        String name = getRequiredText(values, "name");
        int priority = getInt(values, "priority", 0, 4);
        int cpuCores = getInt(values, "resources.cpu_cores", 1, Integer.MAX_VALUE);
        int memMb = getMemory(getRequiredText(values, "resources.memory"));
        long durationMs = getLong(values, "workload.duration_ms", 1, Long.MAX_VALUE);

        return new Job(id, name, priority, cpuCores, memMb, durationMs);
    }

    private Map<String, String> readYamlValues(Path file) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        String section = "";

        for (String rawLine : Files.readAllLines(file)) {
            String withoutComment = removeComment(rawLine);
            if (withoutComment.trim().isEmpty()) {
                continue;
            }

            int indent = countSpaces(withoutComment);
            String line = withoutComment.trim();
            int colon = line.indexOf(':');
            if (colon < 0) {
                throw new IllegalArgumentException("linea YAML no valida: " + line);
            }

            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (value.isEmpty()) {
                section = key;
                continue;
            }

            String fullKey;
            if (indent > 0 && !section.isEmpty()) {
                fullKey = section + "." + key;
            } else {
                fullKey = key;
            }
            values.put(fullKey, unquote(value));
        }

        return values;
    }

    private boolean isYaml(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }

    private String getRequiredText(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("falta o esta vacio el campo " + key);
        }
        return value.trim();
    }

    private int getInt(Map<String, String> values, String key, int min, int max) {
        long number = getLong(values, key, min, max);
        return (int) number;
    }

    private long getLong(Map<String, String> values, String key, long min, long max) {
        String text = getRequiredText(values, key);
        try {
            long value = Long.parseLong(text);
            if (value < min || value > max) {
                throw new IllegalArgumentException(key + " debe estar entre " + min + " y " + max);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " debe ser numerico");
        }
    }

    private int getMemory(String text) {
        String normalized = text.trim().toUpperCase(Locale.ROOT);
        String[] parts = normalized.split("\\s+");
        if (parts.length != 2 || (!parts[1].equals("MB") && !parts[1].equals("GB"))) {
            throw new IllegalArgumentException("resources.memory debe tener formato '256 MB' o '1 GB'");
        }

        try {
            int amount = Integer.parseInt(parts[0]);
            if (amount <= 0) {
                throw new IllegalArgumentException("resources.memory debe ser positiva");
            }
            if (parts[1].equals("GB")) {
                return amount * 1024;
            } else {
                return amount;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("resources.memory debe empezar por un entero");
        }
    }

    private String removeComment(String line) {
        int index = line.indexOf('#');
        if (index >= 0) {
            return line.substring(0, index);
        } else {
            return line;
        }
    }

    private int countSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
