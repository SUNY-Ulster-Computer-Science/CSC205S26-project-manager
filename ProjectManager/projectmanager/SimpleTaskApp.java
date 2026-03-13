package projectmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static projectmanager.ThemeColors.*;

/**
 * Swing-based GUI for managing tasks with the Orange™ visual theme.
 *
 * <p>Users can add tasks (with a name, due date, optional tags, and description),
 * browse them in a scrollable list, filter by date or tag, view them sorted
 * chronologically, inspect all tags, and remove individual tasks. Every
 * mutation is persisted to disk through {@link TaskManager#saveToFile()}.</p>
 *
 * <h3>Layout</h3>
 * <ul>
 *   <li>An outer orange border surrounds a rounded inner peach panel.</li>
 *   <li>A seven-button toolbar spans the top of the inner panel.</li>
 *   <li>A cream-colored {@link JList} fills the remaining space below.</li>
 * </ul>
 *
 * @see TaskManager
 * @see Task
 */
public class SimpleTaskApp extends JFrame {

    // ── Core application state ───────────────────────────────────────────

    /** Handles task CRUD operations and file-based persistence. */
    private TaskManager manager;

    /** Backing model for the on-screen task list. */
    private DefaultListModel<String> taskListModel;

    /** Visible list component that renders {@link #taskListModel}. */
    private JList<String> taskList;

    /** Background image painted behind every dialog (loaded from {@code Small Frame.png}). */
    private ImageIcon dialogBackground;

    // ══════════════════════════════════════════════════════════════════════
    //  Dialog helpers
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Recursively sets every {@link JPanel} and {@link JLabel} in the
     * component tree to non-opaque so that the dialog background image
     * shows through.
     *
     * <p>{@link JScrollPane} is intentionally skipped because its viewport
     * must remain opaque for list-selection highlight colors to render.</p>
     *
     * @param comp root of the subtree to make transparent
     */
    private void makeTransparent(Component comp) {
        if (comp instanceof JPanel) ((JPanel) comp).setOpaque(false);
        else if (comp instanceof JLabel) ((JLabel) comp).setOpaque(false);

        if (comp instanceof Container && !(comp instanceof JScrollPane)) {
            for (Component child : ((Container) comp).getComponents()) {
                makeTransparent(child);
            }
        }
    }

    /**
     * Creates an undecorated, fixed-size, modal dialog whose content pane
     * is a {@link BackgroundPanel} painted with {@link #dialogBackground}.
     *
     * @param title  logical title (not displayed because the dialog is undecorated)
     * @param width  dialog width in pixels
     * @param height dialog height in pixels
     * @return the configured but not-yet-visible dialog
     */
    private JDialog createThemedDialog(String title, int width, int height) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setUndecorated(true);

        BackgroundPanel bgPanel = new BackgroundPanel(
                dialogBackground != null ? dialogBackground.getImage() : null);
        bgPanel.setLayout(new BorderLayout());
        dialog.setContentPane(bgPanel);

        return dialog;
    }

    /**
     * Displays a themed single-line input dialog with OK / Cancel buttons.
     *
     * @param message prompt text shown above the text field
     * @return the entered string, or {@code null} if the user cancelled
     */
    private String showThemedInputDialog(String message) {
        int dialogW = 300;
        int dialogH = (int) (dialogW * (994.0 / 628.0));
        JDialog dialog = createThemedDialog("Input", dialogW, dialogH);
        final String[] result = {null};

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(
                (int)(dialogH * 0.12), 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Prompt label
        gbc.gridy = 0;
        JLabel label = new JLabel(message);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(TEXT_DARK);
        label.setOpaque(false);
        content.add(label, gbc);

        // Text input
        gbc.gridy = 1;
        RoundedTextField textField = new RoundedTextField(20);
        content.add(textField, gbc);

        // OK / Cancel buttons
        gbc.gridy = 2;
        gbc.insets = new Insets(20, 5, 5, 5);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);

        OrangeButton okBtn     = new OrangeButton("OK",     BUTTON_ORANGE, BUTTON_HOVER);
        OrangeButton cancelBtn = new OrangeButton("Cancel", BUTTON_ORANGE, BUTTON_HOVER);

        okBtn.addActionListener(e     -> { result[0] = textField.getText(); dialog.dispose(); });
        cancelBtn.addActionListener(e -> dialog.dispose());
        textField.addActionListener(e -> { result[0] = textField.getText(); dialog.dispose(); });

        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);
        content.add(btnPanel, gbc);

        dialog.getContentPane().add(content, BorderLayout.CENTER);
        dialog.setVisible(true);
        return result[0];
    }

    /**
     * Displays a themed message dialog with a single OK button.
     * The message may contain simple HTML for line breaks, etc.
     *
     * @param message the text (or HTML fragment) to display
     */
    private void showThemedMessageDialog(String message) {
        int dialogW = 280;
        int dialogH = (int) (dialogW * (994.0 / 628.0));
        JDialog dialog = createThemedDialog("Message", dialogW, dialogH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(
                (int)(dialogH * 0.12), 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;

        // Message text (centered, supports HTML)
        gbc.gridy = 0;
        JLabel label = new JLabel("<html><center>" + message + "</center></html>");
        label.setFont(new Font("SansSerif", Font.BOLD, 15));
        label.setForeground(TEXT_DARK);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setOpaque(false);
        content.add(label, gbc);

        // OK button
        gbc.gridy = 1;
        gbc.insets = new Insets(25, 5, 5, 5);
        OrangeButton okBtn = new OrangeButton("OK", BUTTON_ORANGE, BUTTON_HOVER);
        okBtn.addActionListener(e -> dialog.dispose());
        content.add(okBtn, gbc);

        dialog.getContentPane().add(content, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    /**
     * Displays a themed confirmation dialog containing arbitrary content
     * and OK / Cancel buttons.
     *
     * @param innerContent the component to embed in the dialog body
     * @param title        logical dialog title
     * @return {@link JOptionPane#OK_OPTION} or {@link JOptionPane#CANCEL_OPTION}
     */
    private int showThemedConfirmDialog(Component innerContent, String title) {
        int dialogW = 360;
        int dialogH = (int) (dialogW * (994.0 / 628.0));
        JDialog dialog = createThemedDialog(title, dialogW, dialogH);
        final int[] result = {JOptionPane.CANCEL_OPTION};

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(
                (int)(dialogH * 0.12), 20, 20, 20));

        makeTransparent(innerContent);
        content.add(innerContent, BorderLayout.CENTER);

        // OK / Cancel buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        OrangeButton okBtn     = new OrangeButton("OK",     BUTTON_ORANGE, BUTTON_HOVER);
        OrangeButton cancelBtn = new OrangeButton("Cancel", BUTTON_ORANGE, BUTTON_HOVER);

        okBtn.addActionListener(e     -> { result[0] = JOptionPane.OK_OPTION; dialog.dispose(); });
        cancelBtn.addActionListener(e -> dialog.dispose());

        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);
        content.add(btnPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(content, BorderLayout.CENTER);
        dialog.setVisible(true);
        return result[0];
    }

    /**
     * Displays the "Add Task" form dialog with fields for Name, Date, Tag,
     * and a multi-line Description area.
     *
     * @return a four-element array {@code [name, date, tag, description]},
     *         or {@code null} if the user cancelled
     */
    private String[] showAddTaskDialog() {
        int dialogW = 380;
        int dialogH = (int) (dialogW * (994.0 / 628.0));
        JDialog dialog = createThemedDialog("Add Task", dialogW, dialogH);
        final String[][] result = {null};

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(
                (int)(dialogH * 0.12), 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // ── Name row ─────────────────────────────────────────────────────
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        content.add(new PillLabel("Name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        RoundedTextField nameField = new RoundedTextField(15);
        content.add(nameField, gbc);

        // ── Date row ─────────────────────────────────────────────────────
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        content.add(new PillLabel("Date:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        RoundedTextField dateField = new RoundedTextField(15);
        content.add(dateField, gbc);

        // ── Tag row ──────────────────────────────────────────────────────
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        content.add(new PillLabel("Tag:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        RoundedTextField tagField = new RoundedTextField(15);
        content.add(tagField, gbc);

        // ── Description header banner ────────────────────────────────────
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 5, 0, 5);

        JPanel descHeader = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(INNER_PEACH);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 10, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        descHeader.setOpaque(false);
        descHeader.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel descLabel = new JLabel("Description");
        descLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        descLabel.setForeground(TEXT_DARK);
        descHeader.add(descLabel);
        content.add(descHeader, gbc);

        // ── Description text area ────────────────────────────────────────
        gbc.gridy = 4;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 5, 5, 5);
        RoundedTextArea descArea = new RoundedTextArea(5, 20);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setOpaque(false);
        descScroll.getViewport().setOpaque(false);
        descScroll.setBorder(BorderFactory.createEmptyBorder());
        content.add(descScroll, gbc);

        // ── Create / Cancel buttons ──────────────────────────────────────
        gbc.gridy = 5;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPanel.setOpaque(false);

        OrangeButton createBtn = new OrangeButton("Create", BUTTON_ORANGE, BUTTON_HOVER);
        OrangeButton cancelBtn = new OrangeButton("Cancel", BUTTON_ORANGE, BUTTON_HOVER);

        createBtn.addActionListener(e -> {
            result[0] = new String[]{
                    nameField.getText(),
                    dateField.getText(),
                    tagField.getText(),
                    descArea.getText()
            };
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        btnPanel.add(createBtn);
        btnPanel.add(cancelBtn);
        content.add(btnPanel, gbc);

        dialog.getContentPane().add(content, BorderLayout.CENTER);
        dialog.setVisible(true);
        return result[0];
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Builds the main application window: initializes the {@link TaskManager},
     * assembles the toolbar and task list, wires up button actions, and
     * populates the list with any previously saved tasks.
     */
    public SimpleTaskApp() {
        manager = new TaskManager();
        dialogBackground = new ImageIcon("Small Frame.png");

        setTitle("Orange\u2122 Task Manager");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Outer solid-orange border that frames the entire window
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(OUTER_ORANGE);
        outerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setContentPane(outerPanel);

        // Inner rounded peach card that holds all content
        RoundedPanel innerPanel = new RoundedPanel(INNER_PEACH, 30);
        innerPanel.setLayout(new BorderLayout(0, 15));
        innerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        outerPanel.add(innerPanel, BorderLayout.CENTER);

        // ── Toolbar ──────────────────────────────────────────────────────
        JPanel buttonBar = new JPanel(new GridLayout(1, 7, 6, 0));
        buttonBar.setOpaque(false);

        OrangeButton addBtn        = new OrangeButton("Add Task",             BUTTON_ORANGE, BUTTON_HOVER);
        OrangeButton viewDateBtn   = new OrangeButton("View Tasks by Date",   BUTTON_ORANGE, BUTTON_HOVER);
        OrangeButton viewAllBtn    = new OrangeButton("View All Tasks",       BUTTON_ORANGE, BUTTON_HOVER);
        OrangeButton viewSortedBtn = new OrangeButton("View Sorted Tasks",    BUTTON_ORANGE, BUTTON_HOVER);
        OrangeButton viewTagBtn    = new OrangeButton("View Tasks by Tag",    BUTTON_ORANGE, BUTTON_HOVER);
        OrangeButton viewTagsBtn   = new OrangeButton("View All Tags",        BUTTON_ORANGE, BUTTON_HOVER);
        OrangeButton removeBtn     = new OrangeButton("Remove Task",          BUTTON_ORANGE, BUTTON_HOVER);

        buttonBar.add(addBtn);
        buttonBar.add(viewDateBtn);
        buttonBar.add(viewAllBtn);
        buttonBar.add(viewSortedBtn);
        buttonBar.add(viewTagBtn);
        buttonBar.add(viewTagsBtn);
        buttonBar.add(removeBtn);

        innerPanel.add(buttonBar, BorderLayout.NORTH);

        // ── Task list ────────────────────────────────────────────────────
        taskListModel = new DefaultListModel<>();
        taskList = new JList<>(taskListModel);
        taskList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        taskList.setForeground(TEXT_DARK);
        taskList.setBackground(CONTENT_CREAM);
        taskList.setSelectionBackground(LABEL_BG);
        taskList.setSelectionForeground(Color.WHITE);
        taskList.setFixedCellHeight(32);
        taskList.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CONTENT_CREAM);

        innerPanel.add(scrollPane, BorderLayout.CENTER);

        // ── Wire button actions ──────────────────────────────────────────
        addBtn.addActionListener(e        -> addTask());
        viewDateBtn.addActionListener(e   -> viewByDate());
        viewAllBtn.addActionListener(e    -> refreshAllTasks());
        viewSortedBtn.addActionListener(e -> refreshSortedTasks());
        viewTagBtn.addActionListener(e    -> viewByTag());
        viewTagsBtn.addActionListener(e   -> viewAllTags());
        removeBtn.addActionListener(e     -> removeTask());

        // Persist tasks when the window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                manager.saveToFile();
            }
        });

        refreshAllTasks();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Task operations (each method backs a toolbar button)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Opens the Add Task form, validates input, creates a new {@link Task},
     * saves it, and refreshes the list.
     */
    private void addTask() {
        String[] fields = showAddTaskDialog();
        if (fields == null) return;

        String name        = fields[0].trim();
        String date        = fields[1].trim();
        String tags        = fields[2].trim();
        String description = fields[3].trim();

        if (date.isEmpty()) {
            showThemedMessageDialog("Date is required.");
            return;
        }

        try {
            LocalDate taskDate = LocalDate.parse(date);
            if (taskDate.isBefore(LocalDate.now())) {
                showThemedMessageDialog("Cannot add past dates.");
                return;
            }
        } catch (DateTimeParseException e) {
            showThemedMessageDialog("Invalid date format.<br>Use YYYY-MM-DD.");
            return;
        }

        String taskDescription = description.isEmpty() ? name
                : (name.isEmpty() ? description : name + " - " + description);
        if (taskDescription.isEmpty()) {
            showThemedMessageDialog("Please enter a name<br>or description.");
            return;
        }

        if (!tags.isEmpty()) {
            List<String> tagList = new ArrayList<>();
            for (String tag : tags.split("\\s+")) {
                tagList.add(tag.trim());
            }
            manager.addTask(date, taskDescription, tagList);
        } else {
            manager.addTask(date, taskDescription);
        }

        manager.saveToFile();
        refreshAllTasks();
    }

    /**
     * Prompts for a date string and replaces the list contents with only
     * the tasks scheduled for that date.
     */
    private void viewByDate() {
        String date = showThemedInputDialog("Enter date (YYYY-MM-DD):");
        if (date == null) return;

        taskListModel.clear();
        List<Task> tasks = manager.getTasksForDate(date);

        if (tasks == null || tasks.isEmpty()) {
            taskListModel.addElement("No tasks for " + date);
        } else {
            for (Task t : tasks) taskListModel.addElement(t.toString());
        }
    }

    /**
     * Prompts for a tag and replaces the list contents with only the tasks
     * carrying that tag.
     */
    private void viewByTag() {
        String tag = showThemedInputDialog("Enter tag:");
        if (tag == null || tag.trim().isEmpty()) return;

        taskListModel.clear();
        List<Task> tasks = manager.getTasksByTag(tag);

        if (tasks.isEmpty()) {
            taskListModel.addElement("No tasks with tag: " + tag);
        } else {
            for (Task t : tasks) taskListModel.addElement(t.toString());
        }
    }

    /**
     * Replaces the list contents with every known tag and its task count.
     */
    private void viewAllTags() {
        taskListModel.clear();
        List<String> tags = manager.getAllTagsWithCount();
        for (String tag : tags) taskListModel.addElement(tag);
    }

    /**
     * Presents a selection dialog listing every task. On confirmation the
     * chosen task is deleted, persisted, and the list is refreshed.
     */
    private void removeTask() {
        List<Task> allTasks = manager.getAllTasksSortedFlat();

        if (allTasks.isEmpty()) {
            showThemedMessageDialog("No tasks to remove.");
            return;
        }

        String[] taskOptions = new String[allTasks.size()];
        for (int i = 0; i < allTasks.size(); i++) {
            taskOptions[i] = (i + 1) + ". " + allTasks.get(i).toString();
        }

        JList<String> selectionList = new JList<>(taskOptions);
        selectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionList.setSelectedIndex(0);
        selectionList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        selectionList.setForeground(TEXT_DARK);
        selectionList.setBackground(CONTENT_CREAM);
        selectionList.setOpaque(true);
        selectionList.setSelectionBackground(LABEL_BG);
        selectionList.setSelectionForeground(Color.WHITE);

        JScrollPane listScrollPane = new JScrollPane(selectionList);
        listScrollPane.setPreferredSize(new Dimension(300, 250));
        listScrollPane.setOpaque(true);
        listScrollPane.getViewport().setOpaque(true);
        listScrollPane.getViewport().setBackground(CONTENT_CREAM);
        listScrollPane.setBorder(BorderFactory.createLineBorder(LABEL_BG, 1));

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);
        JLabel headerLabel = new JLabel("Select a task to remove:");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        headerLabel.setForeground(TEXT_DARK);
        headerLabel.setOpaque(false);
        panel.add(headerLabel, BorderLayout.NORTH);
        panel.add(listScrollPane, BorderLayout.CENTER);

        int result = showThemedConfirmDialog(panel, "Remove Task");

        if (result == JOptionPane.OK_OPTION) {
            int selectedIndex = selectionList.getSelectedIndex();
            if (selectedIndex >= 0) {
                Task selectedTask = allTasks.get(selectedIndex);
                String date = selectedTask.getDate();

                List<Task> dateTasks = manager.getTasksForDate(date);
                if (dateTasks != null) {
                    for (int i = 0; i < dateTasks.size(); i++) {
                        if (dateTasks.get(i) == selectedTask) {
                            if (manager.removeTask(date, i + 1)) {
                                manager.saveToFile();
                                refreshAllTasks();
                                showThemedMessageDialog("Task removed successfully.");
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /** Replaces the list contents with every task in insertion order. */
    private void refreshAllTasks() {
        taskListModel.clear();
        for (Task t : manager.getAllTasksFlat()) taskListModel.addElement(t.toString());
    }

    /** Replaces the list contents with every task sorted chronologically. */
    private void refreshSortedTasks() {
        taskListModel.clear();
        for (Task t : manager.getAllTasksSortedFlat()) taskListModel.addElement(t.toString());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Entry point
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Launches the Orange™ Task Manager on the Swing Event Dispatch Thread.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SimpleTaskApp().setVisible(true);
        });
    }
}