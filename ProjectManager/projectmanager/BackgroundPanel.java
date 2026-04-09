package projectmanager;

import javax.swing.*;
import java.awt.*;

/**
 * A {@link JPanel} that tiles or stretches a background image to fill its
 * entire bounds, then composites child components on top.
 *
 * <p>The panel sets {@code opaque} to {@code false} so that standard Swing
 * transparency semantics are preserved.  The image is scaled to
 * {@code (getWidth(), getHeight())} on every paint call, which means the image
 * always fills the panel regardless of how the window is resized.</p>
 *
 * <p>If {@code bgImage} is {@code null} the component behaves exactly like a
 * plain transparent {@link JPanel}.</p>
 */
public class BackgroundPanel extends JPanel {

    /**
     * The image drawn as the panel background, or {@code null} if no image has
     * been supplied.
     */
    private final Image bgImage;

    /**
     * Constructs a {@code BackgroundPanel} that will paint the given image as its
     * background.
     *
     * @param bgImage the background image to draw, or {@code null} for no image
     */
    public BackgroundPanel(Image bgImage) {
        this.bgImage = bgImage;
        setOpaque(false);
    }

    /**
     * Paints the background image scaled to the current component size, then
     * delegates to {@code super.paintComponent} to render child components.
     *
     * @param g the graphics context provided by Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }
        super.paintComponent(g);
    }
}