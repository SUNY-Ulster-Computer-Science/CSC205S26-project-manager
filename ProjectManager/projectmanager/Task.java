package projectmanager;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a task with a description, due date, status, priority,
 * notes, tags, and automatic countdown to due date.
 *
 * <p>Changing a task's {@link Status} automatically synchronises a set of
 * reserved status-tags ("todo", "in-progress", "done") so that tag-based
 * filters always reflect the current state.</p>
 */
public class Task implements Serializable {

    private static final long serialVersionUID = 2L;

    // ── Status & Priority enums ───────────────────────────────────────────

    /** Workflow states for a task. */
    public enum Status {
        TODO("To Do"),
        IN_PROGRESS("In Progress"),
        DONE("Done");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /** Importance levels for a task. */
    public enum Priority {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High");

        private final String label;
        Priority(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    // ── Reserved tag names synced to Status ───────────────────────────────
    private static final String TAG_TODO        = "todo";
    private static final String TAG_IN_PROGRESS = "in-progress";
    private static final String TAG_DONE        = "done";

    // ── Fields ────────────────────────────────────────────────────────────

    /** Globally unique identifier (UUID v4). */
    private String id;

    /** Short summary / title of the task. */
    private String description;

    /** Due date in YYYY-MM-DD format. */
    private String date;

    /** Current workflow status. */
    private Status status;

    /** Importance level. */
    private Priority priority;

    /** Longer notes / details. */
    private String notes;

    /** ISO date on which this task was first created. */
    private String createdDate;

    /** User-defined and status-managed tag list. */
    private List<String> tags;

    // ── Constructors ──────────────────────────────────────────────────────

    public Task(String description, String date) {
        this(description, date, new ArrayList<>());
    }

    public Task(String description, String date, List<String> tags) {
        this.id          = UUID.randomUUID().toString();
        this.description = description;
        this.date        = date;
        this.tags        = new ArrayList<>(tags);
        this.status      = Status.TODO;
        this.priority    = Priority.MEDIUM;
        this.notes       = "";
        this.createdDate = LocalDate.now().toString();
        syncStatusTag();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public String getId()          { return id; }
    public void   setId(String id) { this.id = id; }

    public String getDescription()                   { return description; }
    public void   setDescription(String description) { this.description = description; }

    public String getDate()            { return date; }
    public void   setDate(String date) { this.date = date; }

    public Status getStatus() { return status; }

    /**
     * Changes the task status and automatically updates the reserved
     * status-tags ("todo", "in-progress", "done").
     *
     * @param status new status
     */
    public void setStatus(Status status) {
        this.status = (status == null) ? Status.TODO : status;
        syncStatusTag();
    }

    public Priority getPriority()                    { return priority; }
    public void     setPriority(Priority priority)   { this.priority = (priority == null) ? Priority.MEDIUM : priority; }

    public String getNotes()              { return notes == null ? "" : notes; }
    public void   setNotes(String notes)  { this.notes = (notes == null) ? "" : notes; }

    public String getCreatedDate()                      { return createdDate == null ? "" : createdDate; }
    public void   setCreatedDate(String createdDate)    { this.createdDate = createdDate; }

    // ── Tag management ────────────────────────────────────────────────────

    public List<String> getTags() { return new ArrayList<>(tags); }

    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !tags.contains(tag.trim())) {
            tags.add(tag.trim());
        }
    }

    public boolean removeTag(String tag) { return tags.remove(tag); }

    public boolean hasTag(String tag)    { return tags.contains(tag); }

    public String getTagsAsString() {
        return tags.isEmpty() ? "" : String.join(",", tags);
    }

    /**
     * Replaces the entire tag list (used when editing tags directly).
     * Reserved status-tags are re-synced afterwards.
     *
     * @param newTags replacement tag list
     */
    public void setTags(List<String> newTags) {
        this.tags = new ArrayList<>(newTags);
        // Remove any stale status tags then re-add the correct one
        tags.remove(TAG_TODO);
        tags.remove(TAG_IN_PROGRESS);
        tags.remove(TAG_DONE);
        syncStatusTag();
    }

    /** Removes the three reserved status-tags and adds the one that matches {@link #status}. */
    private void syncStatusTag() {
        tags.remove(TAG_TODO);
        tags.remove(TAG_IN_PROGRESS);
        tags.remove(TAG_DONE);
        switch (status) {
            case TODO:        tags.add(0, TAG_TODO);        break;
            case IN_PROGRESS: tags.add(0, TAG_IN_PROGRESS); break;
            case DONE:        tags.add(0, TAG_DONE);        break;
        }
    }

    // ── Countdown helpers ─────────────────────────────────────────────────

    /**
     * @return the parsed due-date as a {@link LocalDate}, or {@code null} if unparseable
     */
    public LocalDate getDueDate() {
        try { return LocalDate.parse(date); } catch (Exception e) { return null; }
    }

    /**
     * @return number of days from today until the due date (negative = past)
     */
    public long daysUntilDue() {
        LocalDate due = getDueDate();
        if (due == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), due);
    }

    /**
     * @return {@code true} if the due date has passed and the task is not DONE
     */
    public boolean isOverdue() {
        return status != Status.DONE && daysUntilDue() < 0;
    }

    /**
     * Human-readable countdown string shown on a task card.
     *
     * @return e.g. "Done ✓", "Overdue 3d", "Due today!", "Tomorrow", "5 days left"
     */
    public String countdownLabel() {
        if (status == Status.DONE) return "Done ✓";
        long days = daysUntilDue();
        if (days < 0)  return "Overdue " + Math.abs(days) + "d";
        if (days == 0) return "Due today!";
        if (days == 1) return "Tomorrow";
        return days + " days left";
    }

    // ── Object overrides ──────────────────────────────────────────────────

    @Override
    public String toString() {
        String tagStr = tags.isEmpty() ? "" : " [" + String.join(", ", tags) + "]";
        return date + ": " + description + tagStr;
    }
}
