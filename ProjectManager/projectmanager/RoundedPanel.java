package projectmanager;

import javax.swing.*;
import java.awt.*;

/**
 * A panel whose background is a filled rounded rectangle, optionally
 * surrounded by a colored border of configurable thickness.
 */
public class RoundedPanel extends JPanel {

    private final Color bgColor;
    private final int arc;
    private final Color borderColor;
    private final int borderWidth;

    /**
     * Creates a borderless rounded panel.
     *
     * @param bgColor fill color
     * @param arc     corner arc diameter in pixels
     */
    public RoundedPanel(Color bgColor, int arc) {
        this(bgColor, arc, null, 0);
    }

    /**
     * Creates a rounded panel with an optional border.
     *
     * @param bgColor     fill color
     * @param arc         corner arc diameter in pixels
     * @param borderColor border color, or {@code null} for no border
     * @param borderWidth border thickness in pixels (ignored when {@code borderColor} is {@code null})
     */
    public RoundedPanel(Color bgColor, int arc, Color borderColor, int borderWidth) {
        this.bgColor = bgColor;
        this.arc = arc;
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (borderColor != null && borderWidth > 0) {
            g2.setColor(borderColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.setColor(bgColor);
            g2.fillRoundRect(borderWidth, borderWidth,
                    getWidth() - 2 * borderWidth, getHeight() - 2 * borderWidth,
                    arc - borderWidth, arc - borderWidth);
        } else {
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}