package projectmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

import static projectmanager.ThemeColors.*;

/**
 * A full month-view calendar panel embedded directly in the main window.
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>{@link SquareCellLayout} — a custom {@link LayoutManager} that
 *       computes cell size as {@code availableWidth / 7} and reuses that
 *       value for height, guaranteeing perfect squares regardless of how the
 *       panel is resized.</li>
 *   <li>Each day cell is a single component whose {@code paintComponent}
 *       handles all drawing (background, today-ring, day number, badge).
 *       No child labels or sub-panels are used, eliminating any
 *       inner-layout step that could cause misalignment.</li>
 *   <li>Clicking a day <em>only repaints the two affected cells</em> (the
 *       previously-selected and newly-selected one).  The grid is never
 *       torn down and rebuilt on a simple selection change, which was the
 *       root cause of the visible "rebuild flash" glitch.</li>
 * </ul>
 * </p>
 */
public class CalendarPanel extends JPanel {

    // ── Listener ──────────────────────────────────────────────────────────

    public interface DateSelectionListener {
        void onDateSelected(LocalDate date);
    }

    // ── Constants ─────────────────────────────────────────────────────────

    private static final String[] DAY_HEADERS =
            {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
    private static final int COLS     = 7;
    private static final int CELL_GAP = 3;
    static final         int DEFAULT_CELL = 40;   // floor used by SquareCellLayout

    // ── State ─────────────────────────────────────────────────────────────

    private YearMonth                   viewMonth    = YearMonth.now();
    private LocalDate                   selectedDate = LocalDate.now();
    private final TaskManager           taskManager;
    private final DateSelectionListener listener;

    /**
     * Live map from date → cell panel so selection changes can repaint
     * only the two affected cells without rebuilding the whole grid.
     */
    private final Map<LocalDate, JPanel> cellsByDate = new HashMap<>();

    // ── UI references ─────────────────────────────────────────────────────

    private JLabel monthLabel;
    private JPanel gridWrapper;   // BorderLayout: NORTH = headers, CENTER = cellGrid
    private JPanel cellGrid;      // SquareCellLayout holding day cells

    // ══════════════════════════════════════════════════════════════════════
    //  Construction
    // ══════════════════════════════════════════════════════════════════════

    public CalendarPanel(TaskManager taskManager, DateSelectionListener listener) {
        this.taskManager = taskManager;
        this.listener    = listener;
        setLayout(new BorderLayout(0, 6));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        add(buildHeader(), BorderLayout.NORTH);
        gridWrapper = buildGridWrapper();
        add(gridWrapper, BorderLayout.CENTER);
    }

    // ── Header (month title + navigation arrows) ──────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        monthLabel = new JLabel(formatMonthTitle(), SwingConstants.CENTER);
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        monthLabel.setForeground(TEXT_DARK);

        JButton prevBtn = buildArrowButton(true);
        JButton nextBtn = buildArrowButton(false);

        prevBtn.addActionListener(e -> { viewMonth = viewMonth.minusMonths(1); rebuildCells(); });
        nextBtn.addActionListener(e -> { viewMonth = viewMonth.plusMonths(1);  rebuildCells(); });

        header.add(prevBtn,    BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextBtn,    BorderLayout.EAST);
        return header;
    }

    private RippleButton buildArrowButton(boolean left) {
        RippleButton btn = new RippleButton("") {
            @Override
            protected void paintBackground(Graphics2D g2) {
                int w = getWidth(), h = getHeight();
                int sz = Math.min(w, h) / 3;
                int cx = w / 2, cy = h / 2;
                Polygon arrow = new Polygon();
                if (left) {
                    arrow.addPoint(cx + sz, cy - sz);
                    arrow.addPoint(cx - sz, cy);
                    arrow.addPoint(cx + sz, cy + sz);
                } else {
                    arrow.addPoint(cx - sz, cy - sz);
                    arrow.addPoint(cx + sz, cy);
                    arrow.addPoint(cx - sz, cy + sz);
                }
                g2.setColor(getModel().isRollover() ? BUTTON_HOVER : DAY_HOVER);
                g2.fill(arrow);
            }
            // Arrow sits on the light-coloured calendar header — use a warm ripple
            @Override protected Color rippleColor() { return LABEL_BG; }
            @Override protected int   rippleArc()   { return 36; }
        };
        btn.setPreferredSize(new Dimension(36, 36));
        return btn;
    }

    // ── Grid wrapper ──────────────────────────────────────────────────────

    private JPanel buildGridWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout(0, CELL_GAP));
        wrapper.setOpaque(false);

        // Day-of-week header row (fixed 20 px height).
        JPanel headerRow = new JPanel(new GridLayout(1, COLS, CELL_GAP, 0));
        headerRow.setOpaque(false);
        for (String h : DAY_HEADERS) {
            JLabel lbl = new JLabel(h, SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            lbl.setForeground(BUTTON_HOVER);
            lbl.setPreferredSize(new Dimension(DEFAULT_CELL, 20));
            headerRow.add(lbl);
        }
        wrapper.add(headerRow, BorderLayout.NORTH);

        cellGrid = buildCellGrid();
        wrapper.add(cellGrid, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Cell grid ─────────────────────────────────────────────────────────

    private JPanel buildCellGrid() {
        cellsByDate.clear();

        JPanel grid = new JPanel(new SquareCellLayout(COLS, CELL_GAP));
        grid.setOpaque(false);

        LocalDate today  = LocalDate.now();
        LocalDate first  = viewMonth.atDay(1);
        int       offset = first.getDayOfWeek().getValue() % 7;  // Sun=0

        for (int i = 0; i < offset; i++) {
            JPanel blank = new JPanel();
            blank.setOpaque(false);
            grid.add(blank);
        }

        for (int d = 1, days = viewMonth.lengthOfMonth(); d <= days; d++) {
            LocalDate date = viewMonth.atDay(d);
            JPanel    cell = buildDayCell(date, today);
            cellsByDate.put(date, cell);
            grid.add(cell);
        }
        return grid;
    }

    // ── Day cell ──────────────────────────────────────────────────────────

    private JPanel buildDayCell(LocalDate date, LocalDate today) {
        final boolean isToday = date.equals(today);

        JPanel cell = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // Read current state dynamically so we don't need a rebuild on click
                final boolean isSelected = date.equals(CalendarPanel.this.selectedDate);
                final boolean hovered    = Boolean.TRUE.equals(getClientProperty("hovered"));
                final int     taskCount  = taskManager.getTaskCountForDate(date.toString());

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                final int w = getWidth(), h = getHeight();

                // Background
                if (isSelected) {
                    g2.setColor(DAY_SELECTED);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                } else if (hovered) {
                    g2.setColor(DAY_HOVER);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                }

                // Today ring
                if (isToday) {
                    g2.setColor(TODAY_RING);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, w - 2, h - 2, 10, 10);
                    g2.setStroke(new BasicStroke(1f));
                }

                paintDayContent(g2, w, h, date, isSelected, isToday, taskCount);
                g2.dispose();
            }
        };

        cell.setOpaque(false);
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cell.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                cell.putClientProperty("hovered", Boolean.TRUE);
                cell.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                cell.putClientProperty("hovered", Boolean.FALSE);
                cell.repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                LocalDate prev = selectedDate;
                selectedDate = date;

                // Only repaint the two affected cells — no grid rebuild.
                JPanel prevCell = cellsByDate.get(prev);
                if (prevCell != null) prevCell.repaint();
                cell.repaint();

                if (listener != null) listener.onDateSelected(date);
            }
        });

        return cell;
    }

    /**
     * Paints the day number and (when there are tasks) a small pill badge,
     * all via direct Graphics2D calls so alignment is pixel-exact.
     */
    private void paintDayContent(Graphics2D g2, int w, int h,
                                  LocalDate date, boolean isSelected,
                                  boolean isToday, int taskCount) {
        Color numColor = isSelected ? Color.WHITE
                : isToday            ? BUTTON_HOVER
                                     : TEXT_DARK;
        Font  numFont  = new Font("SansSerif", isToday ? Font.BOLD : Font.PLAIN, 13);
        g2.setFont(numFont);
        FontMetrics nfm    = g2.getFontMetrics();
        String      numStr = String.valueOf(date.getDayOfMonth());
        int         numW   = nfm.stringWidth(numStr);

        // ── Badge (drawn first so number sits on top of it if they ever overlap) ──
        if (taskCount > 0) {
            Font        bFont  = new Font("SansSerif", Font.BOLD, 8);
            FontMetrics bfm    = g2.getFontMetrics(bFont);
            String      badge  = taskCount > 9 ? "9+" : String.valueOf(taskCount);
            int         bTextW = bfm.stringWidth(badge);
            int         bPadX  = 4;
            int         bw     = Math.max(bTextW + bPadX * 2, 14);
            int         bh     = bfm.getAscent() + bfm.getDescent() + 2;

            // Badge: centred horizontally, 2 px from bottom edge
            int bx = (w - bw) / 2;
            int by = h - bh - 2;

            // Use slightly translucent white when selected so the orange bg shows
            g2.setColor(isSelected
                    ? new Color(255, 255, 255, 200)
                    : pickBadgeColor(date));
            g2.fillRoundRect(bx, by, bw, bh, bh, bh);

            g2.setFont(bFont);
            g2.setColor(isSelected ? DAY_SELECTED : Color.WHITE);
            int btx = bx + (bw - bTextW) / 2;
            int bty = by + bfm.getAscent() + (bh - bfm.getAscent() - bfm.getDescent()) / 2;
            g2.drawString(badge, btx, bty);
        }

        // ── Day number: ALWAYS vertically centred in the FULL cell ──────────
        // This is the key fix — the position never shifts depending on whether
        // a badge is present, which was the original cause of misalignment.
        g2.setFont(numFont);
        g2.setColor(numColor);
        int numX = (w - numW) / 2;
        int numY = (h + nfm.getAscent() - nfm.getDescent()) / 2;
        g2.drawString(numStr, numX, numY);
    }

    private Color pickBadgeColor(LocalDate date) {
        for (Task t : taskManager.getTasksForDate(date.toString())) {
            if (t.isOverdue())                     return COUNTDOWN_OVERDUE;
            if (t.getStatus() == Task.Status.DONE) continue;
            long days = t.daysUntilDue();
            if (days == 0) return COUNTDOWN_TODAY;
            if (days <= 3) return COUNTDOWN_SOON;
        }
        return COUNTDOWN_NORMAL;
    }

    // ── Refresh helpers ───────────────────────────────────────────────────

    /** Full grid rebuild — only called on month navigation. */
    private void rebuildCells() {
        monthLabel.setText(formatMonthTitle());
        gridWrapper.remove(cellGrid);
        cellGrid = buildCellGrid();
        gridWrapper.add(cellGrid, BorderLayout.CENTER);
        gridWrapper.revalidate();
        gridWrapper.repaint();
    }

    /**
     * Repaints all visible day cells to reflect updated task counts.
     * Does <em>not</em> rebuild the grid.
     */
    public void refresh() {
        for (JPanel c : cellsByDate.values()) c.repaint();
    }

    /** Programmatically selects {@code date} and navigates to its month. */
    public void selectDate(LocalDate date) {
        if (!YearMonth.from(date).equals(viewMonth)) {
            selectedDate = date;
            viewMonth    = YearMonth.from(date);
            rebuildCells();
        } else {
            LocalDate prev = selectedDate;
            selectedDate   = date;
            JPanel prevCell = cellsByDate.get(prev);
            if (prevCell != null) prevCell.repaint();
            JPanel newCell  = cellsByDate.get(date);
            if (newCell  != null) newCell.repaint();
        }
    }

    public LocalDate getSelectedDate() { return selectedDate; }

    private String formatMonthTitle() {
        return viewMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + "  " + viewMonth.getYear();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(320, 340);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SquareCellLayout
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Layout manager that places components in {@code cols} columns where
     * every cell is a <em>perfect square</em>.
     *
     * <p>Cell side length = {@code (availableWidth - (cols-1)*gap) / cols}.
     * The same value is used for height, so cells are always square regardless
     * of the container's actual height or the window's current size.</p>
     */
    static final class SquareCellLayout implements LayoutManager {

        private final int cols;
        private final int gap;

        SquareCellLayout(int cols, int gap) {
            this.cols = cols;
            this.gap  = gap;
        }

        /** Derives cell side from the container's current (or ancestor's) width. */
        private int cellSize(Container p) {
            Insets ins    = p.getInsets();
            int    availW = p.getWidth() - ins.left - ins.right;
            if (availW <= 0) {
                Container par = p.getParent();
                while (par != null && availW <= 0) {
                    Insets pi = par.getInsets();
                    availW = par.getWidth() - pi.left - pi.right;
                    par    = par.getParent();
                }
            }
            if (availW <= 0) availW = cols * DEFAULT_CELL + (cols - 1) * gap;
            return Math.max(DEFAULT_CELL, (availW - (cols - 1) * gap) / cols);
        }

        @Override
        public void layoutContainer(Container p) {
            Insets ins = p.getInsets();
            int    cs  = cellSize(p);
            int    x   = ins.left;
            int    y   = ins.top;
            int    col = 0;
            for (Component c : p.getComponents()) {
                c.setBounds(x, y, cs, cs);   // width == height → perfect square
                x += cs + gap;
                if (++col == cols) {
                    col = 0;
                    x   = ins.left;
                    y  += cs + gap;
                }
            }
        }

        @Override public Dimension preferredLayoutSize(Container p) { return computeSize(p); }
        @Override public Dimension minimumLayoutSize(Container p)   { return computeSize(p); }

        private Dimension computeSize(Container p) {
            Insets ins  = p.getInsets();
            int    cs   = cellSize(p);
            int    n    = p.getComponentCount();
            int    rows = (int) Math.ceil((double) n / cols);
            int    tw   = ins.left + ins.right  + cols * cs + (cols - 1) * gap;
            int    th   = ins.top  + ins.bottom + rows * cs + Math.max(0, rows - 1) * gap;
            return new Dimension(tw, th);
        }

        @Override public void addLayoutComponent(String name, Component c) {}
        @Override public void removeLayoutComponent(Component c) {}
    }
}