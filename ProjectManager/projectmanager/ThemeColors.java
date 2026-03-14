package projectmanager;

import java.awt.Color;

/**
 * Centralised color constants for the Orange™ visual theme.
 *
 * <p>Every custom UI component in the project references this class
 * instead of defining its own color literals, keeping the palette
 * consistent and easy to tweak in one place.</p>
 *
 * @author Team Orange
 * @version 1.0
 */
public final class ThemeColors {

    private ThemeColors() { /* non-instantiable */ }

    /** Outer frame background and button idle color. */
    public static final Color OUTER_ORANGE  = new Color(0xE8, 0xA8, 0x65);

    /** Inner panel fill. */
    public static final Color INNER_PEACH   = new Color(0xFD, 0xDC, 0xB5);

    /** Default button background (same hue as the outer frame). */
    public static final Color BUTTON_ORANGE = new Color(0xE8, 0xA8, 0x65);

    /** Button background on mouse-over. */
    public static final Color BUTTON_HOVER  = new Color(0xD4, 0x96, 0x50);

    /** Task list and scroll-pane background. */
    public static final Color CONTENT_CREAM = new Color(0xFF, 0xF5, 0xEB);

    /** Pill-label and list-selection background. */
    public static final Color LABEL_BG      = new Color(0xD4, 0xA7, 0x6A);

    /** Primary text color used on labels and list items. */
    public static final Color TEXT_DARK     = new Color(0x3D, 0x2B, 0x10);
}