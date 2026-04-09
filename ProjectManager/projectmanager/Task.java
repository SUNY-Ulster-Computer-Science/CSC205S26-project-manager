package projectmanager;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single task in the project manager.
 *
 * <p>A task holds a description, a due date, a {@link Status}, a {@link Priority},
 * optional free-text notes, an optional list of user-defined tags, and a creation
 * date.  Three reserved tag values ({@code "todo"}, {@code "in-progress"},
 * {@code "done"}) are managed automatically to keep the tag list in sync with
 * the task's current {@link Status}; callers should never add or remove these
 * tags directly.</p>
 *
 * <p>Instances are serializable so they can be written to and read from the JSON
 * persistence layer via {@link JsonUtil}.</p>
 */
public class Task implements Serializable {

    private static final long serialVersionUID = 2L;

    // -----------------------------------------------------------------------
    // Nested enums
    // -----------------------------------------------------------------------

    /**
     * Workflow states a task can occupy.
     */
    public enum Status {
        /** The task has not been started yet. */
        TODO("To Do"),
        /** The task is actively being worked on. */
        IN_PROGRESS("In Progress"),
        /** The task has been completed. */
        DONE("Done");

        private final String label;

        Status(String label) { this.label = label; }

        /**
         * Returns the human-readable display label for this status.
         *
         * @return display label (e.g. {@code "In Progress"})
         */
        public String getLabel() { return label; }
    }

    /**
     * Importance levels that can be assigned to a task.
     */
    public enum Priority {
        /** Low importance. */
        LOW("Low"),
        /** Medium importance (default). */
        MEDIUM("Medium"),
        /** High importance. */
        HIGH("High");

        private final String label;

        Priority(String label) { this.label = label; }

        /**
         * Returns the human-readable display label for this priority.
         *
         * @return display label (e.g. {@code "High"})
         */
        public String getLabel() { return label; }
    }

    // -----------------------------------------------------------------------
    // Reserved status-tag constants
    // -----------------------------------------------------------------------

    private static final String TAG_TODO        = "todo";
    private static final String TAG_IN_PROGRESS = "in-progress";
    private static final String TAG_DONE        = "done";

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private String       id;
    private String       description;
    private String       date;
    private Status       status;
    private Priority     priority;
    private String       notes;
    private String       createdDate;
    private List<String> tags;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates a new task with the given description and due date, no tags, and
     * default values for all other fields ({@link Status#TODO},
     * {@link Priority#MEDIUM}, empty notes, today as the creation date).
     *
     * @param description short title / description of the task (must not be {@code null})
     * @param date        ISO-8601 due-date string (e.g. {@code "2025-12-31"})
     */
    public Task(String description, String date) {
        this(description, date, new ArrayList<>());
    }

    /**
     * Creates a new task with the given description, due date, and initial tag list.
     *
     * <p>The status-sync tag ({@code "todo"} by default) is prepended automatically;
     * callers should not include status tags in the supplied list.</p>
     *
     * @param description short title / description of the task
     * @param date        ISO-8601 due-date string
     * @param tags        initial user-defined tags (copied defensively; may be empty)
     */
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

    // -----------------------------------------------------------------------
    // Identity
    // -----------------------------------------------------------------------

    /**
     * Returns the unique identifier of this task.
     *
     * @return UUID string assigned at construction time
     */
    public String getId() { return id; }

    /**
     * Overrides the unique identifier (used during JSON deserialization to
     * restore the original UUID).
     *
     * @param id the UUID string to assign
     */
    public void setId(String id) { this.id = id; }

    // -----------------------------------------------------------------------
    // Description
    // -----------------------------------------------------------------------

    /**
     * Returns the short title / description of this task.
     *
     * @return task description
     */
    public String getDescription() { return description; }

    /**
     * Replaces the description with a new value.
     *
     * @param description new description (should not be {@code null} or blank)
     */
    public void setDescription(String description) { this.description = description; }

    // -----------------------------------------------------------------------
    // Date
    // -----------------------------------------------------------------------

    /**
     * Returns the due date as an ISO-8601 string (e.g. {@code "2025-12-31"}).
     *
     * @return due-date string
     */
    public String getDate() { return date; }

    /**
     * Sets a new due date.  The supplied value should be a valid ISO-8601 date
     * string; an invalid value will cause {@link #getDueDate()} to return
     * {@code null}.
     *
     * @param date ISO-8601 due-date string
     */
    public void setDate(String date) { this.date = date; }

    // -----------------------------------------------------------------------
    // Status
    // -----------------------------------------------------------------------

    /**
     * Returns the current workflow status of this task.
     *
     * @return current {@link Status}
     */
    public Status getStatus() { return status; }

    /**
     * Sets the workflow status of this task and updates the reserved status tag
     * in the tag list accordingly.  If {@code null} is passed, the status is
     * reset to {@link Status#TODO}.
     *
     * @param status new status, or {@code null} to default to {@link Status#TODO}
     */
    public void setStatus(Status status) {
        this.status = (status == null) ? Status.TODO : status;
        syncStatusTag();
    }

    // -----------------------------------------------------------------------
    // Priority
    // -----------------------------------------------------------------------

    /**
     * Returns the current priority level of this task.
     *
     * @return current {@link Priority}
     */
    public Priority getPriority() { return priority; }

    /**
     * Sets the priority level.  If {@code null} is passed, the priority is reset
     * to {@link Priority#MEDIUM}.
     *
     * @param priority new priority, or {@code null} to default to {@link Priority#MEDIUM}
     */
    public void setPriority(Priority priority) {
        this.priority = (priority == null) ? Priority.MEDIUM : priority;
    }

    // -----------------------------------------------------------------------
    // Notes
    // -----------------------------------------------------------------------

    /**
     * Returns the free-text notes attached to this task, or an empty string if
     * none have been set.
     *
     * @return notes string (never {@code null})
     */
    public String getNotes() { return notes == null ? "" : notes; }

    /**
     * Replaces the free-text notes.  Passing {@code null} clears the notes field.
     *
     * @param notes new notes, or {@code null} to clear
     */
    public void setNotes(String notes) { this.notes = (notes == null) ? "" : notes; }

    // -----------------------------------------------------------------------
    // Created date
    // -----------------------------------------------------------------------

    /**
     * Returns the ISO-8601 date on which this task was originally created, or an
     * empty string if the field has not been populated (e.g. legacy tasks loaded
     * from {@code tasks.txt}).
     *
     * @return creation date string (never {@code null})
     */
    public String getCreatedDate() { return createdDate == null ? "" : createdDate; }

    /**
     * Overrides the creation date (used during JSON deserialization).
     *
     * @param createdDate ISO-8601 date string
     */
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    // -----------------------------------------------------------------------
    // Tags
    // -----------------------------------------------------------------------

    /**
     * Returns a defensive copy of the full tag list, including the leading
     * status tag ({@code "todo"}, {@code "in-progress"}, or {@code "done"}).
     *
     * @return immutable snapshot of the tag list
     */
    public List<String> getTags() { return new ArrayList<>(tags); }

    /**
     * Appends a user-defined tag if it is not already present and is non-blank.
     * Status tags should never be added this way; use {@link #setStatus} instead.
     *
     * @param tag tag text to add (leading/trailing whitespace is trimmed)
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !tags.contains(tag.trim())) {
            tags.add(tag.trim());
        }
    }

    /**
     * Removes a tag from the list by exact value.
     *
     * @param tag the tag to remove
     * @return {@code true} if the tag was present and removed, {@code false} otherwise
     */
    public boolean removeTag(String tag) { return tags.remove(tag); }

    /**
     * Returns {@code true} if the tag list contains the given value.
     *
     * @param tag the tag to check
     * @return {@code true} if the tag is present
     */
    public boolean hasTag(String tag) { return tags.contains(tag); }

    /**
     * Returns all tags as a single comma-separated string, or an empty string if
     * the tag list is empty.
     *
     * @return comma-separated tag string (e.g. {@code "todo,backend,urgent"})
     */
    public String getTagsAsString() {
        return tags.isEmpty() ? "" : String.join(",", tags);
    }

    /**
     * Replaces the entire user-defined tag list.  Any reserved status tags in
     * {@code newTags} are silently discarded; the correct status tag is then
     * re-inserted by {@link #syncStatusTag()}.
     *
     * @param newTags replacement list of user-defined tags (copied defensively)
     */
    public void setTags(List<String> newTags) {
        this.tags = new ArrayList<>(newTags);
        tags.remove(TAG_TODO);
        tags.remove(TAG_IN_PROGRESS);
        tags.remove(TAG_DONE);
        syncStatusTag();
    }

    /**
     * Removes all three reserved status tags from the list, then prepends the
     * one that corresponds to the current {@link #status}.  This keeps the tag
     * list consistent whenever the status changes.
     */
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

    // -----------------------------------------------------------------------
    // Date helpers
    // -----------------------------------------------------------------------

    /**
     * Parses the due-date string into a {@link LocalDate}.
     *
     * @return the parsed {@link LocalDate}, or {@code null} if the date string
     *         is absent or cannot be parsed
     */
    public LocalDate getDueDate() {
        try { return LocalDate.parse(date); } catch (Exception e) { return null; }
    }

    /**
     * Returns the number of days between today (inclusive) and the due date.
     * A negative value means the task is overdue; zero means it is due today.
     * Returns {@code 0} if the due date cannot be parsed.
     *
     * @return signed number of days until due (negative = past due)
     */
    public long daysUntilDue() {
        LocalDate due = getDueDate();
        if (due == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), due);
    }

    /**
     * Returns {@code true} if this task is not yet {@link Status#DONE} and its
     * due date is in the past.
     *
     * @return {@code true} if the task is overdue
     */
    public boolean isOverdue() {
        return status != Status.DONE && daysUntilDue() < 0;
    }

    /**
     * Returns a short, human-readable countdown string suitable for display in a
     * badge on the task card.
     *
     * <ul>
     *   <li>{@code "Done ✓"} – status is {@link Status#DONE}</li>
     *   <li>{@code "Overdue Nd"} – N days past the due date</li>
     *   <li>{@code "Due today!"} – due date is today</li>
     *   <li>{@code "Tomorrow"} – due date is tomorrow</li>
     *   <li>{@code "N days left"} – due date is N days away</li>
     * </ul>
     *
     * @return countdown label string
     */
    public String countdownLabel() {
        if (status == Status.DONE) return "Done ✓";
        long days = daysUntilDue();
        if (days < 0)  return "Overdue " + Math.abs(days) + "d";
        if (days == 0) return "Due today!";
        if (days == 1) return "Tomorrow";
        return days + " days left";
    }

    // -----------------------------------------------------------------------
    // Object overrides
    // -----------------------------------------------------------------------

    /**
     * Returns a compact string representation of the task that includes the due
     * date, description, and current tag list.
     *
     * @return human-readable task summary
     */
    @Override
    public String toString() {
        String tagStr = tags.isEmpty() ? "" : " [" + String.join(", ", tags) + "]";
        return date + ": " + description + tagStr;
    }
}