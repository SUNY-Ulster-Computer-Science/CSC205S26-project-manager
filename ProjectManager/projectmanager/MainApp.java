package projectmanager;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;

import static projectmanager.ThemeColors.*;

/**
 * Root application window for the Orange™ Task Manager.
 *
 * <p>{@code MainApp} is a {@link JFrame} that assembles the entire UI, owns the
 * {@link TaskManager} singleton, and coordinates interactions between the
 * calendar sidebar, the task list, and the add/edit dialogs.</p>
 *
 * <h3>Layout overview</h3>
 * <pre>
 * JFrame (OUTER_ORANGE border)
 * └── outerPanel  (BorderLayout)
 *     ├── NORTH: title bar  (app icon, title, search field)
 *     └── CENTER: innerPanel  (RoundedPanel, INNER_PEACH)
 *         └── JSplitPane (horizontal)
 *             ├── LEFT:  calendarPanel + summaryCard
 *             └── RIGHT: task list header + scrollable TaskCard list
 * </pre>
 *
 * <h3>Auto-refresh</h3>
 * <p>A 60-second Swing timer calls {@link #refreshTaskPanel()} periodically so
 * that countdown badges stay up-to-date without any user interaction.</p>
 *
 * <h3>Persistence</h3>
 * <p>Tasks are saved via {@link TaskManager#saveToFile()} whenever the user
 * creates, edits, or deletes a task, and also when the window is closed.</p>
 */
public class MainApp extends JFrame {

    // -----------------------------------------------------------------------
    // Core state
    // -----------------------------------------------------------------------

    /** Data layer; loaded from disk on construction. */
    private final TaskManager manager      = new TaskManager();

    /** The date whose tasks are currently shown in the right panel. */
    private       LocalDate   selectedDate = LocalDate.now();

    // -----------------------------------------------------------------------
    // UI components
    // -----------------------------------------------------------------------

    /** Left-side month-view calendar; also updates {@link #selectedDate}. */
    private CalendarPanel    calendarPanel;

    /** Vertical box panel that holds the {@link TaskCard} rows. */
    private JPanel           taskListPanel;

    /**
     * Header label above the task list; shows the selected date or the current
     * search query.
     */
    private JLabel           dateHeaderLabel;

    /** Summary label in the lower-left card showing global task statistics. */
    private JLabel           summaryLabel;

    /** Scroll pane wrapping {@link #taskListPanel}. */
    private JScrollPane      taskScrollPane;

    /** Search field in the title bar; triggers live search on every keystroke. */
    private RoundedTextField searchField;

    /** Timer that fires every 60 seconds to refresh countdown badges. */
    private javax.swing.Timer countdownTimer;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs and initialises the main application window.
     *
     * <p>Performs the following steps:</p>
     * <ol>
     *   <li>Configures the frame (title, size, close behaviour).</li>
     *   <li>Registers a {@link WindowAdapter} to save tasks on close.</li>
     *   <li>Builds the entire UI via {@link #buildUI()}.</li>
     *   <li>Populates the task list for today via {@link #refreshTaskPanel()}.</li>
     *   <li>Starts the 60-second countdown refresh timer.</li>
     * </ol>
     */
    public MainApp() {
        setTitle("Orange™ Task Manager");
        setSize(1150, 720);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { manager.saveToFile(); }
        });

        buildUI();
        refreshTaskPanel();

        countdownTimer = new javax.swing.Timer(60_000, e -> refreshTaskPanel());
        countdownTimer.start();
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    /**
     * Assembles the top-level frame content: outer orange border panel, inner
     * peach-tinted rounded panel, and the horizontal split pane that divides the
     * calendar from the task list.
     *
     * <p>A custom split-pane divider is installed that paints a decorative dotted
     * drag handle and enforces minimum widths of 400 px (left) and 500 px (right).
     * </p>
     */
    private void buildUI() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(OUTER_ORANGE);
        outerPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setContentPane(outerPanel);

        outerPanel.add(buildTitleBar(), BorderLayout.NORTH);

        // Inner panel clips its children to the rounded boundary
        RoundedPanel innerPanel = new RoundedPanel(INNER_PEACH, 24) {
            @Override
            public void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                        0, 0, getWidth(), getHeight(), 24, 24));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        innerPanel.setLayout(new BorderLayout(0, 0));
        outerPanel.add(innerPanel, BorderLayout.CENTER);

        JPanel leftPanel  = buildLeftPanel();
        JPanel rightPanel = buildRightPanel();
        leftPanel.setMinimumSize(new Dimension(400, 0));
        rightPanel.setMinimumSize(new Dimension(400, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(400);
        split.setDividerSize(10);
        split.setResizeWeight(0.2);
        split.setBorder(null);
        split.setOpaque(false);
        split.setBackground(INNER_PEACH);
        split.setContinuousLayout(true);

        // Custom divider with a dotted handle and hover highlight
        split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    private boolean hovered = false;
                    {
                        setBorder(null);
                        setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                        addMouseListener(new java.awt.event.MouseAdapter() {
                            @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true;  repaint(); }
                            @Override public void mouseExited(java.awt.event.MouseEvent e)  { hovered = false; repaint(); }
                        });
                    }
                    @Override
                    public Cursor getCursor() {
                        return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
                    }
                    @Override
                    public void paint(Graphics g) {
                        int w = getWidth(), h = getHeight();
                        final int insetY = 10;
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(hovered ? new Color(0xD4, 0xC2, 0xA8) : CARD_BORDER);
                        g2.fillRect(0, insetY, w, h - insetY * 2);
                        g2.setColor(hovered ? LABEL_BG : new Color(0xBB, 0xA0, 0x7A));
                        int cx = w / 2, diam = 3, gap = 6, n = 5;
                        int startY = (h - (n - 1) * gap) / 2;
                        for (int i = 0; i < n; i++) {
                            g2.fillOval(cx - diam / 2, startY + i * gap, diam, diam);
                        }
                        g2.dispose();
                    }
                };
            }
        });

        innerPanel.add(split, BorderLayout.CENTER);

        // Enforce min-width constraints when the user drags the divider
        split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            int loc      = split.getDividerLocation();
            int minLeft  = 400;
            int minRight = 500;
            int total    = split.getWidth();
            int maxLeft  = total - minRight - split.getDividerSize();
            if      (loc < minLeft) split.setDividerLocation(minLeft);
            else if (loc > maxLeft) split.setDividerLocation(maxLeft);
        });
    }

    /**
     * Builds the application title bar containing the app icon, the app title,
     * and the live-search field.
     *
     * @return the assembled title bar {@link JPanel}
     */
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 4, 10, 4));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        ImageIcon icon   = new ImageIcon("OrangeIcon.png");
        Image     scaled = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        JLabel iconLabel = new JLabel(new ImageIcon(scaled));

        JLabel title = new JLabel("Orange™ Task Manager");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(TEXT_DARK);

        left.add(iconLabel);
        left.add(title);
        bar.add(left, BorderLayout.WEST);

        searchField = new RoundedTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Search tasks…");
        Dimension sfSize = new Dimension(220, 34);
        searchField.setPreferredSize(sfSize);
        searchField.setMaximumSize(sfSize);
        searchField.addActionListener(e -> performSearch(searchField.getText().trim()));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { onSearchChange(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { onSearchChange(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { onSearchChange(); }
        });

        JLabel searchIcon = new JLabel("🔍 ");
        searchIcon.setFont(new Font("Serif", Font.PLAIN, 24));

        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchIcon);
        searchWrap.add(searchField);
        bar.add(searchWrap, BorderLayout.EAST);

        return bar;
    }

    /**
     * Builds the left panel containing the {@link CalendarPanel} and the summary
     * statistics card below it.
     *
     * @return the assembled left {@link JPanel}
     */
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));

        calendarPanel = new CalendarPanel(manager, date -> {
            selectedDate = date;
            searchField.setText("");
            refreshTaskPanel();
        });
        panel.add(calendarPanel, BorderLayout.CENTER);

        JPanel summaryCard = new RoundedPanel(CARD_BG, 12, CARD_BORDER, 1);
        summaryCard.setLayout(new BorderLayout());
        summaryCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        summaryLabel = new JLabel();
        summaryLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        summaryLabel.setForeground(TEXT_DARK);
        refreshSummary();
        summaryCard.add(summaryLabel, BorderLayout.CENTER);

        panel.add(summaryCard, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Attaches a {@link ComponentAdapter} to the right panel that resizes all
     * {@link TaskCard} components whenever the panel is resized, ensuring cards
     * always fill the available width.
     *
     * @param container  the task list panel containing the cards
     * @param rightPanel the panel to listen to for resize events
     */
    private void attachTaskCardResizer(JPanel container, JPanel rightPanel) {
        rightPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = rightPanel.getWidth() - 40;
                for (Component c : container.getComponents()) {
                    if (c instanceof TaskCard) {
                        ((TaskCard) c).setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
                        ((TaskCard) c).setPreferredSize(
                                new Dimension(width, c.getPreferredSize().height));
                    }
                }
                container.revalidate();
                container.repaint();
            }
        });
    }

    /**
     * Builds the right panel containing the date header, the "＋ Add Task"
     * button, and the scrollable task card list.
     *
     * @return the assembled right {@link JPanel}
     */
    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 10));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 8, 10, 4));

        dateHeaderLabel = new JLabel(formatDateHeader(selectedDate));
        dateHeaderLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        dateHeaderLabel.setForeground(TEXT_DARK);
        header.add(dateHeaderLabel, BorderLayout.CENTER);

        OrangeButton addBtn = new OrangeButton("＋ Add Task", BUTTON_ORANGE, BUTTON_HOVER);
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        addBtn.addActionListener(e -> showAddTaskDialog());
        header.add(addBtn, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);

        taskListPanel = new JPanel();
        taskListPanel.setLayout(new BoxLayout(taskListPanel, BoxLayout.Y_AXIS));
        taskListPanel.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        taskListPanel.setOpaque(false);

        taskScrollPane = new JScrollPane(taskListPanel);
        taskScrollPane.setBorder(null);
        taskScrollPane.setOpaque(false);
        taskScrollPane.getViewport().setOpaque(true);
        taskScrollPane.getViewport().setBackground(CONTENT_CREAM);
        taskScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        taskScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(taskScrollPane.getVerticalScrollBar());

        RoundedPanel scrollContainer = new RoundedPanel(CONTENT_CREAM, 14, CARD_BORDER, 1);
        scrollContainer.setLayout(new BorderLayout());
        scrollContainer.add(taskScrollPane, BorderLayout.CENTER);

        attachTaskCardResizer(taskListPanel, panel);

        panel.add(scrollContainer, BorderLayout.CENTER);
        return panel;
    }

    // -----------------------------------------------------------------------
    // Task list management
    // -----------------------------------------------------------------------

    /**
     * Clears and rebuilds the task list panel for {@link #selectedDate}.
     *
     * <p>Tasks are sorted by: days until due (ascending), then priority
     * (descending), then status (ascending).  Each card is given a staggered
     * fade-in animation.  If no tasks exist for the selected date, an empty-state
     * placeholder is shown.</p>
     */
    private void refreshTaskPanel() {
        dateHeaderLabel.setText(formatDateHeader(selectedDate));
        taskListPanel.removeAll();

        List<Task> tasks = new ArrayList<>(manager.getTasksForDate(selectedDate.toString()));
        tasks.sort(Comparator
                .comparingLong(Task::daysUntilDue)
                .thenComparing(t -> t.getPriority().ordinal() * -1)
                .thenComparing(t -> t.getStatus().ordinal()));

        if (tasks.isEmpty()) {
            taskListPanel.add(buildEmptyState());
        } else {
            for (int i = 0; i < tasks.size(); i++) {
                final Task task = tasks.get(i);
                TaskCard card = new TaskCard(task, new TaskCard.TaskCardListener() {
                    @Override public void onEdit(Task t)   { showEditTaskDialog(t); }
                    @Override public void onDelete(Task t) { confirmDelete(t); }
                    @Override public void onStatusChange(Task t, Task.Status newStatus) {
                        t.setStatus(newStatus);
                        manager.updateTask(t);
                        manager.saveToFile();
                        refreshTaskPanel();
                        calendarPanel.refresh();
                        refreshSummary();
                    }
                });
                card.setAlignmentX(Component.CENTER_ALIGNMENT);
                taskListPanel.add(card);
                card.startFadeIn(Math.min(i, 6) * 45);
                int width = taskListPanel.getWidth() - 40;
                card.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
                card.setPreferredSize(new Dimension(width, card.getPreferredSize().height));
                card.setAlignmentX(Component.CENTER_ALIGNMENT);
            }
        }
        taskListPanel.add(Box.createVerticalGlue());
        taskListPanel.revalidate();
        taskListPanel.repaint();
        refreshSummary();
    }

    /**
     * Builds a centred placeholder panel shown when no tasks exist for the
     * selected date.
     *
     * @return the empty-state placeholder {@link JPanel}
     */
    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel("<html><center>No tasks for this day.<br>"
                + "<span style='color:#998877;font-size:11pt'>Click  ＋ Add Task"
                + "  to get started.</span></center></html>");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setForeground(TEXT_MUTED);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(lbl);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        return p;
    }

    /**
     * Refreshes the summary card in the bottom-left corner with up-to-date
     * counts of total, done, in-progress, and overdue tasks across all dates.
     */
    private void refreshSummary() {
        List<Task> all  = manager.getAllTasksFlat();
        long total      = all.size();
        long overdue    = all.stream().filter(Task::isOverdue).count();
        long done       = all.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();
        long inProgress = all.stream()
                            .filter(t -> t.getStatus() == Task.Status.IN_PROGRESS).count();

        summaryLabel.setText(String.format(
                "<html><b>All Tasks</b><br>"
                + "Total: %d &nbsp;|&nbsp; Done: %d<br>"
                + "In Progress: %d &nbsp;|&nbsp; "
                + "<span style='color:#F44336'>Overdue: %d</span></html>",
                total, done, inProgress, overdue));
    }

    // -----------------------------------------------------------------------
    // Search
    // -----------------------------------------------------------------------

    /**
     * Called on every keystroke in the search field.  Delegates to
     * {@link #performSearch} if the field is non-empty, or restores the normal
     * date view if it has been cleared.
     */
    private void onSearchChange() {
        String q = searchField.getText().trim();
        if (q.isEmpty()) { refreshTaskPanel(); return; }
        performSearch(q);
    }

    /**
     * Runs a case-insensitive full-text search across all tasks (description,
     * notes, and tags) and displays the matching cards sorted by date.
     *
     * @param query the search string; must not be empty
     */
    private void performSearch(String query) {
        if (query.isEmpty()) { refreshTaskPanel(); return; }
        String lq = query.toLowerCase();
        taskListPanel.removeAll();
        dateHeaderLabel.setText("Search: \u201c" + query + "\u201d");

        List<Task> results = new ArrayList<>();
        for (Task t : manager.getAllTasksFlat()) {
            if (t.getDescription().toLowerCase().contains(lq)
                    || t.getNotes().toLowerCase().contains(lq)
                    || t.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(lq))) {
                results.add(t);
            }
        }
        results.sort(Comparator.comparing(Task::getDate));

        if (results.isEmpty()) {
            JLabel none = new JLabel("No tasks match \u201c" + query + "\u201d.");
            none.setFont(new Font("SansSerif", Font.ITALIC, 13));
            none.setForeground(TEXT_MUTED);
            none.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 0));
            taskListPanel.add(none);
        } else {
            for (int i = 0; i < results.size(); i++) {
                final Task task = results.get(i);
                TaskCard card = new TaskCard(task, new TaskCard.TaskCardListener() {
                    @Override public void onEdit(Task t)   { showEditTaskDialog(t); }
                    @Override public void onDelete(Task t) { confirmDelete(t); }
                    @Override public void onStatusChange(Task t, Task.Status newStatus) {
                        t.setStatus(newStatus);
                        manager.updateTask(t);
                        manager.saveToFile();
                        onSearchChange();
                        calendarPanel.refresh();
                        refreshSummary();
                    }
                });
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                taskListPanel.add(card);
                card.startFadeIn(Math.min(i, 6) * 35);
            }
        }
        taskListPanel.add(Box.createVerticalGlue());
        taskListPanel.revalidate();
        taskListPanel.repaint();
    }

    // -----------------------------------------------------------------------
    // Dialogs
    // -----------------------------------------------------------------------

    /**
     * Opens the "New Task" modal dialog, pre-populated with the currently
     * selected date.  On confirmation, the new task is added to the manager,
     * saved, and the UI is refreshed.
     */
    private void showAddTaskDialog() {
        JDialog dialog = new JDialog(this, "Add Task", true);
        dialog.setSize(460, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(INNER_PEACH);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        dialog.setContentPane(root);

        JLabel title = new JLabel("New Task");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT_DARK);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        root.add(form, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets  = new Insets(5, 0, 5, 0);
        gbc.gridx   = 0;

        gbc.gridy = 0; form.add(fieldLabel("Task Title *"), gbc);
        gbc.gridy = 1;
        RoundedTextField descField = new RoundedTextField(20);
        form.add(descField, gbc);

        gbc.gridy = 2; form.add(fieldLabel("Due Date *"), gbc);
        gbc.gridy = 3;
        CalendarDatePicker datePicker = new CalendarDatePicker();
        if (selectedDate != null) datePicker.setSelectedDateExternal(selectedDate);
        form.add(datePicker, gbc);

        gbc.gridy = 4; form.add(fieldLabel("Priority"), gbc);
        gbc.gridy = 5;
        JComboBox<Task.Priority> priorityBox = new JComboBox<>(Task.Priority.values());
        priorityBox.setSelectedItem(Task.Priority.MEDIUM);
        priorityBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        priorityBox.setBackground(Color.WHITE);
        form.add(priorityBox, gbc);

        gbc.gridy = 6; form.add(fieldLabel("Tags (space-separated)"), gbc);
        gbc.gridy = 7;
        RoundedTextField tagField = new RoundedTextField(20);
        form.add(tagField, gbc);

        gbc.gridy   = 8; form.add(fieldLabel("Notes"), gbc);
        gbc.gridy   = 9;
        gbc.weighty = 1.0;
        gbc.fill    = GridBagConstraints.BOTH;
        RoundedTextArea notesArea   = new RoundedTextArea(8, 20);
        JScrollPane     notesScroll = new JScrollPane(notesArea);
        notesScroll.setOpaque(false);
        notesScroll.getViewport().setOpaque(true);
        notesScroll.getViewport().setBackground(Color.WHITE);
        notesScroll.setBorder(null);
        styleScrollBar(notesScroll.getVerticalScrollBar());
        RoundedPanel notesWrapper = new RoundedPanel(Color.WHITE, 20);
        notesWrapper.setLayout(new BorderLayout());
        notesWrapper.setPreferredSize(new Dimension(0, 150));
        notesWrapper.setMinimumSize(new Dimension(0, 150));
        notesWrapper.add(notesScroll, BorderLayout.CENTER);
        form.add(notesWrapper, gbc);

        gbc.gridy   = 10;
        gbc.weighty = 0;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.insets  = new Insets(14, 0, 0, 0);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        OrangeButton cancelBtn = new OrangeButton("Cancel",      LABEL_BG,      BUTTON_HOVER);
        OrangeButton createBtn = new OrangeButton("Create Task", BUTTON_ORANGE, BUTTON_HOVER);

        cancelBtn.addActionListener(e -> dialog.dispose());
        createBtn.addActionListener(e -> {
            String desc = descField.getText().trim();
            String date = datePicker.getDateText().trim();
            if (desc.isEmpty()) { showError(dialog, "Task title cannot be empty."); return; }
            if (date.isEmpty()) { showError(dialog, "Please select a due date.");   return; }
            Task task = new Task(desc, date);
            task.setPriority((Task.Priority) priorityBox.getSelectedItem());
            task.setNotes(notesArea.getText().trim());
            String tagInput = tagField.getText().trim();
            if (!tagInput.isEmpty()) {
                for (String t : tagInput.split("\\s+"))
                    if (!t.isEmpty()) task.addTag(t);
            }
            manager.addTask(task);
            manager.saveToFile();
            selectedDate = LocalDate.parse(date);
            calendarPanel.selectDate(selectedDate);
            refreshTaskPanel();
            calendarPanel.refresh();
            dialog.dispose();
        });

        btnRow.add(cancelBtn);
        btnRow.add(createBtn);
        form.add(btnRow, gbc);

        descField.addActionListener(e -> datePicker.requestFocusInWindow());
        dialog.setVisible(true);
    }

    /**
     * Opens the "Edit Task" modal dialog pre-populated with the given task's
     * current values.  On save, the task is updated in the manager, the
     * changes are persisted, and the calendar and task list are refreshed.
     *
     * @param task the task to edit
     */
    private void showEditTaskDialog(Task task) {
        JDialog dialog = new JDialog(this, "Edit Task", true);
        dialog.setSize(460, 660);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(INNER_PEACH);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        dialog.setContentPane(root);

        JLabel title = new JLabel("Edit Task");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT_DARK);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        root.add(form, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets  = new Insets(5, 0, 5, 0);
        gbc.gridx   = 0;

        gbc.gridy = 0; form.add(fieldLabel("Task Title *"), gbc);
        gbc.gridy = 1;
        RoundedTextField descField = new RoundedTextField(20);
        descField.setText(task.getDescription());
        form.add(descField, gbc);

        gbc.gridy = 2; form.add(fieldLabel("Due Date *"), gbc);
        gbc.gridy = 3;
        CalendarDatePicker datePicker = new CalendarDatePicker();
        try {
            datePicker.setSelectedDateExternal(LocalDate.parse(task.getDate()));
        } catch (Exception ignored) {}
        form.add(datePicker, gbc);

        gbc.gridy = 4; form.add(fieldLabel("Status"), gbc);
        gbc.gridy = 5;
        JComboBox<Task.Status> statusBox = new JComboBox<>(Task.Status.values());
        statusBox.setSelectedItem(task.getStatus());
        statusBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusBox.setBackground(Color.WHITE);
        form.add(statusBox, gbc);

        gbc.gridy = 6; form.add(fieldLabel("Priority"), gbc);
        gbc.gridy = 7;
        JComboBox<Task.Priority> priorityBox = new JComboBox<>(Task.Priority.values());
        priorityBox.setSelectedItem(task.getPriority());
        priorityBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        priorityBox.setBackground(Color.WHITE);
        form.add(priorityBox, gbc);

        gbc.gridy = 8; form.add(fieldLabel("Tags (space-separated)"), gbc);
        gbc.gridy = 9;
        RoundedTextField tagField = new RoundedTextField(20);
        List<String> userTags = new ArrayList<>(task.getTags());
        userTags.remove("todo"); userTags.remove("in-progress"); userTags.remove("done");
        tagField.setText(String.join(" ", userTags));
        form.add(tagField, gbc);

        gbc.gridy   = 10; form.add(fieldLabel("Notes"), gbc);
        gbc.gridy   = 11;
        gbc.weighty = 1.0;
        gbc.fill    = GridBagConstraints.BOTH;
        RoundedTextArea notesArea   = new RoundedTextArea(8, 20);
        notesArea.setText(task.getNotes());
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setOpaque(false);
        notesScroll.getViewport().setOpaque(true);
        notesScroll.getViewport().setBackground(Color.WHITE);
        notesScroll.setBorder(null);
        styleScrollBar(notesScroll.getVerticalScrollBar());
        RoundedPanel notesWrapper = new RoundedPanel(Color.WHITE, 20);
        notesWrapper.setLayout(new BorderLayout());
        notesWrapper.setPreferredSize(new Dimension(0, 150));
        notesWrapper.setMinimumSize(new Dimension(0, 150));
        notesWrapper.add(notesScroll, BorderLayout.CENTER);
        form.add(notesWrapper, gbc);

        gbc.gridy   = 12;
        gbc.weighty = 0;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.insets  = new Insets(14, 0, 0, 0);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        OrangeButton cancelBtn = new OrangeButton("Cancel",       LABEL_BG,      BUTTON_HOVER);
        OrangeButton saveBtn   = new OrangeButton("Save Changes", BUTTON_ORANGE, BUTTON_HOVER);

        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            String desc = descField.getText().trim();
            String date = datePicker.getDateText().trim();
            if (desc.isEmpty()) { showError(dialog, "Task title cannot be empty."); return; }
            if (date.isEmpty()) { showError(dialog, "Please select a due date.");   return; }

            task.setDescription(desc);
            task.setDate(date);
            task.setStatus((Task.Status)    statusBox.getSelectedItem());
            task.setPriority((Task.Priority) priorityBox.getSelectedItem());
            task.setNotes(notesArea.getText().trim());

            List<String> newTags = new ArrayList<>();
            for (String t : tagField.getText().trim().split("\\s+"))
                if (!t.isEmpty()) newTags.add(t);
            task.setTags(newTags);

            manager.updateTask(task);
            manager.saveToFile();
            selectedDate = LocalDate.parse(date);
            calendarPanel.selectDate(selectedDate);
            refreshTaskPanel();
            calendarPanel.refresh();
            dialog.dispose();
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        form.add(btnRow, gbc);

        dialog.setVisible(true);
    }

    /**
     * Shows a confirmation dialog before permanently deleting a task.  If the
     * user confirms, the task is removed from the manager, the file is saved,
     * and the UI is refreshed.
     *
     * @param task the task to delete
     */
    private void confirmDelete(Task task) {
        int opt = JOptionPane.showConfirmDialog(
                this,
                "<html>Delete task:<br><b>" + task.getDescription() + "</b>?</html>",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            manager.removeTaskById(task.getId());
            manager.saveToFile();
            refreshTaskPanel();
            calendarPanel.refresh();
            refreshSummary();
        }
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    /**
     * Creates a small bold form-field label with the given text and appropriate
     * styling for use inside the add/edit dialogs.
     *
     * @param text the label text (e.g. {@code "Task Title *"})
     * @return the configured {@link JLabel}
     */
    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(TEXT_DARK);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
        return lbl;
    }

    /**
     * Displays a modal error message dialog with the given text.
     *
     * @param parent  the parent component for the dialog
     * @param message the error message to display
     */
    private void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Formats a date as a long human-readable string suitable for the task list
     * header (e.g. {@code "Thursday, April 10  2025"}).
     *
     * @param date the date to format; returns an empty string if {@code null}
     * @return formatted date header string
     */
    private String formatDateHeader(LocalDate date) {
        if (date == null) return "";
        String dow = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String mon = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return dow + ", " + mon + " " + date.getDayOfMonth() + "  " + date.getYear();
    }

    /**
     * Installs a custom thin scroll-bar UI on the given {@link JScrollBar}.
     *
     * <p>The scroll bar is made narrow (8 px), the track is hidden, and the
     * thumb is rendered as a small rounded rectangle using theme colours.
     * The increase/decrease arrow buttons are replaced with zero-size invisible
     * buttons.</p>
     *
     * @param sb the scroll bar to style
     */
    private static void styleScrollBar(JScrollBar sb) {
        sb.setOpaque(false);
        sb.setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
        sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor          = LABEL_BG;
                thumbHighlightColor = BUTTON_ORANGE;
                trackColor          = CONTENT_CREAM;
                trackHighlightColor = CONTENT_CREAM;
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isDragging ? BUTTON_HOVER : LABEL_BG);
                g2.fillRoundRect(r.x + 1, r.y + 2, r.width - 2, r.height - 4, 6, 6);
                g2.dispose();
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setBorder(BorderFactory.createEmptyBorder());
                return b;
            }
        });
    }

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    /**
     * Application entry point.  Sets the system look-and-feel (if available),
     * then creates and shows the {@code MainApp} window on the Event Dispatch
     * Thread.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new MainApp().setVisible(true));
    }
}