package projectmanager;

import javax.swing.*;
import java.awt.*;

/**
 * A panel that stretches a background image to fill its entire bounds
 * before painting child components on top.
 */
public class BackgroundPanel extends JPanel {

    private final Image bgImage;

    /**
     * @param bgImage the image to draw; if {@code null} no background is painted
     */
    public BackgroundPanel(Image bgImage) {
        this.bgImage = bgImage;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }
        super.paintComponent(g);
    }
}