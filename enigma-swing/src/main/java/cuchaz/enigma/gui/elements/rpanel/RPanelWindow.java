package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Rectangle;
import java.awt.Window;
import java.util.Comparator;

import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import cuchaz.enigma.utils.Pair;

public class RPanelWindow implements RPanelHost {

	private final JFrame ui;
	private RPanel panel;

	private long lastDragOp;
	private boolean isMinimized;

	public RPanelWindow() {
		this.ui = new JFrame();

		// Unfortunately, having window decorations handled by the OS makes it
		// not receive position update events until the user lets go of the
		// window.
		this.ui.setUndecorated(true);
		this.ui.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);

		RootPaneUtil.registerDragListener(this.ui, new WindowDragListener() {
			@Override
			public void onStopDrag(Window w) {
				onDropped();
			}

			@Override
			public void onDragMove(Window w) {
				onDragged();
			}
		});
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
	}

	@Override
	public void detach(RPanel panel) {
		if (!owns(panel)) return;

		this.panel = null;
		panel.setOwner(null);
		this.ui.setContentPane(new JPanel());
		this.ui.setTitle("");
		this.ui.setVisible(false);
		this.ui.dispose();
	}

	@Override
	public boolean owns(RPanel panel) {
		return panel != null && this.panel == panel;
	}

	@Nullable
	@Override
	public RPanel getActivePanel() {
		if (this.ui.isVisible()) {
			return panel;
		} else {
			return null;
		}
	}

	@Override
	public void titleChanged(RPanel panel) {
		if (this.panel == panel) {
			this.ui.setTitle(this.panel.getTitle());
		}
	}

	@Override
	public void activate(RPanel panel) {
		if (!owns(panel)) return;

		isMinimized = false;
		updateVisibleState(panel);
		if (this.ui.isVisible()) this.ui.requestFocus();
	}

	@Override
	public void minimize(RPanel panel) {
		if (!owns(panel)) return;

		isMinimized = true;
		updateVisibleState(panel);
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
	public void updateVisibleState(RPanel panel) {
		if (!owns(panel)) return;

		this.ui.setVisible(panel.isVisible() && !isMinimized);
	}

	@Override
	public Rectangle getPanelLocation(RPanel panel) {
		if (!owns(panel)) return null;

		Rectangle bounds = this.ui.getBounds();
		bounds.setLocation(this.ui.getLocationOnScreen());
		return bounds;
	}

	@Override
	public void tryMoveTo(RPanel panel, Rectangle rect) {
		if (!owns(panel)) return;

		this.ui.setBounds(rect);
	}

	@Override
	public Rectangle getDragInsertBounds() {
		return null;
	}

	@Override
	public boolean onDragOver(RPanel panel) {
		return false;
	}

	private void onDragged() {
		lastDragOp = System.currentTimeMillis();
	}

	private void onDropped() {
		if (System.currentTimeMillis() - lastDragOp < 1000) {
			Rectangle b = this.ui.getBounds();
			panel.getDragTargets().stream()
					.filter(t -> t.getDragInsertBounds() != null)
					.map(t -> new Pair<>(t, getOverlap(b, t.getDragInsertBounds())))
					.sorted(Comparator.comparingDouble(t -> t.b))
					.filter(t -> t.b < 200 * 200)
					.filter(t -> t.a.onDragOver(panel))
					.map(t -> t.a)
					.findFirst()
					.ifPresent(t -> {
						panel.attach(t);
						t.activate(panel);
					});
		}
	}

	private static double getOverlap(Rectangle a, Rectangle b) {
		double dx = b.getCenterX() - a.getCenterX();
		double dy = b.getCenterY() - a.getCenterY();
		return dx * dx + dy * dy;
	}

	@Override
	public boolean isDedicatedHost() {
		return true;
	}

}
