package projectmanager;

import javax.swing.*;
import java.awt.*;

/**
 * A multi-line text area that paints a plain white background, overriding the
 * look-and-feel default so it integrates cleanly with the application's custom
 * rounded containers.
 *
 * <p>The component sets {@code opaque} to {@code false} so that clipping applied
 * by a parent {@link RoundedPanel} is respected at the corners.  Word-wrapping is
 * enabled by default.</p>
 *
 * <p>Typical usage inside a scrollable wrapper:</p>
 * <pre>
 * RoundedTextArea notes = new RoundedTextArea(8, 20);
 * JScrollPane scroll    = new JScrollPane(notes);
 * </pre>
 */
public class RoundedTextArea extends JTextArea {

    /**
     * Constructs a {@code RoundedTextArea} with the given row and column hints.
     * The area uses a 14 pt sans-serif plain font, word-wrap, and 10 px vertical /
     * 14 px horizontal internal padding.
     *
     * @param rows number of visible text rows (passed to
     *             {@link JTextArea#JTextArea(int, int)})
     * @param cols preferred column width (passed to
     *             {@link JTextArea#JTextArea(int, int)})
     */
    public RoundedTextArea(int rows, int cols) {
        super(rows, cols);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        setFont(new Font("SansSerif", Font.PLAIN, 14));
        setLineWrap(true);
        setWrapStyleWord(true);
    }

    /**
     * Paints a plain white rectangle as the background before delegating to
     * the superclass to render the text content and caret.
     *
     * <p>A rectangle (rather than a rounded rectangle) is used here because
     * the text area is always placed inside a {@link RoundedPanel} that
     * provides the visible rounded corners.</p>
     *
     * @param g the graphics context provided by Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}