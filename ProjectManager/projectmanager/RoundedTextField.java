package projectmanager;

import javax.swing.*;
import java.awt.*;

/**
 * A single-line text field with a white rounded-rectangle background,
 * consistent with the Orange™ theme.
 */
public class RoundedTextField extends JTextField {

    /** @param columns initial column count passed to {@link JTextField} */
    public RoundedTextField(int columns) {
        super(columns);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        setFont(new Font("SansSerif", Font.PLAIN, 14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
        g2.dispose();
        super.paintComponent(g);
    }
}