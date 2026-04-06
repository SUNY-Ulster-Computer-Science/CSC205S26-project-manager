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
 * Navigation arrows let the user step through months.  Clicking outside
 * the popup dismisses it.</p>
 *
 * <p>Call {@link #setSelectedDateExternal(LocalDate)} to pre-fill the
 * picker when opening an edit dialog.</p>
 */
public class CalendarDatePicker extends JPanel {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] DAY_NAMES = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

    private LocalDate selectedDate = null;
    private YearMonth currentMonth = YearMonth.now();

    private final JButton displayButton;
    private       JWindow popupWindow         = null;
    private AWTEventListener outsideClickListener = null;

    // ── Constructor ───────────────────────────────────────────────────────

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
            // White background → use a warm orange ripple so it's visible
            @Override protected Color rippleColor() { return LABEL_BG; }
        };
        displayButton.setHorizontalAlignment(SwingConstants.LEFT);
        displayButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        displayButton.setForeground(new Color(0x99, 0x88, 0x77));
        displayButton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 40));
        displayButton.addActionListener(e -> togglePopup());

        add(displayButton, BorderLayout.CENTER);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** @return the selected date, or {@code null} if none */
    public LocalDate getSelectedDate() { return selectedDate; }

    /** @return {@code "yyyy-MM-dd"} or empty string */
    public String getDateText() {
        return selectedDate != null ? selectedDate.format(DISPLAY_FMT) : "";
    }

    /**
     * Pre-fills the picker (used by the Edit dialog to show the task's current date).
     *
     * @param date the date to pre-select
     */
    public void setSelectedDateExternal(LocalDate date) {
        if (date == null) return;
        selectedDate  = date;
        currentMonth  = YearMonth.from(date);
        displayButton.setText(date.format(DISPLAY_FMT));
        displayButton.setForeground(TEXT_DARK);
    }

    // ── Popup lifecycle ───────────────────────────────────────────────────

    private void togglePopup() {
        if (popupWindow != null && popupWindow.isVisible()) closePopup();
        else openPopup();
    }

    private void openPopup() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        popupWindow  = new JWindow(owner);
        popupWindow.setContentPane(buildCalendarPanel());
        popupWindow.pack();

        Point loc = getLocationOnScreen();
        int x = loc.x;
        int y = loc.y + getHeight() + 2;

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (x + popupWindow.getWidth()  > screen.width)  x = screen.width  - popupWindow.getWidth();
        if (y + popupWindow.getHeight() > screen.height) y = loc.y - popupWindow.getHeight() - 2;

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

    private void closePopup() {
        if (popupWindow != null) { popupWindow.dispose(); popupWindow = null; }
        if (outsideClickListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
            outsideClickListener = null;
        }
    }

    // ── Calendar panel ────────────────────────────────────────────────────

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

        prevBtn.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); refreshGrid(panel, monthLabel); });
        nextBtn.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1);  refreshGrid(panel, monthLabel); });

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

    private JPanel buildDayGrid() {
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setBackground(CONTENT_CREAM);
        grid.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        for (String name : DAY_NAMES) {
            JLabel lbl = new JLabel(name, SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(BUTTON_HOVER);
            grid.add(lbl);
        }

        LocalDate today = LocalDate.now();
        LocalDate first = currentMonth.atDay(1);
        int startCol = first.getDayOfWeek().getValue() % 7;
        for (int i = 0; i < startCol; i++) grid.add(new JLabel(""));

        int days = currentMonth.lengthOfMonth();
        for (int d = 1; d <= days; d++) {
            LocalDate date   = currentMonth.atDay(d);
            boolean isPast   = date.isBefore(today);
            boolean isToday  = date.equals(today);
            boolean isChosen = date.equals(selectedDate);

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

    private JButton buildDayButton(String label, boolean isPast, boolean isToday, boolean isChosen) {
        Color bg     = isChosen ? BUTTON_ORANGE
                     : isPast   ? new Color(0xE8, 0xE0, 0xD6)
                                : CONTENT_CREAM;
        // White ripple on orange (chosen), warm-orange ripple on light backgrounds
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

    private JButton buildNavButton(String symbol) {
        RippleButton btn = new RippleButton(symbol) {
            @Override
            protected void paintBackground(Graphics2D g2) {
                if (getModel().isRollover()) {
                    // Soft highlight pill on the orange header
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

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int iconSize = 16;
        int ix = getWidth() - iconSize - 12;
        int iy = (getHeight() - iconSize) / 2;
        g2.setColor(LABEL_BG);
        g2.fillRoundRect(ix, iy, iconSize, iconSize, 4, 4);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(ix + 3, iy + 4, ix + iconSize - 3, iy + 4);
        int dotSize = 2;
        for (int row = 0; row < 2; row++)
            for (int col = 0; col < 3; col++)
                g2.fillRect(ix + 3 + col * 4, iy + 7 + row * 4, dotSize, dotSize);
        g2.setColor(BUTTON_HOVER);
        g2.fillRoundRect(ix + 4,             iy - 1, 2, 4, 2, 2);
        g2.fillRoundRect(ix + iconSize - 6,  iy - 1, 2, 4, 2, 2);
        g2.dispose();
    }
}