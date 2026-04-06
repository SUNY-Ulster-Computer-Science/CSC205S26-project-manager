package projectmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Base JButton that plays a Material-style expanding-circle ripple animation
 * starting from the exact press point each time the user clicks.
 *
 * <h3>How to subclass</h3>
 * <ol>
 *   <li>Override {@link #paintBackground(Graphics2D)} to draw the button's fill
 *       (rounded rect, arrow, etc.).  The ripple is composited on top of
 *       whatever you paint here.</li>
 *   <li>Override {@link #rippleArc()} if the button shape is not fully
 *       rounded (default: {@code min(w,h)}).</li>
 *   <li>Override {@link #rippleColor()} for buttons with a light background
 *       where white would be invisible (default: {@link Color#WHITE}).</li>
 * </ol>
 *
 * <p>The ripple is guaranteed to stay clipped inside the button shape, and the
 * animation runs at ~60 fps for 240 ms then stops, so it imposes zero cost at
 * rest.</p>
 */
public class RippleButton extends JButton {

    // ── Ripple state ──────────────────────────────────────────────────────

    private int   rx, ry;
    private float rRadius    = 0;
    private float rMaxRadius = 0;
    private float rAlpha     = 0;
    private Timer rTimer;

    // ── Constructor ───────────────────────────────────────────────────────

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

    // ── Hooks ─────────────────────────────────────────────────────────────

    /**
     * Draws the button's background fill.  Called before the ripple overlay.
     * Default: no-op (fully transparent background).
     */
    protected void paintBackground(Graphics2D g2) {}

    /**
     * Corner arc used to clip the ripple circle to the button shape.
     * Default: {@code min(width, height)} — fully rounded pill.
     */
    protected int rippleArc() {
        return Math.min(getWidth(), getHeight());
    }

    /**
     * Colour of the expanding ripple circle.
     * Override for buttons with light backgrounds where white would be invisible.
     * Default: {@link Color#WHITE}.
     */
    protected Color rippleColor() {
        return Color.WHITE;
    }

    // ── Ripple engine ─────────────────────────────────────────────────────

    private void startRipple(int x, int y) {
        rx = x;
        ry = y;
        rRadius = 0;
        rAlpha  = 0.42f;

        // Max radius = distance to the farthest corner, so the circle always
        // fills the entire button before it disappears.
        double d1 = Math.hypot(x,              y);
        double d2 = Math.hypot(getWidth() - x, y);
        double d3 = Math.hypot(x,              getHeight() - y);
        double d4 = Math.hypot(getWidth() - x, getHeight() - y);
        rMaxRadius = (float) Math.max(Math.max(d1, d2), Math.max(d3, d4));

        if (rTimer != null && rTimer.isRunning()) rTimer.stop();
        rTimer = new Timer(12, e -> {          // ~83 fps target
            rRadius += rMaxRadius / 20f;       // expands over 20 frames ≈ 240 ms
            rAlpha  -= 0.42f       / 20f;
            if (rAlpha <= 0 || rRadius >= rMaxRadius) {
                rAlpha = 0; rRadius = 0;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        rTimer.start();
    }

    // ── Painting ──────────────────────────────────────────────────────────

    /**
     * Paints: background → ripple overlay → text/icon (via super).
     * The ripple is clipped to the rounded shape returned by {@link #rippleArc()}.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Background (subclass paints whatever fill it needs)
        paintBackground(g2);

        // 2. Ripple overlay — clipped inside the button shape
        if (rAlpha > 0 && rRadius > 0) {
            int arc = rippleArc();
            g2.setClip(new RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(), arc, arc));
            Color rc = rippleColor();
            int alpha = Math.max(0, Math.min(255, (int)(rAlpha * 255)));
            g2.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), alpha));
            int r = (int) rRadius;
            g2.fillOval(rx - r, ry - r, r * 2, r * 2);
        }

        g2.dispose();

        // 3. Text / icon (Swing default rendering, paints on top of our work)
        super.paintComponent(g);
    }
}
