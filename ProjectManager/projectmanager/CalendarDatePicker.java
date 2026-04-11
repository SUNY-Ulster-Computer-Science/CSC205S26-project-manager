package projectmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import static projectmanager.ThemeColors.*;

/**
 * An inline calendar date-picker widget for use inside dialogs and forms.
 *
 * <p>The component renders as a single pill-shaped button that displays the
 * currently selected date (or a "Select date…" placeholder).  Clicking the
 * button opens a lightweight {@link JWindow} popup containing a month-view
 * calendar.  Clicking outside the popup closes it automatically via an
 * AWT-level event listener.</p>
 *
 * <h3>Thread safety</h3>
 * <p>All interaction occurs on the Event Dispatch Thread; no additional
 * synchronisation is required.</p>
 *
 * <h3>Typical usage</h3>
 * <pre>
 * CalendarDatePicker picker = new CalendarDatePicker();
 * form.add(picker, gbc);
 * // Later:
 * String isoDate = picker.getDateText(); // e.g. "2025-12-31"
 * </pre>
 */
public class CalendarDatePicker extends JPanel {

    /** Formatter used for both the display button label and {@link #getDateText()}. */
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Short column headers for the day-of-week row. */
    private static final String[] DAY_NAMES = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

    /** Currently selected date, or {@code null} if none has been chosen. */
    private LocalDate selectedDate = null;

    /** Month currently displayed in the popup calendar grid. */
    private YearMonth currentMonth = YearMonth.now();

    /**
     * The pill-shaped button that shows the selected date and toggles the
     * calendar popup.
     */
    private final JButton displayButton;

    /** The popup window that contains the calendar grid, or {@code null} when closed. */
    private JWindow popupWindow = null;

    /**
     * System-wide AWT event listener installed while the popup is open to detect
     * mouse presses outside the popup (used to close it automatically).
     */
    private AWTEventListener outsideClickListener = null;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs a new {@code CalendarDatePicker} with no date pre-selected.
     */
    public CalendarDatePicker() {
        setLayout(new BorderLayout());
        setOpaque(false);

        displayButton = new RippleButton("Select date…") {
            @Override
            protected void paintBackground(Graphics2D g2) {
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            }
            @Override protected int   rippleArc()   { return 30; }
            @Override protected Color rippleColor() { return LABEL_BG; }
        };
        displayButton.setHorizontalAlignment(SwingConstants.LEFT);
        displayButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        displayButton.setForeground(new Color(0x99, 0x88, 0x77));
        displayButton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 40));
        displayButton.addActionListener(e -> togglePopup());

        add(displayButton, BorderLayout.CENTER);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns the currently selected date, or {@code null} if no date has been
     * chosen by the user.
     *
     * @return selected {@link LocalDate}, or {@code null}
     */
    public LocalDate getSelectedDate() { return selectedDate; }

    /**
     * Returns the currently selected date as an ISO-8601 string (e.g.
     * {@code "2025-12-31"}), or an empty string if no date has been selected.
     *
     * @return ISO-8601 date string, or {@code ""}
     */
    public String getDateText() {
        return selectedDate != null ? selectedDate.format(DISPLAY_FMT) : "";
    }

    /**
     * Programmatically sets the selected date (e.g. when pre-populating the
     * picker for an edit dialog).  The button label and the internal view month
     * are updated accordingly.  A {@code null} argument is silently ignored.
     *
     * @param date the date to select; must not be {@code null}
     */
    public void setSelectedDateExternal(LocalDate date) {
        if (date == null) return;
        selectedDate  = date;
        currentMonth  = YearMonth.from(date);
        displayButton.setText(date.format(DISPLAY_FMT));
        displayButton.setForeground(TEXT_DARK);
    }

    // -----------------------------------------------------------------------
    // Popup lifecycle
    // -----------------------------------------------------------------------

    /**
     * Toggles the calendar popup: opens it if closed, closes it if open.
     */
    private void togglePopup() {
        if (popupWindow != null && popupWindow.isVisible()) closePopup();
        else openPopup();
    }

    /**
     * Opens the calendar popup below the display button.  The popup is
     * positioned to remain fully on screen even near the bottom or right edges
     * of the display.  An AWT-level mouse listener is installed to close the
     * popup when the user clicks anywhere outside it.
     */
    private void openPopup() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        popupWindow  = new JWindow(owner);
        popupWindow.setContentPane(buildCalendarPanel());
        popupWindow.pack();

        Point loc = getLocationOnScreen();
        int   x   = loc.x;
        int   y   = loc.y + getHeight() + 2;

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (x + popupWindow.getWidth()  > screen.width)
            x = screen.width  - popupWindow.getWidth();
        if (y + popupWindow.getHeight() > screen.height)
            y = loc.y - popupWindow.getHeight() - 2;

        popupWindow.setLocation(x, y);
        popupWindow.setVisible(true);

        outsideClickListener = event -> {
            if (event instanceof MouseEvent &&
                    ((MouseEvent) event).getID() == MouseEvent.MOUSE_PRESSED) {
                MouseEvent me = (MouseEvent) event;
                if (popupWindow != null && popupWindow.isVisible() &&
                        !popupWindow.getBounds().contains(me.getLocationOnScreen())) {
                    closePopup();
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(
                outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    /**
     * Disposes the popup window and removes the AWT outside-click listener.
     */
    private void closePopup() {
        if (popupWindow != null) { popupWindow.dispose(); popupWindow = null; }
        if (outsideClickListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
            outsideClickListener = null;
        }
    }

    // -----------------------------------------------------------------------
    // Calendar panel construction
    // -----------------------------------------------------------------------

    /**
     * Builds and returns the complete calendar panel placed inside the popup
     * window.  Includes a header with navigation buttons and the day grid.
     *
     * @return the assembled calendar {@link JPanel}
     */
    private JPanel buildCalendarPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(CONTENT_CREAM);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LABEL_BG, 2),
                BorderFactory.createEmptyBorder(0, 0, 4, 0)));
        panel.setPreferredSize(new Dimension(252, 220));

        JLabel monthLabel = buildMonthLabel();
        JButton prevBtn   = buildNavButton("‹");
        JButton nextBtn   = buildNavButton("›");

        prevBtn.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshGrid(panel, monthLabel);
        });
        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshGrid(panel, monthLabel);
        });

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(OUTER_ORANGE);
        header.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        header.add(prevBtn,    BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextBtn,    BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);
        panel.add(buildDayGrid(), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Rebuilds the day grid inside the calendar panel after the user navigates
     * to a different month.
     *
     * @param panel      the calendar panel whose centre component should be replaced
     * @param monthLabel the header label to update with the new month/year text
     */
    private void refreshGrid(JPanel panel, JLabel monthLabel) {
        monthLabel.setText(monthTitle());
        BorderLayout layout = (BorderLayout) panel.getLayout();
        Component old = layout.getLayoutComponent(BorderLayout.CENTER);
        if (old != null) panel.remove(old);
        panel.add(buildDayGrid(), BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
        if (popupWindow != null) popupWindow.pack();
    }

    /**
     * Builds and returns the centred month/year header label.
     *
     * @return configured month label
     */
    private JLabel buildMonthLabel() {
        JLabel lbl = new JLabel(monthTitle(), SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(TEXT_DARK);
        return lbl;
    }

    /**
     * Returns the month/year string for the current view month
     * (e.g. {@code "December  2025"}).
     *
     * @return formatted month/year string
     */
    private String monthTitle() {
        return currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + "  " + currentMonth.getYear();
    }

    /**
     * Builds and returns the day grid panel containing the day-of-week header
     * row and one button per day of {@link #currentMonth}.
     *
     * @return the assembled day grid {@link JPanel}
     */
    private JPanel buildDayGrid() {
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setBackground(CONTENT_CREAM);
        grid.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        // Day-of-week header row
        for (String name : DAY_NAMES) {
            JLabel lbl = new JLabel(name, SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(BUTTON_HOVER);
            grid.add(lbl);
        }

        LocalDate today    = LocalDate.now();
        LocalDate first    = currentMonth.atDay(1);
        int       startCol = first.getDayOfWeek().getValue() % 7;

        // Blank cells before the first day of the month
        for (int i = 0; i < startCol; i++) grid.add(new JLabel(""));

        int days = currentMonth.lengthOfMonth();
        for (int d = 1; d <= days; d++) {
            LocalDate date     = currentMonth.atDay(d);
            boolean   isPast   = date.isBefore(today);
            boolean   isToday  = date.equals(today);
            boolean   isChosen = date.equals(selectedDate);

            JButton btn = buildDayButton(String.valueOf(d), isPast, isToday, isChosen);
            if (!isPast) {
                btn.addActionListener(e -> {
                    selectedDate = date;
                    displayButton.setText(date.format(DISPLAY_FMT));
                    displayButton.setForeground(TEXT_DARK);
                    closePopup();
                });
            }
            grid.add(btn);
        }
        return grid;
    }

    /**
     * Builds a single day cell button for the calendar grid.
     *
     * @param label    the day-of-month number as a string
     * @param isPast   {@code true} if the date is before today (rendered greyed out)
     * @param isToday  {@code true} if the date is today (rendered bold)
     * @param isChosen {@code true} if the date is the currently selected date
     * @return the configured day button
     */
    private JButton buildDayButton(String label, boolean isPast,
                                   boolean isToday, boolean isChosen) {
        Color bg     = isChosen ? BUTTON_ORANGE
                     : isPast   ? new Color(0xE8, 0xE0, 0xD6)
                                : CONTENT_CREAM;
        Color ripple = isChosen ? Color.WHITE : LABEL_BG;

        RippleButton btn = new RippleButton(label) {
            @Override
            protected void paintBackground(Graphics2D g2) {
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            @Override protected int   rippleArc()   { return 10; }
            @Override protected Color rippleColor() { return ripple; }
        };
        btn.setFont(new Font("SansSerif", isToday ? Font.BOLD : Font.PLAIN, 12));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setMargin(new Insets(2, 0, 2, 0));

        if (isChosen) {
            btn.setForeground(Color.WHITE);
        } else if (isPast) {
            btn.setForeground(new Color(0xBB, 0xAA, 0x99));
            btn.setCursor(Cursor.getDefaultCursor());
        } else {
            btn.setForeground(isToday ? BUTTON_HOVER : TEXT_DARK);
        }
        return btn;
    }

    /**
     * Builds a navigation arrow button ({@code "‹"} or {@code "›"}) for the
     * calendar header.
     *
     * @param symbol the arrow character to display
     * @return the configured navigation {@link RippleButton}
     */
    private JButton buildNavButton(String symbol) {
        RippleButton btn = new RippleButton(symbol) {
            @Override
            protected void paintBackground(Graphics2D g2) {
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 50));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
            }
            @Override protected int   rippleArc()   { return 8; }
            @Override protected Color rippleColor() { return Color.WHITE; }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setForeground(TEXT_DARK);
        return btn;
    }

    // -----------------------------------------------------------------------
    // Painting
    // -----------------------------------------------------------------------

    /**
     * Paints a small calendar icon on the right side of the display button,
     * overlaid on top of the button's own rendering, to hint that clicking will
     * open a date-picker popup.
     *
     * @param g the graphics context provided by Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int iconSize = 16;
        int ix = getWidth()  - iconSize - 12;
        int iy = (getHeight() - iconSize) / 2;

        // Calendar body
        g2.setColor(LABEL_BG);
        g2.fillRoundRect(ix, iy, iconSize, iconSize, 4, 4);

        // Grid dots
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(ix + 3, iy + 4, ix + iconSize - 3, iy + 4);
        int dotSize = 2;
        for (int row = 0; row < 2; row++)
            for (int col = 0; col < 3; col++)
                g2.fillRect(ix + 3 + col * 4, iy + 7 + row * 4, dotSize, dotSize);

        // Ring clips at top (representing calendar page rings)
        g2.setColor(BUTTON_HOVER);
        g2.fillRoundRect(ix + 4,            iy - 1, 2, 4, 2, 2);
        g2.fillRoundRect(ix + iconSize - 6, iy - 1, 2, 4, 2, 2);

        g2.dispose();
    }
}