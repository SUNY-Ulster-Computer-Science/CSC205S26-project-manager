package projectmanager;

import java.awt.*;

/**
 * A {@link FlowLayout} variant that wraps components onto subsequent rows
 * when the container is too narrow, and reports the correct preferred
 * height so parent layouts allocate enough vertical space.
 */
public class WrapLayout extends FlowLayout {

    /**
     * @param align horizontal alignment ({@link FlowLayout#LEFT}, etc.)
     * @param hgap  horizontal gap between components in pixels
     * @param vgap  vertical gap between rows in pixels
     */
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return computeSize(target);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return computeSize(target);
    }

    /**
     * Simulates a left-to-right flow, advancing to the next row whenever
     * a component would exceed the container's current width, and returns
     * the total size required.
     */
    private Dimension computeSize(Container target) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getWidth();
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

            Insets insets = target.getInsets();
            int maxWidth = targetWidth - insets.left - insets.right;
            int x = 0, y = 0, rowHeight = 0;

            for (int i = 0; i < target.getComponentCount(); i++) {
                Component c = target.getComponent(i);
                if (c.isVisible()) {
                    Dimension d = c.getPreferredSize();
                    if (x > 0 && x + d.width > maxWidth) {
                        y += rowHeight + getVgap();
                        x = 0;
                        rowHeight = 0;
                    }
                    x += d.width + getHgap();
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }
            y += rowHeight;
            return new Dimension(targetWidth,
                    y + insets.top + insets.bottom + getVgap() * 2);
        }
    }
}