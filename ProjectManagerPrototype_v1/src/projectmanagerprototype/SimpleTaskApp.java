package src.projectmanagerprototype;

import java.util.Scanner;

/**
 * Simple task tracker application with a console-based user interface.
 * <p>
 * This class provides the main entry point for the task tracking system.
 * It presents a menu-driven interface allowing users to add tasks, view tasks
 * by date, view all tasks, and exit the application. All data is automatically
 * saved to a file after each operation.
 * </p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * java projectmanagerprototype.SimpleTaskApp
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
     *   <li>1. Add task - Prompts for date and task description</li>
     *   <li>2. View tasks for a date - Displays all tasks for a specified date</li>
     *   <li>3. View all tasks - Shows all tasks sorted by date</li>
     *   <li>4. Exit - Saves data and terminates the application</li>
     * </ul>
     * </p>
     * 
     * <p>
     * The application automatically saves to file after each operation to ensure
     * data persistence. Date format expected is YYYY-MM-DD (e.g., 2024-12-25).
     * </p>
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        TaskManager manager = new TaskManager();
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n1. Add task");
            System.out.println("2. View tasks for a date");
            System.out.println("3. View all tasks");
            System.out.println("4. Exit");
            System.out.print("Choice: ");
            
            String choice = scanner.nextLine();
            
            if (choice.equals("1")) {
                System.out.print("Enter date (YYYY-MM-DD): ");
                String date = scanner.nextLine();
                System.out.print("Enter task: ");
                String task = scanner.nextLine();
                manager.addTask(date, task);
                manager.saveToFile();
                
            } else if (choice.equals("2")) {
                System.out.print("Enter date (YYYY-MM-DD): ");
                String date = scanner.nextLine();
                manager.viewTasks(date);
                manager.saveToFile();
                
            } else if (choice.equals("3")) {
                manager.viewAllTasks();
                manager.saveToFile();

            } else if (choice.equals("4")) {
                manager.saveToFile();
                System.out.println("Goodbye!");
                break;
            } else {
                System.out.println("Invalid choice");
            }
        }
        
        scanner.close();
    }
}