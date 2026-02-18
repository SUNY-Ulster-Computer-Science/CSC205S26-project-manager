package projectmanager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class SimpleTaskApp extends JFrame {

    private TaskManager manager;
    private DefaultListModel<String> taskListModel;
    private JList<String> taskList;

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

    private void viewAllTags() {
        taskListModel.clear();
        List<String> tags = manager.getAllTagsWithCount();
        for (String tag : tags) {
            taskListModel.addElement(tag);
        }
    }

    private void removeTask() {
        String date = JOptionPane.showInputDialog("Enter date (YYYY-MM-DD):");
        if (date == null) return;

        List<Task> tasks = manager.getTasksForDate(date);
        if (tasks == null || tasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No tasks found.");
            return;
        }

        String input = JOptionPane.showInputDialog("Enter task number to remove (1-" + tasks.size() + "):");
        try {
            int number = Integer.parseInt(input);
            if (manager.removeTask(date, number)) {
                manager.saveToFile();
                refreshAllTasks();
            }
        } catch (Exception ignored) {}
    }

    private void refreshAllTasks() {
        taskListModel.clear();
        for (Task t : manager.getAllTasksFlat()) {
            taskListModel.addElement(t.toString());
        }
    }

    private void refreshSortedTasks() {
        taskListModel.clear();
        for (Task t : manager.getAllTasksSortedFlat()) {
            taskListModel.addElement(t.toString());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SimpleTaskApp().setVisible(true);
        });
    }
}
