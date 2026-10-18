package com.meisterbauer.tagbasedskills.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Minimal, intentionally limited TOML reader for this mod's flat scalar values and quoted string
 * arrays. It is not a general TOML implementation; unsupported syntax falls back to callers'
 * defaults where possible.
 */
public final class TomlConfig {
    private static final char COMMENT_MARKER = '#';
    private static final char KEY_VALUE_SEPARATOR = '=';
    private static final char QUOTE = '"';
    private static final String ARRAY_START = "[";
    private static final String ARRAY_END = "]";
    private static final Pattern QUOTED_ARRAY_VALUE = Pattern.compile("\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");

    private TomlConfig() {
    }

    // Reads section-qualified raw values, such as {@code twerk.unlock_tag}, from a config file.
    public static Map<String, String> read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Map<String, String> values = new HashMap<>();
        String section = "";

        for (int index = 0; index < lines.size(); index++) {
            String line = stripComment(lines.get(index)).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (isSection(line)) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }

            int separatorIndex = line.indexOf(KEY_VALUE_SEPARATOR);
            if (separatorIndex <= 0) {
                continue;
            }

            String key = line.substring(0, separatorIndex).trim();
            String value = line.substring(separatorIndex + 1).trim();
            while (isUnclosedArray(value) && ++index < lines.size()) {
                value += ' ' + stripComment(lines.get(index)).trim();
            }
            values.put(section.isEmpty() ? key : section + '.' + key, value);
        }
        return values;
    }

    // Returns a non-blank string, unquoting the limited escaped-string syntax this reader supports.
    public static String stringValue(Map<String, String> values, String key, String fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        if (isQuoted(normalized)) {
            normalized = unescape(normalized.substring(1, normalized.length() - 1));
        }
        return normalized.isBlank() ? fallback : normalized;
    }

    // Parses only the TOML boolean literals {@code true} and {@code false}.
    public static boolean booleanValue(Map<String, String> values, String key, boolean fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value.trim().equalsIgnoreCase("true")) {
            return true;
        }
        if (value.trim().equalsIgnoreCase("false")) {
            return false;
        }
        return fallback;
    }

    // Parses a finite decimal value, using the supplied fallback for absent or invalid values.
    public static double doubleValue(Map<String, String> values, String key, double fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    // Parses the quoted members of an array, or returns no members when it is not a valid array.
    public static List<String> stringArray(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null) {
            return List.of();
        }
        String normalized = value.trim();
        if (!normalized.startsWith(ARRAY_START) || !normalized.endsWith(ARRAY_END)) {
            return List.of();
        }

        List<String> entries = new ArrayList<>();
        Matcher matcher = QUOTED_ARRAY_VALUE.matcher(normalized);
        while (matcher.find()) {
            entries.add(unescape(matcher.group(1)));
        }
        return List.copyOf(entries);
    }

    private static boolean isSection(String line) {
        return line.startsWith(ARRAY_START) && line.endsWith(ARRAY_END);
    }

    private static boolean isUnclosedArray(String value) {
        return value.startsWith(ARRAY_START) && !value.contains(ARRAY_END);
    }

    private static boolean isQuoted(String value) {
        return value.length() >= 2 && value.charAt(0) == QUOTE && value.charAt(value.length() - 1) == QUOTE;
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (character == '\\' && quoted) {
                escaped = true;
            } else if (character == QUOTE) {
                quoted = !quoted;
            } else if (character == COMMENT_MARKER && !quoted) {
                return line.substring(0, index);
            }
        }
        return line;
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
