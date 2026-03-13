package projectmanager;

import javax.swing.*;
import java.awt.*;

/**
 * A multi-line text area with a white rounded-rectangle background,
 * word-wrapping enabled, consistent with the Orange™ theme.
 */
public class RoundedTextArea extends JTextArea {

    /**
     * @param rows preferred row count
     * @param cols preferred column count
     */
    public RoundedTextArea(int rows, int cols) {
        super(rows, cols);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        setFont(new Font("SansSerif", Font.PLAIN, 14));
        setLineWrap(true);
        setWrapStyleWord(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        g2.dispose();
        super.paintComponent(g);
    }
}