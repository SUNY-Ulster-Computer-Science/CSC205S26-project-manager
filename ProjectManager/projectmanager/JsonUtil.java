package projectmanager;

import java.util.*;
import java.util.regex.*;

/**
 * Lightweight JSON serialization / deserialization utility for {@link Task} objects.
 *
 * <p>This class avoids any third-party JSON library and instead produces and
 * consumes a well-defined subset of JSON.  The serialized format is a single
 * top-level object with one {@code "tasks"} array:</p>
 *
 * <pre>
 * {
 *   "tasks": [
 *     {
 *       "id": "…",
 *       "description": "…",
 *       "date": "2025-12-31",
 *       "status": "TODO",
 *       "priority": "MEDIUM",
 *       "notes": "…",
 *       "createdDate": "2025-01-01",
 *       "tags": ["todo", "backend"]
 *     },
 *     …
 *   ]
 * }
 * </pre>
 *
 * <p>The class is non-instantiable (utility class pattern).  All methods are
 * static.</p>
 */
public final class JsonUtil {

    /** Prevent instantiation. */
    private JsonUtil() {}

    // -----------------------------------------------------------------------
    // Serialization
    // -----------------------------------------------------------------------

    /**
     * Serializes a list of tasks to a JSON string using the format described in
     * the class-level documentation.
     *
     * <p>Special characters in string values ({@code \}, {@code "}, newline,
     * carriage return, tab) are escaped so the output is valid JSON.</p>
     *
     * @param tasks the tasks to serialize (may be empty; must not be {@code null})
     * @return a formatted JSON string representing all supplied tasks
     */
    public static String tasksToJson(List<Task> tasks) {
        StringBuilder sb = new StringBuilder("{\n  \"tasks\": [\n");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            sb.append("    {\n");
            appendField(sb, "id",          t.getId(),              true);
            appendField(sb, "description", t.getDescription(),     true);
            appendField(sb, "date",        t.getDate(),            true);
            appendField(sb, "status",      t.getStatus().name(),   true);
            appendField(sb, "priority",    t.getPriority().name(), true);
            appendField(sb, "notes",       t.getNotes(),           true);
            appendField(sb, "createdDate", t.getCreatedDate(),     true);
            sb.append("      \"tags\": [");
            List<String> tags = t.getTags();
            for (int j = 0; j < tags.size(); j++) {
                sb.append(esc(tags.get(j)));
                if (j < tags.size() - 1) sb.append(", ");
            }
            sb.append("]\n    }");
            if (i < tasks.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }

    /**
     * Appends a single JSON key-value pair (string value) to the builder,
     * indented for readability.
     *
     * @param sb    the builder to append to
     * @param key   field name (will be JSON-escaped)
     * @param value field value (will be JSON-escaped)
     * @param comma if {@code true} a trailing comma is added after the value
     */
    private static void appendField(StringBuilder sb, String key, String value, boolean comma) {
        sb.append("      ").append(esc(key)).append(": ").append(esc(value));
        if (comma) sb.append(",");
        sb.append("\n");
    }

    /**
     * Wraps a string in JSON double-quotes and escapes any characters that would
     * produce invalid JSON ({@code \}, {@code "}, {@code \n}, {@code \r},
     * {@code \t}).  Returns {@code ""} for a {@code null} input.
     *
     * @param s the string to escape and quote
     * @return properly quoted and escaped JSON string literal
     */
    private static String esc(String s) {
        if (s == null) return "\"\"";
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    // -----------------------------------------------------------------------
    // Deserialization
    // -----------------------------------------------------------------------

    /**
     * Parses a JSON string (as produced by {@link #tasksToJson}) and returns the
     * list of {@link Task} objects it contains.
     *
     * <p>The method uses a character-level brace-matching approach to extract
     * individual task objects from the array, then delegates each object to
     * {@link #parseTaskObject(String)}.  Objects that are missing mandatory
     * fields ({@code description}, {@code date}) are silently skipped.</p>
     *
     * @param json the JSON string to parse; may be {@code null} or empty
     * @return list of parsed tasks (may be empty; never {@code null})
     */
    public static List<Task> tasksFromJson(String json) {
        List<Task> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;

        int arrayStart = json.indexOf("[");
        if (arrayStart < 0) return result;

        int depth    = 0;
        int objStart = -1;
        for (int i = arrayStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    Task task = parseTaskObject(json.substring(objStart, i + 1));
                    if (task != null) result.add(task);
                    objStart = -1;
                }
            }
        }
        return result;
    }

    /**
     * Parses a single JSON object string into a {@link Task}.
     *
     * <p>Fields that are present in the JSON are applied to the task; unknown
     * fields are silently ignored.  If either {@code description} or {@code date}
     * is missing the method returns {@code null} and the object is skipped by the
     * caller.</p>
     *
     * @param obj the JSON object substring (including braces)
     * @return the populated {@link Task}, or {@code null} if mandatory fields
     *         are absent
     */
    private static Task parseTaskObject(String obj) {
        String description = extractStr(obj, "description");
        String date        = extractStr(obj, "date");

        if (description == null || date == null) return null;

        List<String> tags = extractStrArray(obj, "tags");
        Task task = new Task(description, date, tags);

        String id = extractStr(obj, "id");
        if (id != null && !id.isEmpty()) task.setId(id);

        String notes = extractStr(obj, "notes");
        if (notes != null) task.setNotes(notes);

        String createdDate = extractStr(obj, "createdDate");
        if (createdDate != null && !createdDate.isEmpty()) task.setCreatedDate(createdDate);

        String statusStr = extractStr(obj, "status");
        if (statusStr != null) {
            try { task.setStatus(Task.Status.valueOf(statusStr)); } catch (Exception ignored) {}
        }

        String priorityStr = extractStr(obj, "priority");
        if (priorityStr != null) {
            try { task.setPriority(Task.Priority.valueOf(priorityStr)); } catch (Exception ignored) {}
        }

        return task;
    }

    // -----------------------------------------------------------------------
    // Regex helpers
    // -----------------------------------------------------------------------

    /**
     * Pre-compiled pattern that matches a JSON string field of the form
     * {@code "key": "value"} where {@code value} may contain escaped characters.
     */
    private static final Pattern STR_FIELD =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    /**
     * Extracts the string value of a named field from a JSON object fragment.
     * Standard JSON escape sequences ({@code \"}, {@code \n}, {@code \r},
     * {@code \t}, {@code \\}) in the value are decoded before returning.
     *
     * @param obj the JSON object substring to search
     * @param key the field name to look up
     * @return the decoded string value, or {@code null} if the field is not found
     */
    private static String extractStr(String obj, String key) {
        Matcher m = STR_FIELD.matcher(obj);
        while (m.find()) {
            if (m.group(1).equals(key)) {
                return m.group(2)
                        .replace("\\\"", "\"")
                        .replace("\\n",  "\n")
                        .replace("\\r",  "\r")
                        .replace("\\t",  "\t")
                        .replace("\\\\", "\\");
            }
        }
        return null;
    }

    /**
     * Pre-compiled pattern that matches a JSON array field of the form
     * {@code "key": ["v1", "v2", …]}.
     */
    private static final Pattern ARRAY_FIELD =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\[([^\\]]*)\\]");

    /**
     * Pre-compiled pattern that matches a single quoted string inside a JSON
     * array element.
     */
    private static final Pattern QUOTED =
            Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    /**
     * Extracts all string elements from a named JSON string-array field.
     *
     * @param obj the JSON object substring to search
     * @param key the array field name to look up
     * @return list of decoded string elements, or an empty list if the field
     *         is not found or contains no string elements
     */
    private static List<String> extractStrArray(String obj, String key) {
        List<String> result = new ArrayList<>();
        Matcher am = ARRAY_FIELD.matcher(obj);
        while (am.find()) {
            if (am.group(1).equals(key)) {
                Matcher qm = QUOTED.matcher(am.group(2));
                while (qm.find()) result.add(qm.group(1));
                break;
            }
        }
        return result;
    }
}