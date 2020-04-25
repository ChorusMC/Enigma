package cuchaz.enigma.gui.elements.rpanel;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.SwingUtilities;
import javax.swing.plaf.LayerUI;

// HUGE amount of code taken from here: https://stackoverflow.com/a/43235638
public class RotationLayerUI extends LayerUI<JComponent> {

	private final int rotation;

	private Component lastEnteredTarget, lastPressedTarget;

	private boolean dispatchingMode = false;

	public RotationLayerUI(int rotation) {
		this.rotation = (rotation % 4 + 4) % 4;
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.translate(0, c.getHeight());
		g2d.rotate(-rotation * (Math.PI * 0.5));
		super.paint(g2d, c);
	}

	@Override
	public void doLayout(JLayer<? extends JComponent> l) {
		Component view = l.getView();
		Dimension d = rotate(new Dimension(l.getWidth(), l.getHeight()), rotation);
		if (view != null) {
			view.setBounds(0, 0, d.width, d.height);
		}
		Component glassPane = l.getGlassPane();
		if (glassPane != null) {
			glassPane.setBounds(0, 0, d.width, d.height);
		}
	}

	/**
	 * Find the deepest component in the AWT hierarchy
	 *
	 * @param layer       the layer to which this UI is installed
	 * @param targetPoint the point in layer's coordinates
	 * @return the component in the specified point
	 */
	private Component getTarget(JLayer<? extends JComponent> layer, Point targetPoint) {
		Component view = layer.getView();
		if (view == null) {
			return null;
		} else {
			Point viewPoint = SwingUtilities.convertPoint(layer, targetPoint, view);
			return SwingUtilities.getDeepestComponentAt(view, viewPoint.x, viewPoint.y);
		}
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		return rotate(super.getPreferredSize(c), rotation);
	}

	@Override
	public Dimension getMinimumSize(JComponent c) {
		return rotate(super.getMinimumSize(c), rotation);
	}

	@Override
	public Dimension getMaximumSize(JComponent c) {
		return rotate(super.getMaximumSize(c), rotation);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
		JLayer<Component> l = (JLayer<Component>) c;
		l.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void uninstallUI(JComponent c) {
		JLayer<Component> l = (JLayer<Component>) c;
		l.setLayerEventMask(0);
		super.uninstallUI(c);
	}

	/**
	 * Process the mouse events and map the mouse coordinates inverting the internal affine transformation.
	 *
	 * @param event the event to be dispatched
	 * @param layer the layer this LayerUI is set to
	 */
	@Override
	public void eventDispatched(AWTEvent event, JLayer<? extends JComponent> layer) {
		if (event instanceof MouseEvent) {
			MouseEvent mouseEvent = (MouseEvent) event;
			// The if discriminate between generated and original event.
			// Removing it cause a stack overflow caused by the event being redispatched to this class.

			if (!dispatchingMode) {
				// Process an original mouse event
				dispatchingMode = true;
				try {
					redispatchMouseEvent(mouseEvent, layer);
				} finally {
					dispatchingMode = false;
				}
			} else {
				// Process generated mouse events
				// Added a check, because on mouse entered or exited, the cursor
				// may be set to specific dragging cursors.

				if (MouseEvent.MOUSE_ENTERED == mouseEvent.getID() || MouseEvent.MOUSE_EXITED == mouseEvent.getID()) {
					layer.getGlassPane().setCursor(null);
				} else {
					Component component = mouseEvent.getComponent();
					layer.getGlassPane().setCursor(component.getCursor());
				}
			}
		} else {
			super.eventDispatched(event, layer);
		}
		layer.repaint();
	}

	private void redispatchMouseEvent(MouseEvent originalEvent, JLayer<? extends JComponent> layer) {
		if (layer.getView() != null) {
			if (originalEvent.getComponent() != layer.getGlassPane()) {
				originalEvent.consume();
			}
			MouseEvent newEvent = null;

			Point realPoint = transform(originalEvent.getX(), originalEvent.getY(), layer.getWidth(), layer.getHeight(), rotation);
			Component realTarget = getTarget(layer, realPoint);

			if (realTarget != null) {
				realTarget = getListeningComponent(originalEvent, realTarget);
			}

			switch (originalEvent.getID()) {
				case MouseEvent.MOUSE_PRESSED:
					newEvent = transformMouseEvent(layer, originalEvent, realTarget, realPoint);
					if (newEvent != null) {
						lastPressedTarget = newEvent.getComponent();
					}
					break;
				case MouseEvent.MOUSE_RELEASED:
					newEvent = transformMouseEvent(layer, originalEvent, lastPressedTarget, realPoint);
					lastPressedTarget = null;
					break;
				case MouseEvent.MOUSE_CLICKED:
					newEvent = transformMouseEvent(layer, originalEvent, realTarget, realPoint);
					lastPressedTarget = null;
					break;
				case MouseEvent.MOUSE_MOVED:
					newEvent = transformMouseEvent(layer, originalEvent, realTarget, realPoint);
					generateEnterExitEvents(layer, originalEvent, realTarget, realPoint);
					break;
				case MouseEvent.MOUSE_ENTERED:
					generateEnterExitEvents(layer, originalEvent, realTarget, realPoint);
					break;
				case MouseEvent.MOUSE_EXITED:
					generateEnterExitEvents(layer, originalEvent, realTarget, realPoint);
					break;
				case MouseEvent.MOUSE_DRAGGED:
					newEvent = transformMouseEvent(layer, originalEvent, lastPressedTarget, realPoint);
					generateEnterExitEvents(layer, originalEvent, realTarget, realPoint);
					break;
				case MouseEvent.MOUSE_WHEEL:
					newEvent = transformMouseWheelEvent(layer, (MouseWheelEvent) originalEvent, realTarget, realPoint);
					break;
			}
			dispatchMouseEvent(newEvent);
		}
	}

	private MouseEvent transformMouseEvent(JLayer<? extends JComponent> layer, MouseEvent mouseEvent, Component target, Point realPoint) {
		return transformMouseEvent(layer, mouseEvent, target, realPoint, mouseEvent.getID());
	}

	/**
	 * Create the new event to being dispatched
	 */
	private MouseEvent transformMouseEvent(JLayer<? extends JComponent> layer, MouseEvent mouseEvent, Component target, Point targetPoint, int id) {
		if (target == null) {
			return null;
		} else {
			Point newPoint = SwingUtilities.convertPoint(layer, targetPoint, target);
			return new MouseEvent(target,
					id,
					mouseEvent.getWhen(),
					mouseEvent.getModifiers(),
					newPoint.x,
					newPoint.y,
					mouseEvent.getClickCount(),
					mouseEvent.isPopupTrigger(),
					mouseEvent.getButton());
		}
	}

	/**
	 * Create the new mouse wheel event to being dispached
	 */
	private MouseWheelEvent transformMouseWheelEvent(JLayer<? extends JComponent> layer, MouseWheelEvent mouseWheelEvent, Component target, Point targetPoint) {
		if (target == null) {
			return null;
		} else {
			Point newPoint = SwingUtilities.convertPoint(layer, targetPoint, target);
			return new MouseWheelEvent(target,
					mouseWheelEvent.getID(),
					mouseWheelEvent.getWhen(),
					mouseWheelEvent.getModifiers(),
					newPoint.x,
					newPoint.y,
					mouseWheelEvent.getClickCount(),
					mouseWheelEvent.isPopupTrigger(),
					mouseWheelEvent.getScrollType(),
					mouseWheelEvent.getScrollAmount(),
					mouseWheelEvent.getWheelRotation()
			);
		}
	}

	/**
	 * dispatch the {@code mouseEvent}
	 *
	 * @param mouseEvent the event to be dispatched
	 */
	private void dispatchMouseEvent(MouseEvent mouseEvent) {
		if (mouseEvent != null) {
			Component target = mouseEvent.getComponent();
			target.dispatchEvent(mouseEvent);
		}
	}

	/**
	 * Get the listening component associated to the {@code component}'s {@code event}
	 */
	private Component getListeningComponent(MouseEvent event, Component component) {
		switch (event.getID()) {
			case MouseEvent.MOUSE_CLICKED:
			case MouseEvent.MOUSE_ENTERED:
			case MouseEvent.MOUSE_EXITED:
			case MouseEvent.MOUSE_PRESSED:
			case MouseEvent.MOUSE_RELEASED:
				return getMouseListeningComponent(component);
			case MouseEvent.MOUSE_DRAGGED:
			case MouseEvent.MOUSE_MOVED:
				return getMouseMotionListeningComponent(component);
			case MouseEvent.MOUSE_WHEEL:
				return getMouseWheelListeningComponent(component);
		}
		return null;
	}

	/**
	 * Cycles through the {@code component}'s parents to find the {@link Component} with associated {@link MouseListener}
	 */
	private Component getMouseListeningComponent(Component component) {
		if (component.getMouseListeners().length > 0) {
			return component;
		} else {
			Container parent = component.getParent();
			if (parent != null) {
				return getMouseListeningComponent(parent);
			} else {
				return null;
			}
		}
	}

	/**
	 * Cycles through the {@code component}'s parents to find the {@link Component} with associated {@link MouseMotionListener}
	 */
	private Component getMouseMotionListeningComponent(Component component) {
		// Mouse motion events may result in MOUSE_ENTERED and MOUSE_EXITED.
		// Therefore, components with MouseListeners registered should be
		// returned as well.

		if (component.getMouseMotionListeners().length > 0 || component.getMouseListeners().length > 0) {
			return component;
		} else {
			Container parent = component.getParent();
			if (parent != null) {
				return getMouseMotionListeningComponent(parent);
			} else {
				return null;
			}
		}
	}

	/**
	 * Cycles through the {@code component}'s parents to find the {@link Component} with associated {@link MouseWheelListener}
	 */
	private Component getMouseWheelListeningComponent(Component component) {
		if (component.getMouseWheelListeners().length > 0) {
			return component;
		} else {
			Container parent = component.getParent();
			if (parent != null) {
				return getMouseWheelListeningComponent(parent);
			} else {
				return null;
			}
		}
	}

	/**
	 * Generate a {@code MOUSE_ENTERED} and {@code MOUSE_EXITED} event when the target component is changed
	 */
	private void generateEnterExitEvents(JLayer<? extends JComponent> layer, MouseEvent originalEvent, Component newTarget, Point realPoint) {
		if (lastEnteredTarget != newTarget) {
			dispatchMouseEvent(transformMouseEvent(layer, originalEvent, lastEnteredTarget, realPoint, MouseEvent.MOUSE_EXITED));
			lastEnteredTarget = newTarget;
			dispatchMouseEvent(transformMouseEvent(layer, originalEvent, lastEnteredTarget, realPoint, MouseEvent.MOUSE_ENTERED));
		}
	}

	@SuppressWarnings("SuspiciousNameCombination")
	private static Dimension rotate(Dimension self, int rotation) {
		if (rotation % 2 == 0) {
			return self;
		} else {
			return new Dimension(self.height, self.width);
		}
	}

	@SuppressWarnings("SuspiciousNameCombination")
	private static Point transform(int x, int y, int width, int height, int rotation) {
		if (rotation == 0) {
			return new Point(x, y);
		} else if (rotation == 1) {
			return new Point(height - y, x);
		} else if (rotation == 2) {
			return new Point(width - x, height - y);
		} else if (rotation == 3) {
			return new Point(y, width - x);
		} else {
			throw new IllegalArgumentException(String.format("Invalid rotation %d", rotation));
		}
	}

}
