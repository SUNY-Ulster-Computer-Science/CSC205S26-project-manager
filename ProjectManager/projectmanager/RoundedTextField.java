package projectmanager;

import javax.swing.*;
import java.awt.*;

/**
 * A single-line text field that paints its own white, pill-shaped background
 * instead of relying on the look-and-feel.
 *
 * <p>The component sets {@code opaque} to {@code false} so that parent panels
 * see through the corners of the rounded rectangle correctly.  An empty border
 * provides comfortable internal padding without affecting the custom background.</p>
 *
 * <p>Typical usage:</p>
 * <pre>
 * RoundedTextField field = new RoundedTextField(20);
 * field.putClientProperty("JTextField.placeholderText", "Search tasks…");
 * </pre>
 */
public class RoundedTextField extends JTextField {

    /**
     * Constructs a {@code RoundedTextField} with the specified column width.
     * The field uses a 14 pt sans-serif plain font and 8 px vertical / 14 px
     * horizontal internal padding.
     *
     * @param columns preferred width hint in character columns (passed directly
     *                to {@link JTextField#JTextField(int)})
     */
    public RoundedTextField(int columns) {
        super(columns);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        setFont(new Font("SansSerif", Font.PLAIN, 14));
    }

    /**
     * Paints a white pill-shaped (fully rounded) background before delegating to
     * the superclass to render the text cursor and content.
     *
     * @param g the graphics context provided by Swing
     */
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