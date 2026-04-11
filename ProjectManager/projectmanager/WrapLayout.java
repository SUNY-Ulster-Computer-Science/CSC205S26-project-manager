package projectmanager;

import java.awt.*;

/**
 * A {@link FlowLayout} variant that wraps child components onto new rows when
 * they exceed the container's available width, and correctly reports the
 * resulting multi-row height as the container's preferred / minimum size.
 *
 * <p>Standard {@link FlowLayout} always reports a single-row preferred height
 * regardless of how narrow the container is, which causes scroll panes and
 * other layout managers to underallocate vertical space.  {@code WrapLayout}
 * fixes this by computing the actual wrapped height in
 * {@link #preferredLayoutSize} and {@link #minimumLayoutSize}.</p>
 *
 * <p>Used by {@link TaskCard} to lay out the tag pills so they reflow naturally
 * as the card is resized.</p>
 */
public class WrapLayout extends FlowLayout {

    /**
     * Constructs a {@code WrapLayout} with the given alignment and gap values.
     *
     * @param align horizontal alignment constant (e.g. {@link FlowLayout#LEFT})
     * @param hgap  horizontal gap between components in pixels
     * @param vgap  vertical gap between rows in pixels
     */
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    /**
     * Returns the preferred size of the container after wrapping all visible
     * children into rows that fit within the container's current width.
     *
     * @param target the container whose preferred size is being computed
     * @return preferred {@link Dimension} accounting for wrapped rows
     */
    @Override
    public Dimension preferredLayoutSize(Container target) {
        return computeSize(target);
    }

    /**
     * Returns the minimum size of the container (identical to the preferred size
     * for this layout).
     *
     * @param target the container whose minimum size is being computed
     * @return minimum {@link Dimension} accounting for wrapped rows
     */
    @Override
    public Dimension minimumLayoutSize(Container target) {
        return computeSize(target);
    }

    /**
     * Computes the height required to display all visible components wrapped into
     * rows of at most {@code targetWidth} pixels.
     *
     * <p>If the container has not yet been given a width (i.e. it is still 0), the
     * method walks up the ancestor chain to find the nearest ancestor with a
     * non-zero width.  If no ancestor width can be found it defaults to 400 px.</p>
     *
     * @param target the container to measure (must not be {@code null})
     * @return the {@link Dimension} needed to display all children without
     *         horizontal clipping
     */
    private Dimension computeSize(Container target) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getWidth();

            if (targetWidth == 0) {
                Container parent = target.getParent();
                while (parent != null && targetWidth == 0) {
                    targetWidth = parent.getWidth();
                    parent      = parent.getParent();
                }
            }
            if (targetWidth == 0) targetWidth = 400;

            Insets insets   = target.getInsets();
            int    maxWidth = targetWidth - insets.left - insets.right;
            int    x        = 0;
            int    y        = 0;
            int    rowHeight = 0;

            for (int i = 0; i < target.getComponentCount(); i++) {
                Component c = target.getComponent(i);
                if (c.isVisible()) {
                    Dimension d = c.getPreferredSize();
                    if (x > 0 && x + d.width > maxWidth) {
                        // Start a new row
                        y        += rowHeight + getVgap();
                        x        = 0;
                        rowHeight = 0;
                    }
                    x        += d.width + getHgap();
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }
            y += rowHeight;
            return new Dimension(targetWidth,
                    y + insets.top + insets.bottom + getVgap() * 2);
        }
    }
}