package projectmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A custom {@link JButton} that plays a material-style ripple animation on
 * mouse press.
 *
 * <p>Subclasses are expected to override {@link #paintBackground(Graphics2D)}
 * to draw the button's own background shape and colour before the ripple overlay
 * is applied.  Two additional hook methods – {@link #rippleArc()} and
 * {@link #rippleColor()} – control the clip radius and colour of the ripple
 * circle respectively.</p>
 *
 * <h3>Usage pattern</h3>
 * <pre>
 * RippleButton btn = new RippleButton("Click me") {
 *     {@literal @}Override
 *     protected void paintBackground(Graphics2D g2) {
 *         g2.setColor(Color.ORANGE);
 *         g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
 *     }
 * };
 * </pre>
 *
 * <p>The button sets {@code contentAreaFilled}, {@code borderPainted}, and
 * {@code opaque} to {@code false} so that the subclass retains full control
 * of its visual appearance.</p>
 */
public class RippleButton extends JButton {

    /** X-coordinate (within the button) at which the most recent ripple started. */
    private int rx;

    /** Y-coordinate (within the button) at which the most recent ripple started. */
    private int ry;

    /** Current radius of the expanding ripple circle (in pixels). */
    private float rRadius    = 0;

    /** Maximum radius the ripple must reach before the animation stops. */
    private float rMaxRadius = 0;

    /** Current alpha (0.0–1.0) of the ripple overlay. */
    private float rAlpha     = 0;

    /** Swing timer that drives the ripple expand / fade animation. */
    private Timer rTimer;

    /**
     * Constructs a {@code RippleButton} with the given label text.
     *
     * @param text the button label
     */
    public RippleButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startRipple(e.getX(), e.getY());
            }
        });
    }

    // -----------------------------------------------------------------------
    // Overrideable hooks
    // -----------------------------------------------------------------------

    /**
     * Called during {@link #paintComponent(Graphics)} <em>before</em> the ripple
     * overlay is drawn.  Subclasses should fill the button background here.
     * The default implementation does nothing (transparent background).
     *
     * @param g2 the graphics context, already configured with anti-aliasing
     */
    protected void paintBackground(Graphics2D g2) {}

    /**
     * Returns the corner arc radius used when clipping the ripple circle to the
     * button bounds.  The default is {@code min(width, height)}, producing a
     * pill/circle clip.  Subclasses may override to match their background shape.
     *
     * @return arc radius in pixels
     */
    protected int rippleArc() {
        return Math.min(getWidth(), getHeight());
    }

    /**
     * Returns the base colour of the ripple overlay.  The actual colour used
     * during painting is this colour at the current {@link #rAlpha} transparency.
     * Defaults to {@link Color#WHITE}.
     *
     * @return ripple overlay colour (alpha is ignored; transparency is applied
     *         dynamically)
     */
    protected Color rippleColor() {
        return Color.WHITE;
    }

    // -----------------------------------------------------------------------
    // Animation
    // -----------------------------------------------------------------------

    /**
     * Starts a new ripple animation originating at the given point.  If an
     * animation is already running it is stopped and restarted from the new
     * origin.
     *
     * <p>The maximum radius is computed as the distance from the origin to the
     * farthest corner of the button, ensuring the ripple always covers the entire
     * surface.</p>
     *
     * @param x x-coordinate of the mouse press within the button
     * @param y y-coordinate of the mouse press within the button
     */
    private void startRipple(int x, int y) {
        rx = x;
        ry = y;
        rRadius = 0;
        rAlpha  = 0.42f;

        double d1 = Math.hypot(x,              y);
        double d2 = Math.hypot(getWidth() - x, y);
        double d3 = Math.hypot(x,              getHeight() - y);
        double d4 = Math.hypot(getWidth() - x, getHeight() - y);
        rMaxRadius = (float) Math.max(Math.max(d1, d2), Math.max(d3, d4));

        if (rTimer != null && rTimer.isRunning()) rTimer.stop();
        rTimer = new Timer(12, e -> {
            rRadius += rMaxRadius / 20f;
            rAlpha  -= 0.42f / 20f;
            if (rAlpha <= 0 || rRadius >= rMaxRadius) {
                rAlpha = 0; rRadius = 0;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        rTimer.start();
    }

    // -----------------------------------------------------------------------
    // Painting
    // -----------------------------------------------------------------------

    /**
     * Paints the button in three layers:
     * <ol>
     *   <li>{@link #paintBackground(Graphics2D)} – subclass-defined background</li>
     *   <li>Ripple overlay circle (if the animation is active)</li>
     *   <li>{@code super.paintComponent(g)} – label text and icon</li>
     * </ol>
     *
     * @param g the graphics context provided by Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        paintBackground(g2);

        if (rAlpha > 0 && rRadius > 0) {
            int arc = rippleArc();
            g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
            Color rc    = rippleColor();
            int   alpha = Math.max(0, Math.min(255, (int)(rAlpha * 255)));
            g2.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), alpha));
            int r = (int) rRadius;
            g2.fillOval(rx - r, ry - r, r * 2, r * 2);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}