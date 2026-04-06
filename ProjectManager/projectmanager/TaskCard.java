package projectmanager;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import static projectmanager.ThemeColors.*;

/**
 * A self-contained card panel that displays a single {@link Task}.
 *
 * <p>Layout:
 * <pre>
 * ┌──────────────────────────────────────────────┐
 * │▌ [Title / Description]          [countdown] │
 * │  [Notes preview]                             │
 * │  [tag1] [tag2] ...                           │
 * │  [Status: ▼]  [Priority]    [Edit] [Delete] │
 * └──────────────────────────────────────────────┘
 * </pre>
 * The left border strip is colored by priority.
 * </p>
 */
public class TaskCard extends JPanel {

    // ── Callback interface ────────────────────────────────────────────────

    public interface TaskCardListener {
        void onEdit(Task task);
        void onDelete(Task task);
        void onStatusChange(Task task, Task.Status newStatus);
    }

    // ── Fields ────────────────────────────────────────────────────────────

    private Task           task;
    private TaskCardListener listener;

    // Dynamic labels that need live updates
    private JLabel countdownLabel;
    private JLabel statusLabel;
    private JPanel tagPanel;

    // Fade-in animation state (1f = fully visible / no animation running)
    private volatile float            fadeAlpha = 1f;
    private javax.swing.Timer         fadeTimer;

    // ── Constructor ───────────────────────────────────────────────────────

    public TaskCard(Task task, TaskCardListener listener) {
        this.task     = task;
        this.listener = listener;
        buildUI();
    }

    // ── UI construction ───────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        // Outer wrapper with rounded white background + drop-shadow effect.
        // The priority strip is painted directly inside the rounded shape so it
        // never bleeds out through the rounded corners (no separate opaque JPanel).
        final Color priorityColor = forPriority(task.getPriority());
        RoundedPanel card = new RoundedPanel(CARD_BG, 14, CARD_BORDER, 1) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);   // draws rounded bg + border first
                // Paint the left priority strip, clipped to the card's rounded shape
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                        0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(priorityColor);
                g2.fillRect(0, 0, 5, getHeight());
                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout(0, 0));

        // Main content — left inset includes the 5 px strip width
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(10, 17, 8, 12));
        card.add(body, BorderLayout.CENTER);

        buildBody(body);
        add(card);
        setMaximumSize(new Dimension(600, Integer.MAX_VALUE));
    }

    private void buildBody(JPanel body) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.weightx   = 1.0;
        gbc.gridx     = 0;
        gbc.gridy     = 0;
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.insets    = new Insets(0, 0, 4, 0);

        // ── Row 0: title + countdown ──────────────────────────────────────
        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);

        JLabel titleLabel = new JLabel(task.getDescription());
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(task.getStatus() == Task.Status.DONE
                ? TEXT_MUTED : TEXT_DARK);
        if (task.getStatus() == Task.Status.DONE) {
            // Strikethrough via HTML
            titleLabel.setText("<html><s>" + escHtml(task.getDescription()) + "</s></html>");
        }
        titleRow.add(titleLabel, BorderLayout.CENTER);

        countdownLabel = buildCountdownBadge();
        titleRow.add(countdownLabel, BorderLayout.EAST);

        gbc.gridwidth = 2;
        body.add(titleRow, gbc);

        // ── Row 1: notes preview ──────────────────────────────────────────
        String notes = task.getNotes();
        if (notes != null && !notes.trim().isEmpty()) {
            gbc.gridy = 1;
            gbc.insets = new Insets(0, 0, 4, 0);
            // Show first line only
            String preview = notes.contains("\n") ? notes.substring(0, notes.indexOf('\n')) : notes;
            if (preview.length() > 80) preview = preview.substring(0, 77) + "…";
            JLabel notesLabel = new JLabel("<html><i>" + escHtml(preview) + "</i></html>");
            notesLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            notesLabel.setForeground(TEXT_MUTED);
            body.add(notesLabel, gbc);
        }

        // ── Row 2: tags ───────────────────────────────────────────────────
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 6, 0);
        tagPanel = buildTagPanel();
        body.add(tagPanel, gbc);

        // ── Row 3: status + priority + buttons ────────────────────────────
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(buildActionRow(), gbc);
    }

    // ── Countdown badge ───────────────────────────────────────────────────

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

    // ── Tag panel ─────────────────────────────────────────────────────────

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

    private JLabel buildTagPill(String tag) {
        // Reserved status tags get their status color; user tags get LABEL_BG
        Color bg;
        if      (tag.equals("todo"))        bg = STATUS_TODO;
        else if (tag.equals("in-progress")) bg = STATUS_IN_PROGRESS;
        else if (tag.equals("done"))        bg = STATUS_DONE;
        else                                 bg = LABEL_BG;

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

    // ── Action row ────────────────────────────────────────────────────────

    private JPanel buildActionRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);

        // Status toggle button
        JButton statusBtn = buildStatusButton();
        row.add(statusBtn);

        // Priority badge (label only, not interactive — priority is set in edit dialog)
        row.add(buildPriorityBadge());

        // Spacer
        row.add(Box.createHorizontalStrut(4));

        // Edit button
        JButton editBtn = buildIconButton("✎ Edit", BUTTON_ORANGE, BUTTON_HOVER);
        editBtn.addActionListener(e -> { if (listener != null) listener.onEdit(task); });
        row.add(editBtn);

        // Delete button
        JButton deleteBtn = buildIconButton("✕ Delete", new Color(0xF4, 0x43, 0x36), new Color(0xC6, 0x28, 0x28));
        deleteBtn.addActionListener(e -> { if (listener != null) listener.onDelete(task); });
        row.add(deleteBtn);

        return row;
    }

    private JButton buildStatusButton() {
        Task.Status current = task.getStatus();
        Color bg = forStatus(current);

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

        // Status cycle popup
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

    private JLabel buildPriorityBadge() {
        Color bg  = forPriority(task.getPriority());
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

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public Task getTask() { return task; }

    /**
     * Starts a staggered fade-in animation for this card.
     *
     * <p>Sets alpha to 0 immediately so the card is invisible on first paint,
     * waits {@code delayMs}, then ramps alpha to 1 over ~250 ms at ~60 fps.</p>
     *
     * @param delayMs milliseconds before the fade begins (use {@code i * 45}
     *                for a cascading appearance of multiple cards)
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

    /**
     * Overrides {@code paint} (not just {@code paintComponent}) so the
     * AlphaComposite is applied to the full component subtree — background,
     * borders, and all child widgets — during the fade-in animation.
     */
    @Override
    public void paint(Graphics g) {
        float a = fadeAlpha;
        if (a <= 0f) return;          // not yet visible
        if (a >= 1f) {
            super.paint(g);           // fully opaque — no composite overhead
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        super.paint(g2);
        g2.dispose();
    }

    /**
     * Overrides the default maximum size so that {@code BoxLayout} (Y_AXIS)
     * never stretches this card vertically beyond its natural preferred height.
     *
     * <p>This replaces the previous pattern of calling
     * {@code setMaximumSize(new Dimension(MAX, card.getPreferredSize().height))}
     * from the parent, which was called <em>before</em> the card was given its
     * real width and therefore returned a wrong (too-small) height.</p>
     */
    @Override
    public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
    }
}