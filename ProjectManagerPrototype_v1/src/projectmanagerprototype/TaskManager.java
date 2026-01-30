package projectmanagerprototype;

import java.util.*;
import java.io.*;

/**
 * Manages a collection of tasks organized by date with file persistence.
 * <p>
 * This class provides functionality to add, view, and persist tasks to a file.
 * Tasks are organized in a map structure where the key is a date string and
 * the value is a list of tasks for that date. Data is automatically loaded
 * from file upon instantiation and can be saved at any time.
 * </p>
 * 
 * <p><b>File Format:</b> Tasks are stored in a pipe-delimited text file format:
 * <pre>date|description</pre>
 * Example: <code>2024-12-25|Buy Christmas gifts</code>
 * </p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * TaskManager manager = new TaskManager();
 * manager.addTask("2024-12-31", "Submit final report");
 * manager.viewTasks("2024-12-31");
 * manager.saveToFile();
 * </pre>
 * 
 * @author Team Orange
 * @version 1.0
 * @see Task
 */
public class TaskManager {
    
    /**
     * Map storing tasks organized by date. 
     * Key: date string in YYYY-MM-DD format
     * Value: list of tasks for that date
     */
    private Map<String, List<Task>> tasks;
    
    /**
     * The filename used for loading and saving task data.
     * Default value is "tasks.txt".
     */
    private String filename = "tasks.txt";
    
    /**
     * Constructs a new TaskManager and loads existing tasks from file.
     * <p>
     * Upon instantiation, this constructor initializes the task map and
     * attempts to load any previously saved tasks from the default file.
     * If the file does not exist, an empty task manager is created.
     * </p>
     */
    public TaskManager() {
        tasks = new HashMap<>();
        loadFromFile();
    }
    
    /**
     * Adds a new task for the specified date.
     * <p>
     * Creates a new Task object and adds it to the collection under the
     * specified date. If this is the first task for that date, a new
     * list is created. A confirmation message is printed to the console.
     * </p>
     * 
     * @param date the date for the task in YYYY-MM-DD format (e.g., "2024-12-25")
     * @param description the textual description of the task
     * @see Task#Task(String, String)
     */
    public void addTask(String date, String description) {
        Task task = new Task(description, date);
        tasks.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
        System.out.println("Added: " + task);
    }
    
    /**
     * Displays all tasks for a specific date to the console.
     * <p>
     * Retrieves and prints all tasks associated with the given date.
     * If no tasks exist for that date, a message indicating this is displayed.
     * </p>
     * 
     * @param date the date to query in YYYY-MM-DD format (e.g., "2024-12-25")
     */
    public void viewTasks(String date) {
        List<Task> dayTasks = tasks.get(date);
        if (dayTasks == null || dayTasks.isEmpty()) {
            System.out.println("No tasks for " + date);
        } else {
            System.out.println("\nTasks for " + date + ":");
            for (Task task : dayTasks) {
                System.out.println("  - " + task.getDescription());
            }
        }
    }
    
    /**
     * Displays all tasks in the system, organized by date.
     * <p>
     * Prints all tasks sorted by date in ascending order. Each date is
     * displayed as a header followed by its associated tasks. If no tasks
     * exist in the system, a message indicating this is displayed.
     * </p>
     */
    public void viewAllTasks() {
        if (tasks.isEmpty()) {
            System.out.println("No tasks stored.");
            return;
        }
        
        System.out.println("\nAll Tasks:");
        List<String> sortedDates = new ArrayList<>(tasks.keySet());
        Collections.sort(sortedDates);

        for (String date : sortedDates) {
            System.out.println("\n" + date + ":");
            for (Task task : tasks.get(date)) {
                System.out.println("  - " + task.getDescription());
            }
        }
    }
    
    /**
     * Saves all tasks to the file specified by {@link #filename}.
     * <p>
     * Writes all tasks to a text file in pipe-delimited format (date|description).
     * Each task is written on a separate line. If an error occurs during saving,
     * an error message is printed to the console.
     * </p>
     * 
     * <p><b>File Format:</b></p>
     * <pre>
     * 2024-12-25|Buy Christmas gifts
     * 2024-12-31|Submit final report
     * </pre>
     * 
     * @see #loadFromFile()
     */
    public void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (String date : tasks.keySet()) {
                for (Task task : tasks.get(date)) {
                    writer.println(date + "|" + task.getDescription());
                }
            }
            System.out.println("Saved to " + filename);
        } catch (IOException e) {
            System.out.println("Error saving: " + e.getMessage());
        }
    }
    
    /**
     * Loads tasks from the file specified by {@link #filename}.
     * <p>
     * Reads tasks from the text file and populates the task map. The file is
     * expected to be in pipe-delimited format (date|description). If the file
     * does not exist, the method returns silently without error. If an error
     * occurs during loading, an error message is printed to the console.
     * </p>
     * 
     * <p>
     * This method is automatically called by the constructor to restore
     * previously saved tasks.
     * </p>
     * 
     * @see #saveToFile()
     */
    public void loadFromFile() {
        File file = new File(filename);
        if (!file.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    String date = parts[0];
                    String description = parts[1];
                    Task task = new Task(description, date);
                    tasks.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
                }
            }
            System.out.println("Loaded tasks from " + filename);
        } catch (IOException e) {
            System.out.println("Error loading: " + e.getMessage());
        }
    }
}