package projectmanager;

import java.util.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
 * <pre>date|description|tag1,tag2,tag3</pre>
 * Example: <code>2024-12-25|Buy Christmas gifts|shopping,personal</code>
 * </p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * TaskManager manager = new TaskManager();
 * manager.addTask("2024-12-31", "Submit final report", Arrays.asList("work", "urgent"));
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
     * Adds a new task for the specified date without tags.
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
     * Adds a new task for the specified date with tags.
     * <p>
     * Creates a new Task object with tags and adds it to the collection under the
     * specified date. If this is the first task for that date, a new
     * list is created. A confirmation message is printed to the console.
     * </p>
     * 
     * @param date the date for the task in YYYY-MM-DD format (e.g., "2024-12-25")
     * @param description the textual description of the task
     * @param tags list of tags to categorize the task
     * @see Task#Task(String, String, List)
     */
    public void addTask(String date, String description, List<String> tags) {
        Task task = new Task(description, date, tags);
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
                System.out.println("  - " + task.getDescription() + 
                    (task.getTags().isEmpty() ? "" : " [" + String.join(", ", task.getTags()) + "]"));
            }
        }
    }
    
    /**
     * Displays all tasks in the system, organized and sorted by date.
     * <p>
     * Prints all tasks sorted by date in ascending order. Each date is
     * displayed as a header with the weekday, followed by its associated tasks. 
     * If no tasks exist in the system, a message indicating this is displayed.
     * </p>
     */
    public void viewAllTasksSorted() {
        if (tasks.isEmpty()) {
            System.out.println("No tasks stored.");
            return;
        }
        
        System.out.println("\n=== All Tasks (Sorted by Date) ===");
        List<String> sortedDates = new ArrayList<>(tasks.keySet());
        Collections.sort(sortedDates);

        for (String date : sortedDates) {
            String weekday = getWeekday(date);
            System.out.println("\n" + date + " (" + weekday + "):");
            for (Task task : tasks.get(date)) {
                System.out.println("  - " + task.getDescription() + 
                    (task.getTags().isEmpty() ? "" : " [" + String.join(", ", task.getTags()) + "]"));
            }
        }
    }
    
 // Return all tasks as a flat list
    public List<Task> getAllTasksFlat() {
        List<Task> all = new ArrayList<>();
        for (List<Task> list : tasks.values()) {
            all.addAll(list);
        }
        return all;
    }

    // Return all tasks sorted by date
    public List<Task> getAllTasksSortedFlat() {
        List<String> sortedDates = new ArrayList<>(tasks.keySet());
        Collections.sort(sortedDates);

        List<Task> result = new ArrayList<>();
        for (String date : sortedDates) {
            result.addAll(tasks.get(date));
        }
        return result;
    }

    // Return tasks by tag
    public List<Task> getTasksByTag(String tag) {
        List<Task> result = new ArrayList<>();
        for (List<Task> list : tasks.values()) {
            for (Task t : list) {
                if (t.hasTag(tag)) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    // Return all tags with counts
    public List<String> getAllTagsWithCount() {
        Map<String, Integer> tagCount = new TreeMap<>();

        for (List<Task> list : tasks.values()) {
            for (Task t : list) {
                for (String tag : t.getTags()) {
                    tagCount.put(tag, tagCount.getOrDefault(tag, 0) + 1);
                }
            }
        }

        List<String> result = new ArrayList<>();
        for (String tag : tagCount.keySet()) {
            int count = tagCount.get(tag);
            result.add(tag + " (" + count + ")");
        }
        return result;
    }

    /**
     * Displays all tasks that have a specific tag.
     * <p>
     * Searches through all tasks and displays those that contain the specified tag.
     * Tasks are grouped by date and sorted chronologically.
     * </p>
     * 
     * @param tag the tag to search for
     */
    public void viewTasksByTag(String tag) {
        boolean found = false;
        System.out.println("\n=== Tasks with tag: " + tag + " ===");
        
        List<String> sortedDates = new ArrayList<>(tasks.keySet());
        Collections.sort(sortedDates);

        for (String date : sortedDates) {
            List<Task> taggedTasks = new ArrayList<>();
            for (Task task : tasks.get(date)) {
                if (task.hasTag(tag)) {
                    taggedTasks.add(task);
                }
            }
            
            if (!taggedTasks.isEmpty()) {
                found = true;
                String weekday = getWeekday(date);
                System.out.println("\n" + date + " (" + weekday + "):");
                for (Task task : taggedTasks) {
                    System.out.println("  - " + task.getDescription() + 
                        " [" + String.join(", ", task.getTags()) + "]");
                }
            }
        }
        
        if (!found) {
            System.out.println("No tasks found with tag: " + tag);
        }
    }

    /**
     * Displays all unique tags used across all tasks.
     */
    public void viewAllTags() {
        Set<String> allTags = new TreeSet<>();
        
        for (List<Task> taskList : tasks.values()) {
            for (Task task : taskList) {
                allTags.addAll(task.getTags());
            }
        }
        
        if (allTags.isEmpty()) {
            System.out.println("\nNo tags in use.");
        } else {
            System.out.println("\n=== All Tags ===");
            for (String tag : allTags) {
                int count = countTasksWithTag(tag);
                System.out.println("  - " + tag + " (" + count + " task" + (count != 1 ? "s" : "") + ")");
            }
        }
    }

    /**
     * Counts the number of tasks that have a specific tag.
     * 
     * @param tag the tag to count
     * @return the number of tasks with this tag
     */
    private int countTasksWithTag(String tag) {
        int count = 0;
        for (List<Task> taskList : tasks.values()) {
            for (Task task : taskList) {
                if (task.hasTag(tag)) {
                    count++;
                }
            }
        }
        return count;
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
     * Converts a date string to its corresponding weekday name.
     * <p>
     * Takes a date in YYYY-MM-DD format and returns the day of the week
     * (e.g., "Monday", "Tuesday"). If the date format is invalid, returns
     * "Invalid Date".
     * </p>
     * 
     * @param dateStr the date string in YYYY-MM-DD format (e.g., "2024-12-25")
     * @return the weekday name (e.g., "Wednesday") or "Invalid Date" if parsing fails
     */
    private String getWeekday(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.getDayOfWeek().toString().charAt(0) + 
                   date.getDayOfWeek().toString().substring(1).toLowerCase();
        } catch (DateTimeParseException e) {
            return "Invalid Date";
        }
    }
    
    /**
     * Saves all tasks to the file specified by {@link #filename}.
     * <p>
     * Writes all tasks to a text file in pipe-delimited format (date|description|tags).
     * Each task is written on a separate line. If an error occurs during saving,
     * an error message is printed to the console.
     * </p>
     * 
     * <p><b>File Format:</b></p>
     * <pre>
     * 2024-12-25|Buy Christmas gifts|shopping,personal
     * 2024-12-31|Submit final report|work,urgent
     * </pre>
     * 
     * @see #loadFromFile()
     */
    public void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (String date : tasks.keySet()) {
                for (Task task : tasks.get(date)) {
                    writer.println(date + "|" + task.getDescription() + "|" + task.getTagsAsString());
                }
            }
            System.out.println("Saved to " + filename);
        } catch (IOException e) {
            System.out.println("Error saving: " + e.getMessage());
        }
    }
    
    /**
     * Removes a task for a given date by its index (1-based).
     *
     * @param date the date of the task (YYYY-MM-DD)
     * @param taskNumber the task number shown to the user (1-based index)
     * @return true if removed successfully, false otherwise
     */
    public boolean removeTask(String date, int taskNumber) {
        List<Task> dayTasks = tasks.get(date);

        if (dayTasks == null || dayTasks.isEmpty()) {
            return false;
        }

        if (taskNumber < 1 || taskNumber > dayTasks.size()) {
            return false;
        }

        Task removed = dayTasks.remove(taskNumber - 1);
        System.out.println("Removed: " + removed);

        // If no tasks remain for that date, remove the date entry
        if (dayTasks.isEmpty()) {
            tasks.remove(date);
        }

        return true;
    }
    
    /**
     * Returns tasks for a specific date.
     *
     * @param date the date in YYYY-MM-DD format
     * @return list of tasks or null if none exist
     */
    public List<Task> getTasksForDate(String date) {
        return tasks.get(date);
    }
    
    /**
     * Loads tasks from the file specified by {@link #filename}.
     * <p>
     * Reads tasks from the text file and populates the task map. The file is
     * expected to be in pipe-delimited format (date|description|tags). If the file
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
                String[] parts = line.split("\\|", 3);
                if (parts.length >= 2) {
                    String date = parts[0];
                    String description = parts[1];
                    List<String> tags = new ArrayList<>();
                    
                    if (parts.length == 3 && !parts[2].trim().isEmpty()) {
                        String[] tagArray = parts[2].split(",");
                        for (String tag : tagArray) {
                            String trimmedTag = tag.trim();
                            if (!trimmedTag.isEmpty()) {
                                tags.add(trimmedTag);
                            }
                        }
                    }
                    
                    Task task = new Task(description, date, tags);
                    tasks.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
                }
            }
            System.out.println("Loaded tasks from " + filename);
        } catch (IOException e) {
            System.out.println("Error loading: " + e.getMessage());
        }
    }
}