package projectmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A fully custom {@link JButton} with a rounded rectangle background, hover
 * colour feedback, and a material-style ripple animation on mouse press.
 *
 * <p>Unlike {@link RippleButton}, which delegates background painting to
 * subclasses, {@code OrangeButton} is self-contained: callers supply the normal
 * and hover colours at construction time.  It is used for primary form actions
 * (e.g. "Create Task", "Save Changes", "Cancel").</p>
 *
 * <h3>Visual behaviour</h3>
 * <ul>
 *   <li>Background switches from {@code normalBg} to {@code hoverBg} when the
 *       mouse enters the button.</li>
 *   <li>A semi-transparent white ripple circle expands from the click point and
 *       fades out over ~240 ms.</li>
 * </ul>
 */
public class OrangeButton extends JButton {

    /** Background colour shown when the button is in its default state. */
    private final Color normalBg;

    /** Background colour shown when the mouse is hovering over the button. */
    private final Color hoverBg;

    /** {@code true} while the mouse cursor is within the button bounds. */
    private boolean hovering = false;

    // -----------------------------------------------------------------------
    // Ripple state
    // -----------------------------------------------------------------------

    /** X-coordinate (within the button) of the most recent mouse press. */
    private int   rippleX;

    /** Y-coordinate (within the button) of the most recent mouse press. */
    private int   rippleY;

    /** Current radius of the expanding ripple circle. */
    private float rippleRadius    = 0;

    /** Maximum radius the ripple must reach (distance to the farthest corner). */
    private float rippleMaxRadius = 0;

    /** Current alpha (0.0–1.0) of the ripple overlay. */
    private float rippleAlpha     = 0;

    /** Swing timer that drives the ripple expand / fade animation. */
    private Timer rippleTimer;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs an {@code OrangeButton} with the given label and colour scheme.
     *
     * @param text     button label
     * @param normalBg background colour in the default (non-hovered) state
     * @param hoverBg  background colour while the mouse cursor is over the button
     */
    public OrangeButton(String text, Color normalBg, Color hoverBg) {
        super(text);
        this.normalBg = normalBg;
        this.hoverBg  = hoverBg;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(new Font("SansSerif", Font.BOLD, 13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovering = true;  repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovering = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { startRipple(e.getX(), e.getY()); }
        });
    }

    // -----------------------------------------------------------------------
    // Ripple animation
    // -----------------------------------------------------------------------

    /**
     * Initialises and starts a new ripple animation from the given point.  If a
     * previous animation is still running it is stopped before the new one starts.
     *
     * <p>The maximum radius is the distance from the press point to the farthest
     * corner so the ripple fills the entire button surface before fading out.</p>
     *
     * @param x x-coordinate of the mouse press within the button
     * @param y y-coordinate of the mouse press within the button
     */
    private void startRipple(int x, int y) {
        rippleX = x;
        rippleY = y;
        rippleRadius = 0;
        rippleAlpha  = 0.45f;

        double d1 = Math.hypot(x,              y);
        double d2 = Math.hypot(getWidth() - x, y);
        double d3 = Math.hypot(x,              getHeight() - y);
        double d4 = Math.hypot(getWidth() - x, getHeight() - y);
        rippleMaxRadius = (float) Math.max(Math.max(d1, d2), Math.max(d3, d4));

        if (rippleTimer != null && rippleTimer.isRunning()) {
            rippleTimer.stop();
        }

        rippleTimer = new Timer(12, evt -> {
            float speed = rippleMaxRadius / 20f;
            rippleRadius += speed;
            rippleAlpha  -= 0.45f / 20f;

            if (rippleAlpha <= 0 || rippleRadius >= rippleMaxRadius) {
                rippleAlpha  = 0;
                rippleRadius = 0;
                ((Timer) evt.getSource()).stop();
            }
            repaint();
        });
        rippleTimer.start();
    }

    // -----------------------------------------------------------------------
    // Painting
    // -----------------------------------------------------------------------

    /**
     * Paints the button background and optional ripple overlay, then delegates
     * to {@code super.paintComponent} to render the label text.
     *
     * <p>Painting order:</p>
     * <ol>
     *   <li>Rounded rectangle filled with {@link #hoverBg} or {@link #normalBg}
     *       depending on hover state.</li>
     *   <li>Ripple circle (semi-transparent white) clipped to the rounded
     *       background shape (if the animation is active).</li>
     *   <li>Superclass paint for text and icon.</li>
     * </ol>
     *
     * @param g the graphics context provided by Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(hovering ? hoverBg : normalBg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);

        if (rippleAlpha > 0 && rippleRadius > 0) {
            Shape clip = new java.awt.geom.RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(), 24, 24);
            g2.setClip(clip);
            int alpha = Math.max(0, Math.min(255, (int) (rippleAlpha * 255)));
            g2.setColor(new Color(255, 255, 255, alpha));
            int r = (int) rippleRadius;
            g2.fillOval(rippleX - r, rippleY - r, r * 2, r * 2);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}