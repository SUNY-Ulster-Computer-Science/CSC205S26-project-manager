package projectmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A theme-consistent rounded button that plays a Material-style ripple
 * animation on click.
 *
 * <p>When the user presses the button a translucent white circle expands
 * outward from the click point and fades to transparent over roughly
 * 240&nbsp;ms (20 frames at 12&nbsp;ms each).</p>
 */
public class OrangeButton extends JButton {

    private final Color normalBg;
    private final Color hoverBg;
    private boolean hovering = false;

    // Ripple animation state
    private int   rippleX, rippleY;
    private float rippleRadius    = 0;
    private float rippleMaxRadius = 0;
    private float rippleAlpha     = 0;
    private Timer rippleTimer;

    /**
     * @param text     button label
     * @param normalBg idle background color
     * @param hoverBg  background color while the cursor is over the button
     */
    public OrangeButton(String text, Color normalBg, Color hoverBg) {
        super(text);
        this.normalBg = normalBg;
        this.hoverBg = hoverBg;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(new Font("SansSerif", Font.BOLD, 13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e)  { hovering = true;  repaint(); }
            @Override public void mouseExited(MouseEvent e)   { hovering = false; repaint(); }
            @Override public void mousePressed(MouseEvent e)  { startRipple(e.getX(), e.getY()); }
        });
    }

    /**
     * Kicks off the ripple animation originating at ({@code x}, {@code y}).
     * Any in-progress ripple is cancelled first.
     */
    private void startRipple(int x, int y) {
        rippleX = x;
        rippleY = y;
        rippleRadius = 0;
        rippleAlpha = 0.45f;

        double d1 = Math.hypot(x, y);
        double d2 = Math.hypot(getWidth() - x, y);
        double d3 = Math.hypot(x, getHeight() - y);
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