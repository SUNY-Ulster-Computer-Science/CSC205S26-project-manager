package projectmanager;

import java.util.*;
import java.util.regex.*;

/**
 * Zero-dependency JSON helper that serialises/deserialises a flat list of
 * {@link Task} objects.  The schema is intentionally simple; no nested
 * objects or arrays of objects are used aside from the top-level tasks array.
 *
 * <p>Example output:
 * <pre>
 * {
 *   "tasks": [
 *     {
 *       "id": "...",
 *       "description": "Submit report",
 *       "date": "2026-04-15",
 *       "status": "TODO",
 *       "priority": "HIGH",
 *       "notes": "See Slack thread",
 *       "createdDate": "2026-04-02",
 *       "tags": ["work", "urgent"]
 *     }
 *   ]
 * }
 * </pre>
 * </p>
 */
public final class JsonUtil {

    private JsonUtil() {}

    // ── Serialisation ─────────────────────────────────────────────────────

    public static String tasksToJson(List<Task> tasks) {
        StringBuilder sb = new StringBuilder("{\n  \"tasks\": [\n");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            sb.append("    {\n");
            appendField(sb, "id",          t.getId(),                    true);
            appendField(sb, "description", t.getDescription(),           true);
            appendField(sb, "date",        t.getDate(),                  true);
            appendField(sb, "status",      t.getStatus().name(),         true);
            appendField(sb, "priority",    t.getPriority().name(),       true);
            appendField(sb, "notes",       t.getNotes(),                 true);
            appendField(sb, "createdDate", t.getCreatedDate(),           true);
            // Tags array
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

    private static void appendField(StringBuilder sb, String key, String value, boolean comma) {
        sb.append("      ").append(esc(key)).append(": ").append(esc(value));
        if (comma) sb.append(",");
        sb.append("\n");
    }

    /** Wraps {@code s} in double quotes, escaping special chars. */
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

    // ── Deserialisation ───────────────────────────────────────────────────

    public static List<Task> tasksFromJson(String json) {
        List<Task> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;

        // Locate the tasks array
        int arrayStart = json.indexOf("[");
        if (arrayStart < 0) return result;

        // Walk character-by-character to find each top-level { } block
        int depth = 0;
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

    private static Task parseTaskObject(String obj) {
        String description = extractStr(obj, "description");
        String date        = extractStr(obj, "date");

        // Both are required
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

    // ── Regex helpers ─────────────────────────────────────────────────────

    private static final Pattern STR_FIELD =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** Extracts the string value of a named field from a JSON object fragment. */
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

    private static final Pattern ARRAY_FIELD =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern QUOTED =
            Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** Extracts a JSON string-array field. */
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
