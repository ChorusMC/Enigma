package cuchaz.enigma.gui.elements.rpanel;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

public class RPanelWindow implements RPanelHost {

	private final JFrame ui;
	private RPanel panel;

	private boolean isMovingWindow;

	public RPanelWindow() {
		this.ui = new JFrame();

		// Unfortunately, having window decorations handled by the OS makes it
		// not receive position update events until the user lets go of the
		// window.
		this.ui.setUndecorated(true);
		this.ui.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);

		// We can't "just detect" whether the window is being dragged because
		// everything is hidden away in internal look and feel classes, so
		// we have to guess which is the title pane and register a mouse
		// listener on it to detect dragging

		// This is the layered pane that (hopefully) contains the content pane
		// and the title pane.
		JLayeredPane layeredPane = this.ui.getRootPane().getLayeredPane();

		// ...so we just get all the components in this layered pane, take away
		// the content pane and we get what's hopefully the content pane
		List<Component> potentialTitlePanes = Arrays.stream(layeredPane.getComponents())
				.filter(c -> c != this.ui.getContentPane())
				.collect(Collectors.toList());

		if (potentialTitlePanes.size() == 0) {
			System.err.println("Couldn't detect a title pane for registering RPanel window drag detection!");
		} else {
			if (potentialTitlePanes.size() > 1) {
				System.err.println("Multiple potential title pane components detected, taking the first one. Window drag detection might not work.");
			}

			Component titlePane = potentialTitlePanes.get(0);

			// This code is mostly copied from
			// MetalRootPaneUI.MouseInputHandler, except that the border
			// thickness is set to 1 instead of 5 to accommodate for other look
			// and feels handling dragging differently.
			this.ui.addMouseListener(new MouseAdapter() {

				/**
				 * Region from edges that dragging is active from.
				 */
				private static final int BORDER_DRAG_THICKNESS = 1;

				@Override
				public void mousePressed(MouseEvent e) {
					if (ui.getRootPane().getWindowDecorationStyle() == JRootPane.NONE) {
						return;
					}

					Point dragWindowOffset = e.getPoint();

					Window w = (Window) e.getSource();

					Point convertedDragWindowOffset = SwingUtilities.convertPoint(
							w, dragWindowOffset, titlePane);

					Frame f = null;
					Dialog d = null;

					if (w instanceof Frame) {
						f = (Frame) w;
					} else if (w instanceof Dialog) {
						d = (Dialog) w;
					}

					int frameState = (f != null) ? f.getExtendedState() : 0;

					if (titlePane != null && titlePane.contains(convertedDragWindowOffset)) {
						if ((f != null && ((frameState & Frame.MAXIMIZED_BOTH) == 0) || (d != null)) &&
								dragWindowOffset.y >= BORDER_DRAG_THICKNESS &&
								dragWindowOffset.x >= BORDER_DRAG_THICKNESS &&
								dragWindowOffset.x < w.getWidth() - BORDER_DRAG_THICKNESS) {
							isMovingWindow = true;
							System.out.println("dragging");
							// dragOffsetX = dragWindowOffset.x;
							// dragOffsetY = dragWindowOffset.y;
						}
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (isMovingWindow)
						System.out.println("no");
					isMovingWindow = false;
				}

			});
		}
	}

	@Override
	public void attach(RPanel panel) {
		if (owns(panel)) return;

		if (this.panel != null) {
			detach(this.panel);
		}

		this.panel = panel;
		panel.setOwner(this);
		this.ui.setContentPane(panel.getContentPane());
		this.ui.setTitle(panel.getTitle());
		activate(panel);
		this.ui.pack();

		this.ui.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				System.out.printf("%d,%d\n", ui.getX(), ui.getY());
			}
		});
	}

	@Override
	public void detach(RPanel panel) {
		if (!owns(panel)) return;

		this.panel = null;
		panel.setOwner(null);
		this.ui.setContentPane(new JPanel());
		this.ui.setTitle("");
		this.ui.setVisible(false);
	}

	@Override
	public boolean owns(RPanel panel) {
		return panel != null && this.panel == panel;
	}

	@Override
	public void titleChanged(RPanel panel) {
		if (this.panel == panel) {
			this.setTitle(this.panel.getTitle());
		}
	}

	@Override
	public void activate(RPanel panel) {
		if (!owns(panel)) return;

		this.ui.setVisible(true);
		this.ui.requestFocus();
	}

	@Override
	public void hide(RPanel panel) {
		if (!owns(panel)) return;

		this.ui.setVisible(false);
	}

	@Override
	public void addRPanelListener(RPanelListener listener) {
		throw new IllegalStateException("not implemented");
	}

	@Override
	public void removeRPanelListener(RPanelListener listener) {
		throw new IllegalStateException("not implemented");
	}

	@Override
	public Rectangle getPanelLocation(RPanel panel) {
		if (!owns(panel)) return null;

		return this.ui.getBounds();
	}

	@Override
	public void tryMoveTo(RPanel panel, Rectangle rect) {
		if (!owns(panel)) return;

		this.ui.setBounds(rect);
	}

	@Override
	public boolean isDedicatedHost() {
		return true;
	}

}
