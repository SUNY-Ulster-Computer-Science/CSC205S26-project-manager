package projectmanager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Central data-access layer for the project manager.
 *
 * <p>{@code TaskManager} owns the in-memory store of all {@link Task} objects,
 * organized as a {@code Map} from ISO-8601 date strings to lists of tasks for
 * that date.  It also handles persistence:</p>
 * <ul>
 *   <li><strong>Primary format:</strong> {@code tasks.json} written by
 *       {@link JsonUtil}.</li>
 *   <li><strong>Legacy migration:</strong> if {@code tasks.json} is absent but
 *       the old {@code tasks.txt} pipe-delimited file exists, it is automatically
 *       migrated and saved as JSON on first run.</li>
 * </ul>
 *
 * <p>All mutating methods ({@code addTask}, {@code updateTask},
 * {@code removeTask*}) operate only on the in-memory map.  Call
 * {@link #saveToFile()} explicitly after any mutation to persist changes.</p>
 */
public class TaskManager {

    /** Name of the JSON file used to persist tasks. */
    private static final String FILENAME = "tasks.json";

    /**
     * Internal store: maps ISO-8601 date strings (e.g. {@code "2025-12-31"})
     * to the ordered list of tasks scheduled for that date.
     */
    private final Map<String, List<Task>> tasks = new HashMap<>();

    /**
     * Constructs a new {@code TaskManager} and immediately loads persisted tasks
     * from disk (JSON file, or legacy text file if JSON is absent).
     */
    public TaskManager() {
        loadFromFile();
    }

    // -----------------------------------------------------------------------
    // Mutation
    // -----------------------------------------------------------------------

    /**
     * Creates a new task with the given date and description (no tags) and
     * appends it to the list for that date.
     *
     * @param date        ISO-8601 due-date string
     * @param description short task title
     */
    public void addTask(String date, String description) {
        tasks.computeIfAbsent(date, k -> new ArrayList<>())
             .add(new Task(description, date));
    }

    /**
     * Creates a new task with the given date, description, and initial tags, then
     * appends it to the list for that date.
     *
     * @param date        ISO-8601 due-date string
     * @param description short task title
     * @param tags        initial user-defined tags to attach to the task
     */
    public void addTask(String date, String description, List<String> tags) {
        Task t = new Task(description, date);
        for (String tag : tags) t.addTag(tag);
        tasks.computeIfAbsent(date, k -> new ArrayList<>()).add(t);
    }

    /**
     * Appends a fully-constructed {@link Task} to the list for its due date.
     *
     * @param task the task to add (must have a valid date)
     */
    public void addTask(Task task) {
        tasks.computeIfAbsent(task.getDate(), k -> new ArrayList<>()).add(task);
    }

    /**
     * Replaces an existing task (matched by {@link Task#getId()}) with the
     * supplied updated version.
     *
     * <p>Because the date may have changed, the old entry is removed from its
     * original date bucket and the updated task is inserted into the bucket
     * matching its new date.</p>
     *
     * @param updated task carrying the new field values; its {@code id} must
     *                match an existing task
     * @return {@code true} if a matching task was found and replaced;
     *         {@code false} if no task with that ID exists
     */
    public boolean updateTask(Task updated) {
        for (Map.Entry<String, List<Task>> entry : tasks.entrySet()) {
            List<Task> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getId().equals(updated.getId())) {
                    list.remove(i);
                    if (list.isEmpty()) tasks.remove(entry.getKey());
                    tasks.computeIfAbsent(updated.getDate(), k -> new ArrayList<>())
                         .add(updated);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes the task with the given UUID from the store.
     *
     * @param id UUID of the task to remove
     * @return {@code true} if the task was found and removed; {@code false} otherwise
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

    /**
     * Removes a task by its 1-based position within the list for a specific date.
     *
     * @param date       ISO-8601 date string identifying the task list
     * @param taskNumber 1-based index of the task within that date's list
     * @return {@code true} if the task was removed; {@code false} if the date
     *         has no tasks or the index is out of range
     */
    public boolean removeTask(String date, int taskNumber) {
        List<Task> list = tasks.get(date);
        if (list == null || taskNumber < 1 || taskNumber > list.size()) return false;
        list.remove(taskNumber - 1);
        if (list.isEmpty()) tasks.remove(date);
        return true;
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    /**
     * Returns the (possibly empty) list of tasks for a specific date.  The
     * returned list is the live backing list – do not modify it directly.
     *
     * @param date ISO-8601 date string
     * @return unmodifiable-safe list of tasks for that date (never {@code null})
     */
    public List<Task> getTasksForDate(String date) {
        return tasks.getOrDefault(date, Collections.emptyList());
    }

    /**
     * Returns an unmodifiable view of all dates that have at least one task.
     *
     * @return set of ISO-8601 date strings
     */
    public Set<String> getAllDatesWithTasks() {
        return Collections.unmodifiableSet(tasks.keySet());
    }

    /**
     * Returns a flat list of all tasks across all dates in no guaranteed order.
     *
     * @return mutable list containing every task
     */
    public List<Task> getAllTasksFlat() {
        List<Task> all = new ArrayList<>();
        for (List<Task> list : tasks.values()) all.addAll(list);
        return all;
    }

    /**
     * Returns a flat list of all tasks sorted chronologically by due date
     * (earliest first), then by insertion order within each date.
     *
     * @return chronologically ordered list of every task
     */
    public List<Task> getAllTasksSortedFlat() {
        List<String> sortedDates = new ArrayList<>(tasks.keySet());
        Collections.sort(sortedDates);
        List<Task> result = new ArrayList<>();
        for (String date : sortedDates) result.addAll(tasks.get(date));
        return result;
    }

    /**
     * Returns all tasks that carry the given tag.
     *
     * @param tag the tag to search for (exact match)
     * @return list of matching tasks (may be empty; never {@code null})
     */
    public List<Task> getTasksByTag(String tag) {
        List<Task> result = new ArrayList<>();
        for (List<Task> list : tasks.values())
            for (Task t : list)
                if (t.hasTag(tag)) result.add(t);
        return result;
    }

    /**
     * Returns a sorted list of strings in the format {@code "tagName (N)"} where
     * {@code N} is the number of tasks bearing that tag.  Includes status tags.
     *
     * @return alphabetically sorted tag-with-count strings
     */
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

    /**
     * Returns the number of tasks scheduled for the given date.
     *
     * @param date ISO-8601 date string
     * @return task count for that date (0 if none)
     */
    public int getTaskCountForDate(String date) {
        List<Task> list = tasks.get(date);
        return list == null ? 0 : list.size();
    }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    /**
     * Serializes all tasks to {@value #FILENAME} in the working directory using
     * the custom JSON format produced by {@link JsonUtil#tasksToJson}.  Logs the
     * result (or any {@link IOException}) to {@code System.out} / {@code System.err}.
     */
    public void saveToFile() {
        try {
            String json = JsonUtil.tasksToJson(getAllTasksFlat());
            Files.write(Paths.get(FILENAME), json.getBytes(StandardCharsets.UTF_8));
            System.out.println("[TaskManager] Saved " + getAllTasksFlat().size() + " task(s) to " + FILENAME);
        } catch (IOException e) {
            System.err.println("[TaskManager] Error saving: " + e.getMessage());
        }
    }

    /**
     * Clears the in-memory store and reloads tasks from {@value #FILENAME}.
     * If the JSON file does not exist, falls back to {@link #migrateLegacyTxt()}.
     * Any {@link IOException} is logged to {@code System.err} without re-throwing.
     */
    public void loadFromFile() {
        tasks.clear();
        File file = new File(FILENAME);
        if (!file.exists()) {
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

    /**
     * Reads the legacy pipe-delimited {@code tasks.txt} file (format:
     * {@code date|description[|tag1,tag2,…]}) and populates the in-memory store,
     * then immediately calls {@link #saveToFile()} to write the converted data as
     * JSON.  Called automatically by {@link #loadFromFile()} when
     * {@value #FILENAME} does not yet exist.
     */
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
            saveToFile();
        } catch (IOException e) {
            System.err.println("[TaskManager] Migration error: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the full English weekday name for a date string (e.g.
     * {@code "Monday"}).  Returns {@code "Unknown"} if the string cannot be
     * parsed.
     *
     * @param dateStr ISO-8601 date string
     * @return capitalized weekday name, or {@code "Unknown"}
     */
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