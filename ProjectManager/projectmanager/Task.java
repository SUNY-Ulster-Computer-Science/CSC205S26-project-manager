package projectmanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a simple task with a description, associated date, and tags.
 * <p>
 * This class encapsulates a single task item consisting of a text description,
 * a date string, and optional tags for categorization. Tasks can be serialized 
 * for file storage and implements a string representation for easy display.
 * </p>
 *
 * <p><b>Date Format:</b> Dates are stored as strings in YYYY-MM-DD format
 * (e.g., "2024-12-25").</p>
 *
 * <p><b>Tags:</b> Tags are optional labels that can be used to categorize tasks
 * (e.g., "work", "personal", "urgent").</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * Task task = new Task("Complete project report", "2024-12-31");
 * task.addTag("work");
 * task.addTag("urgent");
 * System.out.println(task); // Output: 2024-12-31: Complete project report [work, urgent]
 * </pre>
 *
 * @author Team Orange
 * @version 1.0
 * @see TaskManager
 */
public class Task implements Serializable {

    /**
     * Serial version UID for serialization compatibility.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The textual description of the task.
     */
    private String description;

    /**
     * The date associated with this task in YYYY-MM-DD format.
     * Example: "2024-12-25"
     */
    private String date;

    /**
     * List of tags associated with this task for categorization.
     */
    private List<String> tags;

    /**
     * Constructs a new Task with the specified description and date.
     *
     * @param description the textual description of what needs to be done
     * @param date the date for this task in YYYY-MM-DD format (e.g., "2024-12-25")
     */
    public Task(String description, String date) {
        this.description = description;
        this.date = date;
        this.tags = new ArrayList<>();
    }

    /**
     * Constructs a new Task with the specified description, date, and tags.
     *
     * @param description the textual description of what needs to be done
     * @param date the date for this task in YYYY-MM-DD format (e.g., "2024-12-25")
     * @param tags list of tags to categorize this task
     */
    public Task(String description, String date, List<String> tags) {
        this.description = description;
        this.date = date;
        this.tags = new ArrayList<>(tags);
    }

    /**
     * Returns the description of this task.
     *
     * @return the task description as a string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the date associated with this task.
     *
     * @return the date string in YYYY-MM-DD format
     */
    public String getDate() {
        return date;
    }

    /**
     * Returns the list of tags associated with this task.
     *
     * @return list of tag strings
     */
    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    /**
     * Adds a tag to this task.
     *
     * @param tag the tag to add
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !tags.contains(tag.trim())) {
            tags.add(tag.trim());
        }
    }

    /**
     * Removes a tag from this task.
     *
     * @param tag the tag to remove
     * @return true if the tag was removed, false if it didn't exist
     */
    public boolean removeTag(String tag) {
        return tags.remove(tag);
    }

    /**
     * Checks if this task has a specific tag.
     *
     * @param tag the tag to check for
     * @return true if the task has this tag, false otherwise
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    /**
     * Returns a string representation of the tags in a comma-separated format.
     *
     * @return comma-separated tag string, or empty string if no tags
     */
    public String getTagsAsString() {
        if (tags.isEmpty()) {
            return "";
        }
        return String.join(",", tags);
    }

    /**
     * Returns a string representation of this task.
     * <p>
     * The format is: "date: description [tag1, tag2, ...]"
     * If no tags exist, the format is: "date: description"
     * </p>
     *
     * @return a formatted string containing the date, description, and tags
     * @see Object#toString()
     */
    @Override
    public String toString() {
        if (tags.isEmpty()) {
            return date + ": " + description;
        } else {
            return date + ": " + description + " [" + String.join(", ", tags) + "]";
        }
    }
}