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
 * A full-featured month-view calendar panel embedded in the left sidebar of the
 * application.
 *
 * <p>The panel renders the days of the current view month as a grid of
 * square cells.  Each cell shows the day number; cells that have tasks display a
 * small badge with the task count, colour-coded by urgency (overdue, due today,
 * due soon, or normal).  The selected day cell is filled with
 * {@link ThemeColors#DAY_SELECTED} and today's cell is outlined with
 * {@link ThemeColors#TODAY_RING}.</p>
 *
 * <h3>Navigation</h3>
 * <p>Left/right arrow buttons in the header navigate one month at a time.
 * Clicking any day cell fires {@link DateSelectionListener#onDateSelected}.</p>
 *
 * <h3>Layout</h3>
 * <p>Day cells are square and laid out by the inner {@link SquareCellLayout}
 * class, which computes an equal cell size from the available container width.</p>
 */
public class CalendarPanel extends JPanel {

    // -----------------------------------------------------------------------
    // Listener interface
    // -----------------------------------------------------------------------

    /**
     * Callback fired when the user clicks a day cell in the calendar.
     */
    public interface DateSelectionListener {
        /**
         * Called on the Event Dispatch Thread when a day cell is clicked.
         *
         * @param date the date that was clicked
         */
        void onDateSelected(LocalDate date);
    }

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Short day-of-week column headers (Sunday–Saturday). */
    private static final String[] DAY_HEADERS = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

    /** Number of columns in the calendar grid (always 7, one per weekday). */
    private static final int COLS = 7;

    /** Pixel gap between adjacent day cells. */
    private static final int CELL_GAP = 3;

    /** Default / minimum cell side length in pixels. */
    static final int DEFAULT_CELL = 40;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** The month currently displayed in the grid. */
    private YearMonth viewMonth    = YearMonth.now();

    /** The currently selected date (highlighted cell). */
    private LocalDate selectedDate = LocalDate.now();

    /** Data source queried for task counts and urgency colours. */
    private final TaskManager           taskManager;

    /** Listener notified when the user clicks a day. */
    private final DateSelectionListener listener;

    /**
     * Map from each rendered date to its corresponding cell panel; used to
     * repaint individual cells efficiently when the selection changes.
     */
    private final Map<LocalDate, JPanel> cellsByDate = new HashMap<>();

    // -----------------------------------------------------------------------
    // Child components
    // -----------------------------------------------------------------------

    /** Header label showing the current month and year. */
    private JLabel monthLabel;

    /** Wrapper that holds both the day-of-week header row and the cell grid. */
    private JPanel gridWrapper;

    /** The current cell grid panel (replaced on month navigation). */
    private JPanel cellGrid;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs a {@code CalendarPanel} connected to the given task manager and
     * selection listener.
     *
     * @param taskManager data source for task counts and due-date colours
     *                    (must not be {@code null})
     * @param listener    callback fired when the user selects a day; may be
     *                    {@code null} to suppress callbacks
     */
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

    // -----------------------------------------------------------------------
    // Header
    // -----------------------------------------------------------------------

    /**
     * Builds the navigation header containing a month/year label flanked by
     * previous and next month arrow buttons.
     *
     * @return the assembled header {@link JPanel}
     */
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

    /**
     * Builds an arrow navigation button that draws a filled triangle pointing
     * left or right.
     *
     * @param left {@code true} for a left-pointing (previous month) arrow,
     *             {@code false} for a right-pointing (next month) arrow
     * @return the configured {@link RippleButton}
     */
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
            @Override protected Color rippleColor() { return LABEL_BG; }
            @Override protected int   rippleArc()   { return 36; }
        };
        btn.setPreferredSize(new Dimension(36, 36));
        return btn;
    }

    // -----------------------------------------------------------------------
    // Grid construction
    // -----------------------------------------------------------------------

    /**
     * Builds the outer wrapper panel containing the day-of-week header row and
     * the initial cell grid.
     *
     * @return the assembled grid wrapper {@link JPanel}
     */
    private JPanel buildGridWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout(0, CELL_GAP));
        wrapper.setOpaque(false);

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

    /**
     * Builds the 7-column cell grid for {@link #viewMonth}, prepending blank
     * panels to align the first day with the correct weekday column.
     *
     * @return the assembled cell grid {@link JPanel}
     */
    private JPanel buildCellGrid() {
        cellsByDate.clear();

        JPanel grid = new JPanel(new SquareCellLayout(COLS, CELL_GAP));
        grid.setOpaque(false);

        LocalDate today  = LocalDate.now();
        LocalDate first  = viewMonth.atDay(1);
        int       offset = first.getDayOfWeek().getValue() % 7;

        // Blank spacers before the first day
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

    /**
     * Builds a single interactive day cell for the given date.
     *
     * <p>The cell paints itself via {@link #paintDayContent} and reacts to
     * hover (highlight), and click (selection) events.  The client property
     * {@code "hovered"} is used as a lightweight way to trigger a hover repaint
     * without a subclass.</p>
     *
     * @param date  the date this cell represents
     * @param today today's date (used to highlight the current day)
     * @return the configured day-cell {@link JPanel}
     */
    private JPanel buildDayCell(LocalDate date, LocalDate today) {
        final boolean isToday = date.equals(today);

        JPanel cell = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                final boolean isSelected = date.equals(CalendarPanel.this.selectedDate);
                final boolean hovered    = Boolean.TRUE.equals(getClientProperty("hovered"));
                final int     taskCount  = taskManager.getTaskCountForDate(date.toString());

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                final int w = getWidth(), h = getHeight();

                if (isSelected) {
                    g2.setColor(DAY_SELECTED);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                } else if (hovered) {
                    g2.setColor(DAY_HOVER);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                }

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

                JPanel prevCell = cellsByDate.get(prev);
                if (prevCell != null) prevCell.repaint();
                cell.repaint();

                if (listener != null) listener.onDateSelected(date);
            }
        });

        return cell;
    }

    // -----------------------------------------------------------------------
    // Cell painting helpers
    // -----------------------------------------------------------------------

    /**
     * Paints the day number and (if there are tasks for this date) a small task-
     * count badge inside the given cell graphics context.
     *
     * <p>The badge colour is resolved via {@link #pickBadgeColor(LocalDate)} to
     * indicate overall urgency.  When the cell is selected the badge uses a
     * semi-transparent white fill so it remains readable against the orange
     * selection background.</p>
     *
     * @param g2         graphics context, already configured with anti-aliasing
     * @param w          cell width in pixels
     * @param h          cell height in pixels
     * @param date       the date being painted
     * @param isSelected {@code true} if this cell is currently selected
     * @param isToday    {@code true} if this cell represents today
     * @param taskCount  number of tasks scheduled for this date
     */
    private void paintDayContent(Graphics2D g2, int w, int h,
                                  LocalDate date, boolean isSelected,
                                  boolean isToday, int taskCount) {
        Color numColor = isSelected ? Color.WHITE
                       : isToday    ? BUTTON_HOVER
                                    : TEXT_DARK;
        Font  numFont  = new Font("SansSerif", isToday ? Font.BOLD : Font.PLAIN, 13);
        g2.setFont(numFont);
        FontMetrics nfm    = g2.getFontMetrics();
        String      numStr = String.valueOf(date.getDayOfMonth());
        int         numW   = nfm.stringWidth(numStr);

        if (taskCount > 0) {
            Font        bFont  = new Font("SansSerif", Font.BOLD, 8);
            FontMetrics bfm    = g2.getFontMetrics(bFont);
            String      badge  = taskCount > 9 ? "9+" : String.valueOf(taskCount);
            int         bTextW = bfm.stringWidth(badge);
            int         bPadX  = 4;
            int         bw     = Math.max(bTextW + bPadX * 2, 14);
            int         bh     = bfm.getAscent() + bfm.getDescent() + 2;

            int bx = (w - bw) / 2;
            int by = h - bh - 2;

            g2.setColor(isSelected
                    ? new Color(255, 255, 255, 200)
                    : pickBadgeColor(date));
            g2.fillRoundRect(bx, by, bw, bh, bh, bh);

            g2.setFont(bFont);
            g2.setColor(isSelected ? DAY_SELECTED : Color.WHITE);
            int btx = bx + (bw - bTextW) / 2;
            int bty = by + bfm.getAscent()
                        + (bh - bfm.getAscent() - bfm.getDescent()) / 2;
            g2.drawString(badge, btx, bty);
        }

        g2.setFont(numFont);
        g2.setColor(numColor);
        int numX = (w - numW) / 2;
        int numY = (h + nfm.getAscent() - nfm.getDescent()) / 2;
        g2.drawString(numStr, numX, numY);
    }

    /**
     * Determines the urgency colour for the task-count badge on a given date by
     * scanning all tasks scheduled for that date.
     *
     * <p>Resolution priority (first match wins):</p>
     * <ol>
     *   <li>Any non-done task is overdue → {@link ThemeColors#COUNTDOWN_OVERDUE}</li>
     *   <li>Any non-done task is due today → {@link ThemeColors#COUNTDOWN_TODAY}</li>
     *   <li>Any non-done task is due within 3 days → {@link ThemeColors#COUNTDOWN_SOON}</li>
     *   <li>Default → {@link ThemeColors#COUNTDOWN_NORMAL}</li>
     * </ol>
     *
     * @param date the date whose tasks should be examined
     * @return the urgency {@link Color} for the badge
     */
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

    // -----------------------------------------------------------------------
    // Refresh / navigation
    // -----------------------------------------------------------------------

    /**
     * Replaces the cell grid with a freshly built grid for {@link #viewMonth}
     * and updates the month/year header label.
     */
    private void rebuildCells() {
        monthLabel.setText(formatMonthTitle());
        gridWrapper.remove(cellGrid);
        cellGrid = buildCellGrid();
        gridWrapper.add(cellGrid, BorderLayout.CENTER);
        gridWrapper.revalidate();
        gridWrapper.repaint();
    }

    /**
     * Requests a repaint of all currently visible day cells (e.g. after task
     * counts have changed).  Does not rebuild the grid.
     */
    public void refresh() {
        for (JPanel c : cellsByDate.values()) c.repaint();
    }

    /**
     * Programmatically selects a date.  If the date falls outside the current
     * view month the grid is rebuilt for the new month first.  The previously
     * selected cell is repainted to remove the selection highlight.
     *
     * @param date the date to select (must not be {@code null})
     */
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

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the currently selected date.
     *
     * @return the selected {@link LocalDate} (never {@code null}; defaults to
     *         today when the panel is first constructed)
     */
    public LocalDate getSelectedDate() { return selectedDate; }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Returns a formatted month/year title string for the current view month
     * (e.g. {@code "December  2025"}).
     *
     * @return formatted title string
     */
    private String formatMonthTitle() {
        return viewMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + "  " + viewMonth.getYear();
    }

    /**
     * Returns a fixed preferred size so the calendar maintains a consistent
     * appearance regardless of parent layout constraints.
     *
     * @return preferred size of 320 × 340 pixels
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(320, 340);
    }

    // -----------------------------------------------------------------------
    // Inner layout manager
    // -----------------------------------------------------------------------

    /**
     * A custom {@link LayoutManager} that arranges a fixed number of columns of
     * equal-size square cells, computing the cell side length from the available
     * container width.
     *
     * <p>This ensures that calendar cells are always square and fill the full
     * width of the panel regardless of the window size.</p>
     */
    static final class SquareCellLayout implements LayoutManager {

        /** Number of columns (always 7 for a calendar). */
        private final int cols;

        /** Pixel gap between adjacent cells (horizontal and vertical). */
        private final int gap;

        /**
         * Constructs a {@code SquareCellLayout} for the given column count and gap.
         *
         * @param cols number of columns
         * @param gap  gap between cells in pixels
         */
        SquareCellLayout(int cols, int gap) {
            this.cols = cols;
            this.gap  = gap;
        }

        /**
         * Computes the square cell side length from the container's current width.
         * If the container has no width yet, the method walks ancestor containers;
         * if no ancestor width is available either, a fallback of
         * {@code cols * DEFAULT_CELL + (cols-1) * gap} is used.
         *
         * @param p the container to measure
         * @return cell side length in pixels (at least {@link CalendarPanel#DEFAULT_CELL})
         */
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

        /**
         * Positions each component in a row-major grid of square cells.
         *
         * @param p the container to lay out
         */
        @Override
        public void layoutContainer(Container p) {
            Insets ins = p.getInsets();
            int    cs  = cellSize(p);
            int    x   = ins.left;
            int    y   = ins.top;
            int    col = 0;
            for (Component c : p.getComponents()) {
                c.setBounds(x, y, cs, cs);
                x += cs + gap;
                if (++col == cols) {
                    col = 0;
                    x   = ins.left;
                    y  += cs + gap;
                }
            }
        }

        /** {@inheritDoc} */
        @Override public Dimension preferredLayoutSize(Container p) { return computeSize(p); }

        /** {@inheritDoc} */
        @Override public Dimension minimumLayoutSize(Container p)   { return computeSize(p); }

        /**
         * Computes the total size required to display all components in the grid.
         *
         * @param p the container to measure
         * @return the required {@link Dimension}
         */
        private Dimension computeSize(Container p) {
            Insets ins  = p.getInsets();
            int    cs   = cellSize(p);
            int    n    = p.getComponentCount();
            int    rows = (int) Math.ceil((double) n / cols);
            int    tw   = ins.left + ins.right  + cols * cs + (cols - 1) * gap;
            int    th   = ins.top  + ins.bottom + rows * cs + Math.max(0, rows - 1) * gap;
            return new Dimension(tw, th);
        }

        /** {@inheritDoc} */
        @Override public void addLayoutComponent(String name, Component c) {}

        /** {@inheritDoc} */
        @Override public void removeLayoutComponent(Component c) {}
    }
}