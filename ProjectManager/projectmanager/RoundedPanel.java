package projectmanager;

import javax.swing.*;
import java.awt.*;

/**
 * A transparent-background {@link JPanel} that paints itself with a rounded
 * rectangle fill and an optional border.
 *
 * <p>Because {@code opaque} is set to {@code false}, parent containers see
 * through the corners of the rounded rectangle correctly.  All children are
 * composited on top of the painted rounded background.</p>
 *
 * <p>Two constructors are provided:</p>
 * <ul>
 *   <li>{@link #RoundedPanel(Color, int)} – fill colour and arc radius only
 *       (no border)</li>
 *   <li>{@link #RoundedPanel(Color, int, Color, int)} – fill, arc, border colour,
 *       and border width</li>
 * </ul>
 *
 * <p>When a border is configured the background colour is drawn at
 * {@code (borderWidth, borderWidth)} with reduced dimensions so that the border
 * colour forms an inset ring around the inner fill.</p>
 */
public class RoundedPanel extends JPanel {

    /** Fill colour painted inside the rounded rectangle. */
    private final Color bgColor;

    /** Corner arc radius (in pixels) for both x and y. */
    private final int arc;

    /**
     * Colour of the simulated border ring, or {@code null} if no border should
     * be painted.
     */
    private final Color borderColor;

    /**
     * Width of the simulated border ring in pixels.  Ignored when
     * {@link #borderColor} is {@code null}.
     */
    private final int borderWidth;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates a {@code RoundedPanel} with a rounded fill and no border.
     *
     * @param bgColor fill colour of the panel
     * @param arc     corner arc radius in pixels
     */
    public RoundedPanel(Color bgColor, int arc) {
        this(bgColor, arc, null, 0);
    }

    /**
     * Creates a {@code RoundedPanel} with a rounded fill and an inset border ring.
     *
     * @param bgColor     fill colour of the panel interior
     * @param arc         corner arc radius in pixels
     * @param borderColor colour of the surrounding border ring; pass {@code null}
     *                    to suppress the border
     * @param borderWidth width of the border ring in pixels; ignored when
     *                    {@code borderColor} is {@code null}
     */
    public RoundedPanel(Color bgColor, int arc, Color borderColor, int borderWidth) {
        this.bgColor     = bgColor;
        this.arc         = arc;
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;
        setOpaque(false);
    }

    // -----------------------------------------------------------------------
    // Painting
    // -----------------------------------------------------------------------

    /**
     * Paints the rounded background (and optional border ring) before delegating
     * to {@code super.paintComponent} to render child components.
     *
     * <p>When a border is configured, the border colour is painted as a larger
     * rounded rectangle and the fill colour is painted on top, inset by
     * {@link #borderWidth} on all sides.  The arc of the inner fill is reduced by
     * {@code borderWidth} to maintain visual consistency.</p>
     *
     * @param g the graphics context provided by Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (borderColor != null && borderWidth > 0) {
            g2.setColor(borderColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.setColor(bgColor);
            g2.fillRoundRect(borderWidth, borderWidth,
                    getWidth()  - 2 * borderWidth,
                    getHeight() - 2 * borderWidth,
                    arc - borderWidth, arc - borderWidth);
        } else {
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}