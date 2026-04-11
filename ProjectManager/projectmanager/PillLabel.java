package projectmanager;

import javax.swing.*;
import java.awt.*;

import static projectmanager.ThemeColors.*;

/**
 * A non-interactive label that renders its text inside a pill-shaped (fully
 * rounded) {@link ThemeColors#LABEL_BG caramel} background.
 *
 * <p>{@code PillLabel} is used in the application wherever a compact,
 * badge-style visual indicator is needed (e.g. date display, section
 * headings).  It sets {@code opaque} to {@code false} so the rounded
 * corners are transparent against any parent background.</p>
 *
 * <p>The preferred width is clamped to a minimum of 70 px so that very short
 * strings still produce a visually balanced pill.</p>
 */
public class PillLabel extends JLabel {

    /**
     * Constructs a {@code PillLabel} displaying the given text.
     * The label uses a 14 pt bold sans-serif font in {@link ThemeColors#TEXT_DARK}
     * and is centred horizontally with comfortable horizontal padding.
     *
     * @param text the text to display inside the pill
     */
    public PillLabel(String text) {
        super(text);
        setFont(new Font("SansSerif", Font.BOLD, 14));
        setForeground(TEXT_DARK);
        setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
    }

    /**
     * Paints a pill-shaped (fully rounded) rectangle in {@link ThemeColors#LABEL_BG}
     * behind the label text.
     *
     * @param g the graphics context provided by Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(LABEL_BG);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Returns the preferred size with a minimum width of 70 px so that short
     * labels still produce a visually proportional pill shape.
     *
     * @return preferred {@link Dimension} with {@code width ≥ 70}
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = Math.max(d.width, 70);
        return d;
    }
}