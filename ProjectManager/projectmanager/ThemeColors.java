package projectmanager;

import java.awt.Color;

/**
 * Central palette for the Orange™ Task Manager UI.
 *
 * <p>All colour constants used throughout the application are declared here as
 * {@code public static final} fields so that a single edit propagates everywhere.
 * The class is non-instantiable (utility class pattern).</p>
 *
 * <p>In addition to raw colour constants the class provides three factory methods
 * that resolve a semantic colour from a domain value:</p>
 * <ul>
 *   <li>{@link #forStatus(Task.Status)} – traffic-light colours for task status</li>
 *   <li>{@link #forPriority(Task.Priority)} – colours for priority levels</li>
 *   <li>{@link #forCountdown(Task)} – urgency colours for countdown badges</li>
 * </ul>
 */
public final class ThemeColors {

    /** Prevent instantiation. */
    private ThemeColors() {}

    // -----------------------------------------------------------------------
    // Structural / chrome colours
    // -----------------------------------------------------------------------

    /** Warm orange used for the outermost application border / frame background. */
    public static final Color OUTER_ORANGE  = new Color(0xE8, 0xA8, 0x65);

    /** Light peach fill used for the inner rounded panel that contains the main layout. */
    public static final Color INNER_PEACH   = new Color(0xFD, 0xDC, 0xB5);

    // -----------------------------------------------------------------------
    // Button colours
    // -----------------------------------------------------------------------

    /** Default background colour for primary action buttons. */
    public static final Color BUTTON_ORANGE = new Color(0xE8, 0xA8, 0x65);

    /** Background colour shown when the user hovers over a primary action button. */
    public static final Color BUTTON_HOVER  = new Color(0xD4, 0x96, 0x50);

    // -----------------------------------------------------------------------
    // Surface / background colours
    // -----------------------------------------------------------------------

    /** Cream-white background used for the task-list scroll area. */
    public static final Color CONTENT_CREAM = new Color(0xFF, 0xF5, 0xEB);

    /** Muted caramel used as the background for tag/pill labels. */
    public static final Color LABEL_BG      = new Color(0xD4, 0xA7, 0x6A);

    // -----------------------------------------------------------------------
    // Text colours
    // -----------------------------------------------------------------------

    /** Dark brown used for primary text (headings, task titles, form labels). */
    public static final Color TEXT_DARK     = new Color(0x3D, 0x2B, 0x10);

    /** Muted grey-brown used for secondary / placeholder text. */
    public static final Color TEXT_MUTED    = new Color(0x99, 0x88, 0x77);

    // -----------------------------------------------------------------------
    // Card colours
    // -----------------------------------------------------------------------

    /** Near-white background used for individual task cards. */
    public static final Color CARD_BG       = new Color(0xFF, 0xFB, 0xF6);

    /** Soft tan border drawn around task cards. */
    public static final Color CARD_BORDER   = new Color(0xE8, 0xD8, 0xC4);

    // -----------------------------------------------------------------------
    // Calendar colours
    // -----------------------------------------------------------------------

    /** Amber ring drawn around today's calendar cell. */
    public static final Color TODAY_RING    = new Color(0xD4, 0x96, 0x50);

    /** Fill colour for the selected day cell in the calendar. */
    public static final Color DAY_SELECTED  = new Color(0xE8, 0xA8, 0x65);

    /** Hover highlight used when the mouse moves over a calendar day cell. */
    public static final Color DAY_HOVER     = new Color(0xFF, 0xEA, 0xD0);

    // -----------------------------------------------------------------------
    // Status colours
    // -----------------------------------------------------------------------

    /** Neutral grey badge colour for tasks in the {@link Task.Status#TODO} state. */
    public static final Color STATUS_TODO        = new Color(0x90, 0x90, 0x90);

    /** Blue badge colour for tasks in the {@link Task.Status#IN_PROGRESS} state. */
    public static final Color STATUS_IN_PROGRESS = new Color(0x42, 0x8B, 0xCA);

    /** Green badge colour for tasks in the {@link Task.Status#DONE} state. */
    public static final Color STATUS_DONE        = new Color(0x5C, 0xB8, 0x5C);

    /**
     * Returns the badge colour that corresponds to the given task status.
     * Falls back to {@link #STATUS_TODO} if {@code status} is {@code null}.
     *
     * @param status the {@link Task.Status} to resolve
     * @return the corresponding badge {@link Color}
     */
    public static Color forStatus(Task.Status status) {
        if (status == null) return STATUS_TODO;
        switch (status) {
            case TODO:        return STATUS_TODO;
            case IN_PROGRESS: return STATUS_IN_PROGRESS;
            case DONE:        return STATUS_DONE;
            default:          return STATUS_TODO;
        }
    }

    // -----------------------------------------------------------------------
    // Priority colours
    // -----------------------------------------------------------------------

    /** Soft green accent used for {@link Task.Priority#LOW} tasks. */
    public static final Color PRIORITY_LOW    = new Color(0x8B, 0xC3, 0x4A);

    /** Amber accent used for {@link Task.Priority#MEDIUM} tasks. */
    public static final Color PRIORITY_MEDIUM = new Color(0xFF, 0x98, 0x00);

    /** Red accent used for {@link Task.Priority#HIGH} tasks. */
    public static final Color PRIORITY_HIGH   = new Color(0xF4, 0x43, 0x36);

    /**
     * Returns the accent colour that corresponds to the given task priority.
     * Falls back to {@link #PRIORITY_MEDIUM} if {@code priority} is {@code null}.
     *
     * @param priority the {@link Task.Priority} to resolve
     * @return the corresponding accent {@link Color}
     */
    public static Color forPriority(Task.Priority priority) {
        if (priority == null) return PRIORITY_MEDIUM;
        switch (priority) {
            case LOW:    return PRIORITY_LOW;
            case MEDIUM: return PRIORITY_MEDIUM;
            case HIGH:   return PRIORITY_HIGH;
            default:     return PRIORITY_MEDIUM;
        }
    }

    // -----------------------------------------------------------------------
    // Countdown badge colours
    // -----------------------------------------------------------------------

    /** Red used when a task is past its due date. */
    public static final Color COUNTDOWN_OVERDUE = new Color(0xF4, 0x43, 0x36);

    /** Orange used when a task is due today. */
    public static final Color COUNTDOWN_TODAY   = new Color(0xFF, 0x98, 0x00);

    /** Yellow-amber used when a task is due within the next three days. */
    public static final Color COUNTDOWN_SOON    = new Color(0xFF, 0xC1, 0x07);

    /** Green used when there is plenty of time remaining before the due date. */
    public static final Color COUNTDOWN_NORMAL  = new Color(0x5C, 0xB8, 0x5C);

    /** Green used on the countdown badge when the task is already done. */
    public static final Color COUNTDOWN_DONE    = new Color(0x5C, 0xB8, 0x5C);

    /**
     * Returns the appropriate countdown-badge colour for the given task based on
     * its current status and the number of days remaining until its due date.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>If the task status is {@link Task.Status#DONE} → {@link #COUNTDOWN_DONE}</li>
     *   <li>If {@link Task#daysUntilDue()} {@literal <} 0 → {@link #COUNTDOWN_OVERDUE}</li>
     *   <li>If {@link Task#daysUntilDue()} == 0 → {@link #COUNTDOWN_TODAY}</li>
     *   <li>If {@link Task#daysUntilDue()} ≤ 3 → {@link #COUNTDOWN_SOON}</li>
     *   <li>Otherwise → {@link #COUNTDOWN_NORMAL}</li>
     * </ol>
     *
     * @param task the task whose countdown colour should be resolved
     * @return the resolved countdown {@link Color}
     */
    public static Color forCountdown(Task task) {
        if (task.getStatus() == Task.Status.DONE) return COUNTDOWN_DONE;
        long days = task.daysUntilDue();
        if (days < 0)  return COUNTDOWN_OVERDUE;
        if (days == 0) return COUNTDOWN_TODAY;
        if (days <= 3) return COUNTDOWN_SOON;
        return COUNTDOWN_NORMAL;
    }
}