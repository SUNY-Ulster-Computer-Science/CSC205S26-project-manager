package projectmanagerprototype;

import java.io.Serializable;

/**
 * Represents a simple task with a description and associated date.
 * <p>
 * This class encapsulates a single task item consisting of a text description
 * and a date string. Tasks can be serialized for file storage and implements
 * a string representation for easy display.
 * </p>
 * 
 * <p><b>Date Format:</b> Dates are stored as strings in YYYY-MM-DD format 
 * (e.g., "2024-12-25").</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * Task task = new Task("Complete project report", "2024-12-31");
 * System.out.println(task); // Output: 2024-12-31: Complete project report
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
     * Constructs a new Task with the specified description and date.
     * 
     * @param description the textual description of what needs to be done
     * @param date the date for this task in YYYY-MM-DD format (e.g., "2024-12-25")
     */
    public Task(String description, String date) {
        this.description = description;
        this.date = date;
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
     * Returns a string representation of this task.
     * <p>
     * The format is: "date: description"
     * </p>
     * 
     * @return a formatted string containing the date and description
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return date + ": " + description;
    }
}