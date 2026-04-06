package projectmanager;

import java.awt.Color;

/**
 * Centralised color constants for the Orange™ visual theme.
 *
 * <p>Every custom UI component references this class so the whole
 * palette can be adjusted in one place.</p>
 */
public final class ThemeColors {

    private ThemeColors() {}

    // ── Base palette ──────────────────────────────────────────────────────

    /** Outer frame background. */
    public static final Color OUTER_ORANGE  = new Color(0xE8, 0xA8, 0x65);

    /** Inner panel fill. */
    public static final Color INNER_PEACH   = new Color(0xFD, 0xDC, 0xB5);

    /** Default button background. */
    public static final Color BUTTON_ORANGE = new Color(0xE8, 0xA8, 0x65);

    /** Button background on mouse-over. */
    public static final Color BUTTON_HOVER  = new Color(0xD4, 0x96, 0x50);

    /** Task list / scroll-pane background. */
    public static final Color CONTENT_CREAM = new Color(0xFF, 0xF5, 0xEB);

    /** Pill-label and list-selection background. */
    public static final Color LABEL_BG      = new Color(0xD4, 0xA7, 0x6A);

    /** Primary text color used on labels and list items. */
    public static final Color TEXT_DARK     = new Color(0x3D, 0x2B, 0x10);

    /** Muted secondary text (notes preview, placeholders). */
    public static final Color TEXT_MUTED    = new Color(0x99, 0x88, 0x77);

    /** Card background (white with slight warm tint). */
    public static final Color CARD_BG       = new Color(0xFF, 0xFB, 0xF6);

    /** Card border / divider line. */
    public static final Color CARD_BORDER   = new Color(0xE8, 0xD8, 0xC4);

    /** Calendar today highlight ring. */
    public static final Color TODAY_RING    = new Color(0xD4, 0x96, 0x50);

    /** Calendar selected-day fill. */
    public static final Color DAY_SELECTED  = new Color(0xE8, 0xA8, 0x65);

    /** Calendar hover color. */
    public static final Color DAY_HOVER     = new Color(0xFF, 0xEA, 0xD0);

    // ── Status colors ─────────────────────────────────────────────────────

    public static final Color STATUS_TODO        = new Color(0x90, 0x90, 0x90);
    public static final Color STATUS_IN_PROGRESS = new Color(0x42, 0x8B, 0xCA);
    public static final Color STATUS_DONE        = new Color(0x5C, 0xB8, 0x5C);

    /** Returns the display color for a given task status. */
    public static Color forStatus(Task.Status status) {
        if (status == null) return STATUS_TODO;
        switch (status) {
            case TODO:        return STATUS_TODO;
            case IN_PROGRESS: return STATUS_IN_PROGRESS;
            case DONE:        return STATUS_DONE;
            default:          return STATUS_TODO;
        }
    }

    // ── Priority colors ───────────────────────────────────────────────────

    public static final Color PRIORITY_LOW    = new Color(0x8B, 0xC3, 0x4A);
    public static final Color PRIORITY_MEDIUM = new Color(0xFF, 0x98, 0x00);
    public static final Color PRIORITY_HIGH   = new Color(0xF4, 0x43, 0x36);

    /** Returns the display color for a given priority level. */
    public static Color forPriority(Task.Priority priority) {
        if (priority == null) return PRIORITY_MEDIUM;
        switch (priority) {
            case LOW:    return PRIORITY_LOW;
            case MEDIUM: return PRIORITY_MEDIUM;
            case HIGH:   return PRIORITY_HIGH;
            default:     return PRIORITY_MEDIUM;
        }
    }

    // ── Countdown colors ──────────────────────────────────────────────────

    /** Overdue badge background. */
    public static final Color COUNTDOWN_OVERDUE  = new Color(0xF4, 0x43, 0x36);

    /** Due-today badge background. */
    public static final Color COUNTDOWN_TODAY    = new Color(0xFF, 0x98, 0x00);

    /** Due-soon (≤ 3 days) badge background. */
    public static final Color COUNTDOWN_SOON     = new Color(0xFF, 0xC1, 0x07);

    /** Normal countdown badge background. */
    public static final Color COUNTDOWN_NORMAL   = new Color(0x5C, 0xB8, 0x5C);

    /** Done badge background. */
    public static final Color COUNTDOWN_DONE     = new Color(0x5C, 0xB8, 0x5C);

    /** Returns the appropriate countdown color for a task. */
    public static Color forCountdown(Task task) {
        if (task.getStatus() == Task.Status.DONE) return COUNTDOWN_DONE;
        long days = task.daysUntilDue();
        if (days < 0)  return COUNTDOWN_OVERDUE;
        if (days == 0) return COUNTDOWN_TODAY;
        if (days <= 3) return COUNTDOWN_SOON;
        return COUNTDOWN_NORMAL;
    }
}
