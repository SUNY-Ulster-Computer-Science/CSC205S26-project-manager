package projectmanager;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import static projectmanager.ThemeColors.*;

/**
 * A rich card component that visualizes a single {@link Task} inside the task
 * list panel.
 *
 * <p>Each card renders:</p>
 * <ul>
 *   <li>A colour-coded left-edge stripe indicating {@link Task.Priority}.</li>
 *   <li>The task title (struck through when the task is {@link Task.Status#DONE}).</li>
 *   <li>A countdown badge in the top-right corner (colour-coded by urgency).</li>
 *   <li>A one-line notes preview (if notes are present).</li>
 *   <li>A row of tag pills.</li>
 *   <li>An action row containing a status dropdown, a priority badge, an
 *       <em>Edit</em> button, and a <em>Delete</em> button.</li>
 * </ul>
 *
 * <p>User interactions are reported to a {@link TaskCardListener} so the
 * parent view can perform the appropriate business logic without coupling to
 * the card's internals.</p>
 *
 * <p>Cards support a fade-in entrance animation triggered by
 * {@link #startFadeIn(int)}.</p>
 */
public class TaskCard extends JPanel {

    // -----------------------------------------------------------------------
    // Listener interface
    // -----------------------------------------------------------------------

    /**
     * Callback interface for task-card user interactions.
     *
     * <p>Implement this interface and pass it to the {@link TaskCard} constructor
     * to receive notifications when the user edits, deletes, or changes the
     * status of a task.</p>
     */
    public interface TaskCardListener {
        /**
         * Called when the user clicks the <em>Edit</em> button on this card.
         *
         * @param task the task to be edited
         */
        void onEdit(Task task);

        /**
         * Called when the user clicks the <em>Delete</em> button on this card.
         *
         * @param task the task to be deleted
         */
        void onDelete(Task task);

        /**
         * Called when the user selects a new status from the status dropdown.
         *
         * @param task      the task whose status should change
         * @param newStatus the status selected by the user
         */
        void onStatusChange(Task task, Task.Status newStatus);
    }

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** The task data displayed by this card. */
    private Task task;

    /** Listener that receives edit / delete / status-change events. */
    private TaskCardListener listener;

    /** Badge label showing the time remaining until the task is due. */
    private JLabel countdownLabel;

    /** Inline label reflecting the current workflow status (unused after build). */
    private JLabel statusLabel;

    /** Panel that holds the tag pill labels. */
    private JPanel tagPanel;

    /**
     * Current opacity for the fade-in animation (0.0 = invisible, 1.0 = fully
     * opaque).  Marked {@code volatile} because the animation timer runs on the
     * EDT and the field is read inside {@link #paint(Graphics)}.
     */
    private volatile float fadeAlpha = 1f;

    /** Swing timer that increments {@link #fadeAlpha} each frame. */
    private javax.swing.Timer fadeTimer;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs a {@code TaskCard} for the given task and wires up the supplied
     * listener.
     *
     * @param task     the task to display (must not be {@code null})
     * @param listener callback for user interactions; may be {@code null} to
     *                 suppress callbacks
     */
    public TaskCard(Task task, TaskCardListener listener) {
        this.task     = task;
        this.listener = listener;
        buildUI();
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    /**
     * Builds the complete card layout: an outer wrapper (for the empty border
     * margin) containing a {@link RoundedPanel} card, which in turn contains the
     * body panel.
     */
    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        final Color priorityColor = forPriority(task.getPriority());

        // Card surface: rounded panel with a custom left-edge priority stripe
        RoundedPanel card = new RoundedPanel(CARD_BG, 14, CARD_BORDER, 1) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                        0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(priorityColor);
                g2.fillRect(0, 0, 5, getHeight());
                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout(0, 0));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(10, 17, 8, 12));
        card.add(body, BorderLayout.CENTER);

        buildBody(body);
        add(card);
        setMaximumSize(new Dimension(600, Integer.MAX_VALUE));
    }

    /**
     * Populates the body panel with the title row, optional notes preview, tag
     * panel, and action row using a {@link GridBagLayout}.
     *
     * @param body the panel to populate (already configured with GridBagLayout)
     */
    private void buildBody(JPanel body) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx   = 0;
        gbc.gridy   = 0;
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.insets  = new Insets(0, 0, 4, 0);

        // Row 0: title + countdown badge
        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);

        JLabel titleLabel = new JLabel(task.getDescription());
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(task.getStatus() == Task.Status.DONE ? TEXT_MUTED : TEXT_DARK);
        if (task.getStatus() == Task.Status.DONE) {
            titleLabel.setText("<html><s>" + escHtml(task.getDescription()) + "</s></html>");
        }
        titleRow.add(titleLabel, BorderLayout.CENTER);

        countdownLabel = buildCountdownBadge();
        titleRow.add(countdownLabel, BorderLayout.EAST);

        gbc.gridwidth = 2;
        body.add(titleRow, gbc);

        // Row 1 (optional): first line of notes, truncated
        String notes = task.getNotes();
        if (notes != null && !notes.trim().isEmpty()) {
            gbc.gridy  = 1;
            gbc.insets = new Insets(0, 0, 4, 0);
            String preview = notes.contains("\n")
                    ? notes.substring(0, notes.indexOf('\n'))
                    : notes;
            if (preview.length() > 80) preview = preview.substring(0, 77) + "…";
            JLabel notesLabel = new JLabel("<html><i>" + escHtml(preview) + "</i></html>");
            notesLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            notesLabel.setForeground(TEXT_MUTED);
            body.add(notesLabel, gbc);
        }

        // Row 2: tag pills
        gbc.gridy  = 2;
        gbc.insets = new Insets(0, 0, 6, 0);
        tagPanel   = buildTagPanel();
        body.add(tagPanel, gbc);

        // Row 3: action buttons
        gbc.gridy  = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(buildActionRow(), gbc);
    }

    /**
     * Builds and returns the countdown badge label, sized to a fixed 90 × 20 px
     * and filled with the urgency colour determined by
     * {@link ThemeColors#forCountdown(Task)}.
     *
     * @return the configured countdown badge {@link JLabel}
     */
    private JLabel buildCountdownBadge() {
        String text  = task.countdownLabel();
        Color  color = ThemeColors.forCountdown(task);
        JLabel lbl   = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(Color.WHITE);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setOpaque(false);
        lbl.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        Dimension fixed = new Dimension(90, 20);
        lbl.setPreferredSize(fixed);
        lbl.setMinimumSize(fixed);
        lbl.setMaximumSize(fixed);
        return lbl;
    }

    /**
     * Builds and returns the tag panel, populated with one pill label per tag.
     * If the task has no tags a muted italic "no tags" placeholder is shown instead.
     *
     * @return the configured tag {@link JPanel}
     */
    private JPanel buildTagPanel() {
        JPanel panel = new JPanel(new WrapLayout(java.awt.FlowLayout.LEFT, 4, 2));
        panel.setOpaque(false);
        List<String> tags = task.getTags();
        if (tags.isEmpty()) {
            JLabel none = new JLabel("no tags");
            none.setFont(new Font("SansSerif", Font.ITALIC, 11));
            none.setForeground(TEXT_MUTED);
            panel.add(none);
        } else {
            for (String tag : tags) {
                panel.add(buildTagPill(tag));
            }
        }
        return panel;
    }

    /**
     * Builds a single pill-shaped label for a tag.  Reserved status tags
     * ({@code "todo"}, {@code "in-progress"}, {@code "done"}) receive their
     * corresponding status colour; all other tags use {@link ThemeColors#LABEL_BG}.
     *
     * @param tag the tag text to display
     * @return the styled tag pill {@link JLabel}
     */
    private JLabel buildTagPill(String tag) {
        Color bg;
        if      (tag.equals("todo"))        bg = STATUS_TODO;
        else if (tag.equals("in-progress")) bg = STATUS_IN_PROGRESS;
        else if (tag.equals("done"))        bg = STATUS_DONE;
        else                                bg = LABEL_BG;

        JLabel pill = new JLabel(tag) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(new Font("SansSerif", Font.BOLD, 10));
        pill.setForeground(Color.WHITE);
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        return pill;
    }

    /**
     * Builds and returns the action row containing the status dropdown button,
     * priority badge, Edit button, and Delete button.
     *
     * @return the configured action-row {@link JPanel}
     */
    private JPanel buildActionRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);

        row.add(buildStatusButton());
        row.add(buildPriorityBadge());
        row.add(Box.createHorizontalStrut(4));

        JButton editBtn = buildIconButton("✎ Edit", BUTTON_ORANGE, BUTTON_HOVER);
        editBtn.addActionListener(e -> { if (listener != null) listener.onEdit(task); });
        row.add(editBtn);

        JButton deleteBtn = buildIconButton("✕ Delete",
                new Color(0xF4, 0x43, 0x36),
                new Color(0xC6, 0x28, 0x28));
        deleteBtn.addActionListener(e -> { if (listener != null) listener.onDelete(task); });
        row.add(deleteBtn);

        return row;
    }

    /**
     * Builds the status dropdown button.  Clicking it shows a popup menu with
     * all {@link Task.Status} values; selecting one fires
     * {@link TaskCardListener#onStatusChange}.
     *
     * @return the configured status {@link RippleButton}
     */
    private JButton buildStatusButton() {
        Task.Status current = task.getStatus();
        Color       bg      = forStatus(current);

        RippleButton btn = new RippleButton(current.getLabel() + " ▾") {
            @Override
            protected void paintBackground(Graphics2D g2) {
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));

        btn.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            for (Task.Status s : Task.Status.values()) {
                JMenuItem item = new JMenuItem(s.getLabel());
                item.setFont(new Font("SansSerif", Font.PLAIN, 12));
                item.addActionListener(ae -> {
                    if (listener != null) listener.onStatusChange(task, s);
                });
                menu.add(item);
            }
            menu.show(btn, 0, btn.getHeight());
        });

        return btn;
    }

    /**
     * Builds a non-interactive badge label showing the current priority with a
     * low-opacity tinted background derived from the priority colour.
     *
     * @return the configured priority badge {@link JLabel}
     */
    private JLabel buildPriorityBadge() {
        Color  bg  = forPriority(task.getPriority());
        String lbl = "● " + task.getPriority().getLabel();
        JLabel badge = new JLabel(lbl) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("SansSerif", Font.BOLD, 11));
        badge.setForeground(bg.darker());
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return badge;
    }

    /**
     * Builds a small pill-shaped {@link RippleButton} used for Edit and Delete
     * actions.
     *
     * @param text  button label (e.g. {@code "✎ Edit"})
     * @param bg    normal background colour
     * @param hover background colour on hover
     * @return the configured action {@link RippleButton}
     */
    private JButton buildIconButton(String text, Color bg, Color hover) {
        RippleButton btn = new RippleButton(text) {
            @Override
            protected void paintBackground(Graphics2D g2) {
                g2.setColor(getModel().isRollover() ? hover : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return btn;
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    /**
     * Escapes HTML special characters so that task descriptions and notes can be
     * safely embedded inside HTML-formatted Swing labels.
     *
     * @param s the string to escape; {@code null} is treated as an empty string
     * @return the escaped string with {@code &amp;}, {@code &lt;}, and
     *         {@code &gt;} substituted for {@code &}, {@code <}, and {@code >}
     */
    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns the {@link Task} displayed by this card.
     *
     * @return the underlying task (never {@code null})
     */
    public Task getTask() { return task; }

    /**
     * Starts a fade-in entrance animation for this card.
     *
     * <p>The card's opacity begins at 0 and increases by 0.07 every 16 ms (roughly
     * 60 fps) after an initial delay.  Calling this method while an animation is
     * already running restarts it from zero.</p>
     *
     * @param delayMs time in milliseconds to wait before the fade begins;
     *                values less than zero are treated as zero
     */
    public void startFadeIn(int delayMs) {
        fadeAlpha = 0f;
        javax.swing.Timer starter = new javax.swing.Timer(Math.max(0, delayMs), e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();
            fadeTimer = new javax.swing.Timer(16, null);
            fadeTimer.addActionListener(ae -> {
                fadeAlpha = Math.min(1f, fadeAlpha + 0.07f);
                repaint();
                if (fadeAlpha >= 1f) {
                    fadeAlpha = 1f;
                    ((javax.swing.Timer) ae.getSource()).stop();
                }
            });
            fadeTimer.start();
            repaint();
        });
        starter.setRepeats(false);
        starter.start();
    }

    // -----------------------------------------------------------------------
    // Painting
    // -----------------------------------------------------------------------

    /**
     * Overrides {@link JPanel#paint(Graphics)} to apply the current
     * {@link #fadeAlpha} as a composite alpha when the fade-in animation is
     * active.
     *
     * <p>If {@code fadeAlpha} is 0 the card is not painted at all; if it is 1
     * the default paint path is used with no compositing overhead.</p>
     *
     * @param g the graphics context provided by Swing
     */
    @Override
    public void paint(Graphics g) {
        float a = fadeAlpha;
        if (a <= 0f) return;
        if (a >= 1f) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        super.paint(g2);
        g2.dispose();
    }

    /**
     * Returns the maximum size with an unconstrained height so that the card
     * always fills the full available width of the task list panel.
     *
     * @return {@link Dimension} with {@code Integer.MAX_VALUE} height
     */
    @Override
    public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
    }
}