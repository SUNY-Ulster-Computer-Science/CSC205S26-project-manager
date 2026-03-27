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
 * A themed date-picker widget that renders as a rounded button field.
 *
 * <p>Clicking the field opens a floating calendar popup aligned below it.
 * Navigation arrows let the user step through months; only today and future
 * dates are selectable (past dates are rendered greyed-out). Clicking outside
 * the popup dismisses it.</p>
 *
 * <p>The selected date is exposed both as a {@link LocalDate} via
 * {@link #getSelectedDate()} and as a formatted {@code "yyyy-MM-dd"} string
 * via {@link #getDateText()}, keeping it a drop-in replacement for the
 * {@link RoundedTextField} previously used for date input.</p>
 *
 * @see MainApp
 */
public class CalendarDatePicker extends JPanel {

    // ── Date formatting ───────────────────────────────────────────────────
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] DAY_NAMES = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

    // ── State ─────────────────────────────────────────────────────────────
    private LocalDate  selectedDate  = null;
    private YearMonth  currentMonth  = YearMonth.now();

    // ── UI components ─────────────────────────────────────────────────────
    private final JButton  displayButton;
    private       JWindow  popupWindow   = null;

    /** AWTEventListener registered to close the popup on outside-click. */
    private AWTEventListener outsideClickListener = null;

    // ══════════════════════════════════════════════════════════════════════
    //  Construction
    // ══════════════════════════════════════════════════════════════════════

    public CalendarDatePicker() {
        setLayout(new BorderLayout());
        setOpaque(false);

        displayButton = new JButton("Select date…") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        displayButton.setContentAreaFilled(false);
        displayButton.setBorderPainted(false);
        displayButton.setFocusPainted(false);
        displayButton.setOpaque(false);
        displayButton.setHorizontalAlignment(SwingConstants.LEFT);
        displayButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        displayButton.setForeground(new Color(0x99, 0x88, 0x77));   // placeholder grey
        displayButton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 40));
        displayButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        displayButton.addActionListener(e -> togglePopup());

        // Calendar icon overlay (painted in paintComponent below)
        add(displayButton, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw a small calendar glyph on the right side of the field
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int iconSize = 16;
        int ix = getWidth() - iconSize - 12;
        int iy = (getHeight() - iconSize) / 2;
        g2.setColor(LABEL_BG);
        g2.fillRoundRect(ix, iy, iconSize, iconSize, 4, 4);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        // Top bar (binding)
        g2.drawLine(ix + 3, iy + 4, ix + iconSize - 3, iy + 4);
        // Grid dots (simplified calendar grid)
        int dotSize = 2;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int dx = ix + 3 + col * 4;
                int dy = iy + 7 + row * 4;
                g2.fillRect(dx, dy, dotSize, dotSize);
            }
        }
        // Binding tabs
        g2.setColor(BUTTON_HOVER);
        g2.fillRoundRect(ix + 4, iy - 1, 2, 4, 2, 2);
        g2.fillRoundRect(ix + iconSize - 6, iy - 1, 2, 4, 2, 2);
        g2.dispose();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════════════════════════════

    /** @return the currently selected date, or {@code null} if none chosen */
    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    /**
     * @return the selected date formatted as {@code "yyyy-MM-dd"},
     *         or an empty string if no date has been chosen
     */
    public String getDateText() {
        return selectedDate != null ? selectedDate.format(DISPLAY_FMT) : "";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Popup lifecycle
    // ══════════════════════════════════════════════════════════════════════

    private void togglePopup() {
        if (popupWindow != null && popupWindow.isVisible()) {
            closePopup();
        } else {
            openPopup();
        }
    }

    private void openPopup() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        popupWindow = new JWindow(owner);
        popupWindow.setContentPane(buildCalendarPanel());
        popupWindow.pack();

        // Position directly below this field
        Point loc = getLocationOnScreen();
        int x = loc.x;
        int y = loc.y + getHeight() + 2;

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (x + popupWindow.getWidth() > screen.width)
            x = screen.width - popupWindow.getWidth();
        if (y + popupWindow.getHeight() > screen.height)
            y = loc.y - popupWindow.getHeight() - 2;

        popupWindow.setLocation(x, y);
        popupWindow.setVisible(true);

        // Dismiss when the user clicks outside the popup
        outsideClickListener = event -> {
            if (event instanceof MouseEvent &&
                    ((MouseEvent) event).getID() == MouseEvent.MOUSE_PRESSED) {
                MouseEvent me = (MouseEvent) event;
                if (popupWindow != null && popupWindow.isVisible()) {
                    if (!popupWindow.getBounds().contains(me.getLocationOnScreen())) {
                        closePopup();
                    }
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(
                outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void closePopup() {
        if (popupWindow != null) {
            popupWindow.dispose();
            popupWindow = null;
        }
        if (outsideClickListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
            outsideClickListener = null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Calendar panel construction
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildCalendarPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(CONTENT_CREAM);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LABEL_BG, 2),
                BorderFactory.createEmptyBorder(0, 0, 4, 0)));
        panel.setPreferredSize(new Dimension(252, 220));

        // ── Month navigation header ───────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(OUTER_ORANGE);
        header.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JLabel monthLabel = buildMonthLabel();
        JButton prevBtn = buildNavButton("‹");
        JButton nextBtn = buildNavButton("›");

        prevBtn.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshGrid(panel, monthLabel);
        });
        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshGrid(panel, monthLabel);
        });

        header.add(prevBtn,    BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextBtn,    BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // ── Day grid ─────────────────────────────────────────────────────
        panel.add(buildDayGrid(), BorderLayout.CENTER);
        return panel;
    }

    /** Re-creates the day grid after a month navigation press. */
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

    private JLabel buildMonthLabel() {
        JLabel lbl = new JLabel(monthTitle(), SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(TEXT_DARK);
        return lbl;
    }

    private String monthTitle() {
        return currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + "  " + currentMonth.getYear();
    }

    /**
     * Builds the 7-column grid of day-name headers + day buttons for
     * {@link #currentMonth}.
     */
    private JPanel buildDayGrid() {
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setBackground(CONTENT_CREAM);
        grid.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        // Day-name row
        for (String name : DAY_NAMES) {
            JLabel lbl = new JLabel(name, SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(BUTTON_HOVER);
            grid.add(lbl);
        }

        LocalDate today  = LocalDate.now();
        LocalDate first  = currentMonth.atDay(1);

        // java.time: Monday=1 … Sunday=7  →  convert to Sunday=0 … Saturday=6
        int startCol = first.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < startCol; i++) grid.add(new JLabel(""));

        int daysInMonth = currentMonth.lengthOfMonth();
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date    = currentMonth.atDay(d);
            boolean isPast    = date.isBefore(today);
            boolean isToday   = date.equals(today);
            boolean isChosen  = date.equals(selectedDate);

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

    // ══════════════════════════════════════════════════════════════════════
    //  Small component factories
    // ══════════════════════════════════════════════════════════════════════

    private JButton buildDayButton(String label,
                                   boolean isPast,
                                   boolean isToday,
                                   boolean isChosen) {
        // Background color captured for use inside the anonymous class
        Color bg = isChosen  ? BUTTON_ORANGE
                 : isPast    ? new Color(0xE8, 0xE0, 0xD6)
                             : CONTENT_CREAM;

        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
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
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        return btn;
    }

    private JButton buildNavButton(String symbol) {
        JButton btn = new JButton(symbol);
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setForeground(TEXT_DARK);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
