package projectmanager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Swing-based GUI application for managing tasks.
 * <p>
 * Provides functionality to add, view, filter, sort, and remove tasks.
 * Tasks are persisted to a file via {@link TaskManager} and can be
 * organized by date and tags.
 * </p>
 *
 * @see TaskManager
 * @see Task
 */
public class SimpleTaskApp extends JFrame {

    /** The task manager handling all task operations and file persistence. */
    private TaskManager manager;

    /** The list model backing the displayed task list. */
    private DefaultListModel<String> taskListModel;

    /** The visual list component displaying tasks to the user. */
    private JList<String> taskList;

    private static ImageIcon resizeIcon(ImageIcon icon, int resizedWidth, int resizedHeight) {
        Image img = icon.getImage();
        Image resizedImage = img.getScaledInstance(resizedWidth, resizedHeight, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(resizedImage);
    }
    /**
     * Constructs and initializes the Task Manager application window.
     * <p>
     * Sets up the GUI layout including the task list display and
     * button panel, wires up all action listeners, and loads
     * existing tasks via {@link #refreshAllTasks()}.
     * </p>
     */
    public SimpleTaskApp() {
        manager = new TaskManager();

        setTitle("Task Manager");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout
        setLayout(new BorderLayout());

        // ===== Task List (Center) =====
        taskListModel = new DefaultListModel<>();
        taskList = new JList<>(taskListModel);
        JScrollPane scrollPane = new JScrollPane(taskList);
        add(scrollPane, BorderLayout.CENTER);

        // ===== Button Panel (Left) =====
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(8, 1, 5, 5));

        JButton addBtn = new JButton("Add Task");
        JButton viewDateBtn = new JButton("View Tasks by Date");
        JButton viewAllBtn = new JButton("View All Tasks");
        JButton viewSortedBtn = new JButton("View Sorted Tasks");
        JButton viewTagBtn = new JButton("View Tasks by Tag");
        JButton viewTagsBtn = new JButton("View All Tags");
        JButton removeBtn = new JButton("Remove Task");
        JButton exitBtn = new JButton("Exit");
        
        List<JButton> ButtonList = new ArrayList<>();
        ButtonList.add(addBtn);
        ButtonList.add(viewDateBtn);
        ButtonList.add(viewAllBtn);
        ButtonList.add(viewSortedBtn);
        ButtonList.add(viewTagBtn);
        ButtonList.add(viewTagsBtn);
        ButtonList.add(removeBtn);
        ButtonList.add(exitBtn);
        
        for(JButton button : ButtonList) {
        	button.setBorderPainted(false);
        	button.setContentAreaFilled(false);
        	button.setFocusPainted(false);
        	button.setOpaque(false);
            
            ImageIcon icon = new ImageIcon("Large Button.png");
            ImageIcon darkerIcon = new ImageIcon("Large Button Dark.png");
            ImageIcon scaledIcon = resizeIcon(icon, 200, 50);
            ImageIcon scaledDarkerIcon = resizeIcon(darkerIcon, 200, 50);
            button.setIcon(scaledIcon);
            button.setRolloverIcon(scaledDarkerIcon);
            
            button.setIcon(scaledIcon);
            button.setVerticalTextPosition(SwingConstants.CENTER);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
        }
        
        

        buttonPanel.add(addBtn);
        buttonPanel.add(viewDateBtn);
        buttonPanel.add(viewAllBtn);
        buttonPanel.add(viewSortedBtn);
        buttonPanel.add(viewTagBtn);
        buttonPanel.add(viewTagsBtn);
        buttonPanel.add(removeBtn);
        buttonPanel.add(exitBtn);

        add(buttonPanel, BorderLayout.WEST);

        // ===== Button Actions =====

        addBtn.addActionListener(e -> addTask());
        viewDateBtn.addActionListener(e -> viewByDate());
        viewAllBtn.addActionListener(e -> refreshAllTasks());
        viewSortedBtn.addActionListener(e -> refreshSortedTasks());
        viewTagBtn.addActionListener(e -> viewByTag());
        viewTagsBtn.addActionListener(e -> viewAllTags());
        removeBtn.addActionListener(e -> removeTask());
        exitBtn.addActionListener(e -> {
            manager.saveToFile();
            System.exit(0);
        });

        refreshAllTasks();
    }

    /**
     * Prompts the user to add a new task via input dialogs.
     * <p>
     * Collects a date (in {@code YYYY-MM-DD} format), a description,
     * and optional space-separated tags. Validates that the date is
     * not in the past and is properly formatted. After a successful
     * addition, saves to file and refreshes the display.
     * </p>
     */
    private void addTask() {
        String date = JOptionPane.showInputDialog("Enter date (YYYY-MM-DD):");
        if (date == null) return;

        try {
            LocalDate taskDate = LocalDate.parse(date);
            if (taskDate.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "Cannot add past dates.");
                return;
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format.");
            return;
        }

        String description = JOptionPane.showInputDialog("Enter task description:");
        if (description == null || description.trim().isEmpty()) return;

        String tagInput = JOptionPane.showInputDialog("Enter tags (space separated):");

        if (tagInput != null && !tagInput.trim().isEmpty()) {
            List<String> tags = new ArrayList<>();
            for (String tag : tagInput.split("\\s+")) {
                tags.add(tag.trim());
            }
            manager.addTask(date, description, tags);
        } else {
            manager.addTask(date, description);
        }

        manager.saveToFile();
        refreshAllTasks();
    }

    /**
     * Prompts the user for a date and displays all tasks scheduled for that date.
     * <p>
     * If no tasks exist for the given date, a message indicating so is shown
     * in the task list.
     * </p>
     */
    private void viewByDate() {
        String date = JOptionPane.showInputDialog("Enter date (YYYY-MM-DD):");
        if (date == null) return;

        taskListModel.clear();
        List<Task> tasks = manager.getTasksForDate(date);

        if (tasks == null || tasks.isEmpty()) {
            taskListModel.addElement("No tasks for " + date);
        } else {
            for (Task t : tasks) {
                taskListModel.addElement(t.toString());
            }
        }
    }

    /**
     * Prompts the user for a tag and displays all tasks associated with that tag.
     * <p>
     * If no tasks match the given tag, a message indicating so is shown
     * in the task list.
     * </p>
     */
    private void viewByTag() {
        String tag = JOptionPane.showInputDialog("Enter tag:");
        if (tag == null || tag.trim().isEmpty()) return;

        taskListModel.clear();
        List<Task> tasks = manager.getTasksByTag(tag);

        if (tasks.isEmpty()) {
            taskListModel.addElement("No tasks with tag: " + tag);
        } else {
            for (Task t : tasks) {
                taskListModel.addElement(t.toString());
            }
        }
    }

    /**
     * Displays all unique tags along with their task counts in the task list.
     */
    private void viewAllTags() {
        taskListModel.clear();
        List<String> tags = manager.getAllTagsWithCount();
        for (String tag : tags) {
            taskListModel.addElement(tag);
        }
    }

    /**
     * Presents a selection dialog allowing the user to remove a task.
     * <p>
     * All tasks are displayed sorted by date. The user selects a task
     * from the list, and upon confirmation, the task is removed from
     * the manager, saved to file, and the display is refreshed.
     * </p>
     */
    private void removeTask() {
        // Get all tasks sorted by date so the user can see everything
        List<Task> allTasks = manager.getAllTasksSortedFlat();

        if (allTasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No tasks to remove.");
            return;
        }

        // Build display strings for the selection list
        String[] taskOptions = new String[allTasks.size()];
        for (int i = 0; i < allTasks.size(); i++) {
            taskOptions[i] = (i + 1) + ". " + allTasks.get(i).toString();
        }

        JList<String> selectionList = new JList<>(taskOptions);
        selectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionList.setSelectedIndex(0);

        JScrollPane listScrollPane = new JScrollPane(selectionList);
        listScrollPane.setPreferredSize(new Dimension(450, 200));

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Select a task to remove:"), BorderLayout.NORTH);
        panel.add(listScrollPane, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Remove Task", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            int selectedIndex = selectionList.getSelectedIndex();
            if (selectedIndex >= 0) {
                Task selectedTask = allTasks.get(selectedIndex);
                String date = selectedTask.getDate();

                // Find the task's position within its date group
                List<Task> dateTasks = manager.getTasksForDate(date);
                if (dateTasks != null) {
                    for (int i = 0; i < dateTasks.size(); i++) {
                        if (dateTasks.get(i) == selectedTask) {
                            if (manager.removeTask(date, i + 1)) {
                                manager.saveToFile();
                                refreshAllTasks();
                                JOptionPane.showMessageDialog(this, "Task removed successfully.");
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Refreshes the task list display with all tasks in their natural order.
     */
    private void refreshAllTasks() {
        taskListModel.clear();
        for (Task t : manager.getAllTasksFlat()) {
            taskListModel.addElement(t.toString());
        }
    }

    /**
     * Refreshes the task list display with all tasks sorted by date.
     */
    private void refreshSortedTasks() {
        taskListModel.clear();
        for (Task t : manager.getAllTasksSortedFlat()) {
            taskListModel.addElement(t.toString());
        }
    }

    /**
     * Application entry point. Launches the Task Manager GUI on the
     * Swing event dispatch thread.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SimpleTaskApp().setVisible(true);
        });
    }
}