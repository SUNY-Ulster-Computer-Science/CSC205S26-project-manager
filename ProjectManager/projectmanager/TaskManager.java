package projectmanager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Manages a collection of {@link Task} objects keyed by due-date string,
 * with JSON-based file persistence.
 *
 * <p>All mutations (add / update / remove) update the internal map; call
 * {@link #saveToFile()} to persist them to {@code tasks.json}.</p>
 */
public class TaskManager {

    /** Filename for JSON persistence. */
    private static final String FILENAME = "tasks.json";

    /**
     * Primary store: date-string → list of tasks.
     * Dates are in ISO format (YYYY-MM-DD).
     */
    private final Map<String, List<Task>> tasks = new HashMap<>();

    // ── Constructor ───────────────────────────────────────────────────────

    public TaskManager() {
        loadFromFile();
    }

    // ── Add ───────────────────────────────────────────────────────────────

    public void addTask(String date, String description) {
        tasks.computeIfAbsent(date, k -> new ArrayList<>())
             .add(new Task(description, date));
    }

    public void addTask(String date, String description, List<String> tags) {
        Task t = new Task(description, date);
        for (String tag : tags) t.addTag(tag);
        tasks.computeIfAbsent(date, k -> new ArrayList<>()).add(t);
    }

    /** Adds a fully constructed task (used when restoring from JSON or creating from a dialog). */
    public void addTask(Task task) {
        tasks.computeIfAbsent(task.getDate(), k -> new ArrayList<>()).add(task);
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Replaces an existing task by its {@link Task#getId() UUID}.
     * If the due-date changed, the task is moved to the new date bucket.
     *
     * @param updated the edited task object with the same UUID as the original
     * @return {@code true} if the task was found and replaced
     */
    public boolean updateTask(Task updated) {
        // Search every date bucket
        for (Map.Entry<String, List<Task>> entry : tasks.entrySet()) {
            List<Task> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getId().equals(updated.getId())) {
                    list.remove(i);
                    if (list.isEmpty()) tasks.remove(entry.getKey());
                    // Re-add under the (possibly new) date
                    tasks.computeIfAbsent(updated.getDate(), k -> new ArrayList<>())
                         .add(updated);
                    return true;
                }
            }
        }
        return false;
    }

    // ── Remove ────────────────────────────────────────────────────────────

    /**
     * Removes a task by its UUID.
     *
     * @return {@code true} if removed
     */
    public boolean removeTaskById(String id) {
        for (Map.Entry<String, List<Task>> entry : tasks.entrySet()) {
            Iterator<Task> it = entry.getValue().iterator();
            while (it.hasNext()) {
                if (it.next().getId().equals(id)) {
                    it.remove();
                    if (entry.getValue().isEmpty()) tasks.remove(entry.getKey());
                    return true;
                }
            }
        }
        return false;
    }

    /** Legacy 1-based removal by date + index (kept for backward compat). */
    public boolean removeTask(String date, int taskNumber) {
        List<Task> list = tasks.get(date);
        if (list == null || taskNumber < 1 || taskNumber > list.size()) return false;
        list.remove(taskNumber - 1);
        if (list.isEmpty()) tasks.remove(date);
        return true;
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public List<Task> getTasksForDate(String date) {
        return tasks.getOrDefault(date, Collections.emptyList());
    }

    /** Returns every date string (YYYY-MM-DD) that has at least one task. */
    public Set<String> getAllDatesWithTasks() {
        return Collections.unmodifiableSet(tasks.keySet());
    }

    public List<Task> getAllTasksFlat() {
        List<Task> all = new ArrayList<>();
        for (List<Task> list : tasks.values()) all.addAll(list);
        return all;
    }

    public List<Task> getAllTasksSortedFlat() {
        List<String> sortedDates = new ArrayList<>(tasks.keySet());
        Collections.sort(sortedDates);
        List<Task> result = new ArrayList<>();
        for (String date : sortedDates) result.addAll(tasks.get(date));
        return result;
    }

    public List<Task> getTasksByTag(String tag) {
        List<Task> result = new ArrayList<>();
        for (List<Task> list : tasks.values())
            for (Task t : list)
                if (t.hasTag(tag)) result.add(t);
        return result;
    }

    public List<String> getAllTagsWithCount() {
        Map<String, Integer> counts = new TreeMap<>();
        for (List<Task> list : tasks.values())
            for (Task t : list)
                for (String tag : t.getTags())
                    counts.merge(tag, 1, Integer::sum);

        List<String> result = new ArrayList<>();
        counts.forEach((tag, count) -> result.add(tag + " (" + count + ")"));
        return result;
    }

    /** Returns the number of tasks scheduled on a specific date. */
    public int getTaskCountForDate(String date) {
        List<Task> list = tasks.get(date);
        return list == null ? 0 : list.size();
    }

    // ── Persistence ───────────────────────────────────────────────────────

    public void saveToFile() {
        try {
            String json = JsonUtil.tasksToJson(getAllTasksFlat());
            Files.write(Paths.get(FILENAME), json.getBytes(StandardCharsets.UTF_8));
            System.out.println("[TaskManager] Saved " + getAllTasksFlat().size() + " task(s) to " + FILENAME);
        } catch (IOException e) {
            System.err.println("[TaskManager] Error saving: " + e.getMessage());
        }
    }

    public void loadFromFile() {
        tasks.clear();
        File file = new File(FILENAME);
        if (!file.exists()) {
            // Try legacy txt file for migration
            migrateLegacyTxt();
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String json  = new String(bytes, StandardCharsets.UTF_8);
            List<Task> loaded = JsonUtil.tasksFromJson(json);
            for (Task t : loaded)
                tasks.computeIfAbsent(t.getDate(), k -> new ArrayList<>()).add(t);
            System.out.println("[TaskManager] Loaded " + loaded.size() + " task(s) from " + FILENAME);
        } catch (IOException e) {
            System.err.println("[TaskManager] Error loading: " + e.getMessage());
        }
    }

    /** One-time migration from the old pipe-delimited tasks.txt format. */
    private void migrateLegacyTxt() {
        File legacy = new File("tasks.txt");
        if (!legacy.exists()) return;
        System.out.println("[TaskManager] Migrating tasks.txt → tasks.json …");
        try (BufferedReader reader = new BufferedReader(new FileReader(legacy))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 3);
                if (parts.length < 2) continue;
                String date        = parts[0].trim();
                String description = parts[1].trim();
                List<String> tags  = new ArrayList<>();
                if (parts.length == 3 && !parts[2].trim().isEmpty()) {
                    for (String tag : parts[2].split(","))
                        if (!tag.trim().isEmpty()) tags.add(tag.trim());
                }
                Task t = new Task(description, date, tags);
                tasks.computeIfAbsent(date, k -> new ArrayList<>()).add(t);
            }
            saveToFile(); // write out the migrated JSON
        } catch (IOException e) {
            System.err.println("[TaskManager] Migration error: " + e.getMessage());
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private String getWeekday(String dateStr) {
        try {
            LocalDate d = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            String name = d.getDayOfWeek().toString();
            return name.charAt(0) + name.substring(1).toLowerCase();
        } catch (DateTimeParseException e) {
            return "Unknown";
        }
    }
}
