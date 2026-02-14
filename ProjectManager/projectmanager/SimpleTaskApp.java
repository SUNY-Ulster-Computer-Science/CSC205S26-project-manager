package projectmanager;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Simple task tracker application with a console-based user interface.
 * <p>
 * This class provides the main entry point for the task tracking system.
 * It presents a menu-driven interface allowing users to add tasks with tags, 
 * view tasks by date, view all tasks (sorted and unsorted), filter by tags, 
 * view tag statistics, and exit the application. All data is automatically 
 * saved to a file after each operation.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * java projectmanager.SimpleTaskApp
 * </pre>
 *
 * @author Team Orange
 * @version 1.0
 * @see TaskManager
 * @see Task
 */
public class SimpleTaskApp {

    /**
     * Main entry point for the task tracker application.
     * <p>
     * Initializes the task manager and presents an interactive console menu
     * with the following options:
     * <ul>
     *   <li>1. Add task - Prompts for date, task description, and optional tags</li>
     *   <li>2. View tasks for a date - Displays all tasks for a specified date</li>
     *   <li>3. View all tasks - Shows all tasks in insertion order</li>
     *   <li>4. View sorted tasks - Shows all tasks sorted by date with weekdays</li>
     *   <li>5. View tasks by tag - Filter and display tasks with a specific tag</li>
     *   <li>6. View all tags - Display all tags in use with task counts</li>
     *   <li>7. Exit - Saves data and terminates the application</li>
     * </ul>
     * </p>
     *
     * <p>
     * The application automatically saves to file after each operation to ensure
     * data persistence. Date format expected is YYYY-MM-DD (e.g., 2024-12-25).
     * Only current or future dates are allowed when adding new tasks; past dates
     * will be rejected. Task names can contain spaces and special characters. 
     * Tags should be space-separated when entering multiple tags (e.g., "urgent work personal").
     * Task names can contain spaces and special characters. Tags should be 
     * space-separated when entering multiple tags (e.g., "urgent work personal").
     * </p>
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        TaskManager manager = new TaskManager();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Task Manager ===");
            System.out.println("1. Add task");
            System.out.println("2. View tasks for a date");
            System.out.println("3. View all tasks");
            System.out.println("4. View sorted tasks by date");
            System.out.println("4. View sorted tasks");
            System.out.println("5. View tasks by tag");
            System.out.println("6. View all tags");
            System.out.println("7. Exit");
            System.out.print("Choice: ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                System.out.print("Enter date (YYYY-MM-DD): ");
                String date = scanner.nextLine().trim();
                
                // Validate date format and ensure it's not in the past
                try {
                    LocalDate taskDate = LocalDate.parse(date);
                    LocalDate today = LocalDate.now();
                    
                    if (taskDate.isBefore(today)) {
                        System.out.println("Error: Cannot add tasks for past dates. Please enter today's date or a future date.");
                        continue;
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("Error: Invalid date format. Please use YYYY-MM-DD format.");
                    continue;
                }
                
                System.out.print("Enter task: ");
                String task = scanner.nextLine().trim();
                
                if (!task.isEmpty()) {
                    System.out.print("Enter tags (space-separated, press Enter to skip): ");
                    String tagInput = scanner.nextLine().trim();
                    
                    if (!tagInput.isEmpty()) {
                        List<String> tags = new ArrayList<>();
                        String[] tagArray = tagInput.split("\\s+");
                        for (String tag : tagArray) {
                            String trimmedTag = tag.trim();
                            if (!trimmedTag.isEmpty()) {
                                tags.add(trimmedTag);
                            }
                        }
                        manager.addTask(date, task, tags);
                    } else {
                        manager.addTask(date, task);
                    }
                    
                    manager.saveToFile();
                    System.out.println("Task added successfully!");
                } else {
                    System.out.println("Task cannot be empty.");
                }

            } else if (choice.equals("2")) {
                System.out.print("Enter date (YYYY-MM-DD): ");
                String date = scanner.nextLine().trim();
                manager.viewTasks(date);
                manager.saveToFile();

            } else if (choice.equals("3")) {
                manager.viewAllTasks();
                manager.saveToFile();
                
            } else if (choice.equals("4")) {
                manager.viewAllTasksSorted();
                manager.saveToFile();

            } else if (choice.equals("5")) {
                System.out.print("Enter tag to filter by: ");
                String tag = scanner.nextLine().trim();
                if (!tag.isEmpty()) {
                    manager.viewTasksByTag(tag);
                } else {
                    System.out.println("Tag cannot be empty.");
                }
                manager.saveToFile();

            } else if (choice.equals("6")) {
                manager.viewAllTags();
                manager.saveToFile();

            } else if (choice.equals("7")) {
                manager.saveToFile();
                System.out.println("Goodbye!");
                break;
            } else {
                System.out.println("Invalid choice. Please enter 1, 2, 3, 4, 5, 6, or 7.");
            }
        }

        scanner.close();
    }
}