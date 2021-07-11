package cuchaz.enigma.gui.elements.rpanel;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.ui.FlatTitlePane;

public class RootPaneUtil {

	private RootPaneUtil() {
	}

	public static boolean registerDragListener(StandaloneRootPane self, WindowDragListener listener) {
		return registerDragListener(self, self, listener, false);
	}

	public static boolean registerDragListener(JFrame self, WindowDragListener listener) {
		return registerDragListener(self, self.getRootPane(), listener, true);
	}

	public static boolean registerDragListener(Container outer, JRootPane rootPane, WindowDragListener listener, boolean useComponentListener) {
		boolean[] isMovingWindow = new boolean[]{false};

		// We can't "just detect" whether the window is being dragged because
		// everything is hidden away in internal look and feel classes, so
		// we have to guess which is the title pane and register a mouse
		// listener on it to detect dragging

		// This is the layered pane that (hopefully) contains the content pane
		// and the title pane.
		JLayeredPane layeredPane = rootPane.getLayeredPane();

		// ...so we just get all the components in this layered pane, take away
		// the content pane and we get what's hopefully the content pane
		List<Component> potentialTitlePanes = Arrays.stream(layeredPane.getComponents())
				.filter(c -> c != rootPane.getContentPane())
				.collect(Collectors.toList());

		if (potentialTitlePanes.size() == 0) {
			System.err.println("Couldn't detect a title pane for registering RPanel window drag detection!");
			return false;
		} else {
			Component titlePane = null;

			for (Component potentialTitlePane : potentialTitlePanes) {
				if (potentialTitlePane instanceof FlatTitlePane) {
					// thanks, FlatLaf!
					titlePane = potentialTitlePane;
					break;
				}
			}

			if (titlePane == null) {
				if (potentialTitlePanes.size() > 1) {
					System.err.println("Multiple potential title pane components detected, taking the first one. Window drag detection might not work.");
				}

				titlePane = potentialTitlePanes.get(0);
			}

			final Component titlePane1 = titlePane;

			// This code is mostly copied from
			// MetalRootPaneUI.MouseInputHandler, except that the border
			// thickness is set to 1 instead of 5 to accommodate for other look
			// and feels handling dragging differently.
			MouseAdapter l = new MouseAdapter() {

				/**
				 * Region from edges that dragging is active from.
				 */
				private static final int BORDER_DRAG_THICKNESS = 1;

				@Override
				public void mousePressed(MouseEvent e) {
					if (rootPane.getWindowDecorationStyle() == JRootPane.NONE) {
						return;
					}

					Point dragWindowOffset = e.getPoint();

					Component w = (Component) e.getSource();

					Point convertedDragWindowOffset = SwingUtilities.convertPoint(
							w, dragWindowOffset, titlePane1);

					Frame f = null;
					Dialog d = null;

					if (w instanceof Frame) {
						f = (Frame) w;
					} else if (w instanceof Dialog) {
						d = (Dialog) w;
					}

					int frameState = (f != null) ? f.getExtendedState() : 0;

					if (titlePane1 != null && titlePane1.contains(convertedDragWindowOffset)) {
						if ((f != null && ((frameState & Frame.MAXIMIZED_BOTH) == 0) || (d != null)) &&
								dragWindowOffset.y >= BORDER_DRAG_THICKNESS &&
								dragWindowOffset.x >= BORDER_DRAG_THICKNESS &&
								dragWindowOffset.x < w.getWidth() - BORDER_DRAG_THICKNESS) {
							isMovingWindow[0] = true;
							listener.onStartDrag(null);
							// dragOffsetX = dragWindowOffset.x;
							// dragOffsetY = dragWindowOffset.y;
						}
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (isMovingWindow[0]) {
						listener.onStopDrag(null);
						isMovingWindow[0] = false;
					}
				}

				@Override
				public void mouseDragged(MouseEvent e) {
					if (!useComponentListener) {
						listener.onDragMove(null);
					}
				}
			};

			outer.addMouseListener(l);

			if (!useComponentListener) {
				outer.addMouseMotionListener(l);
			} else {
				outer.addComponentListener(new ComponentAdapter() {
					@Override
					public void componentMoved(ComponentEvent e) {
						if (isMovingWindow[0]) {
							listener.onDragMove(null);
						}
					}
				});
			}

			return true;
		}

	}


}
