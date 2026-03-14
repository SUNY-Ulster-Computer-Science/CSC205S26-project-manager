package projectmanager;

import javax.swing.*;
import java.awt.*;

import static projectmanager.ThemeColors.*;

/**
 * A compact label drawn as a fully-rounded "pill" filled with the theme's
 * {@link ThemeColors#LABEL_BG} color. Used to label form fields in dialogs
 * (e.g.&nbsp;"Name:", "Date:", "Tag:").
 */
public class PillLabel extends JLabel {

    /** @param text the label caption */
    public PillLabel(String text) {
        super(text);
        setFont(new Font("SansSerif", Font.BOLD, 14));
        setForeground(TEXT_DARK);
        setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(LABEL_BG);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = Math.max(d.width, 70);
        return d;
    }
}