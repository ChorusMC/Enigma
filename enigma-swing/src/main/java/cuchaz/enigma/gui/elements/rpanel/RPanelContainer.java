package cuchaz.enigma.gui.elements.rpanel;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JToggleButton;

public class RPanelContainer implements RPanelHost {

	private final JPanel ui;

	private final Map<RPanel, StandaloneRootPane> panels = new HashMap<>();
	private RPanel openPanel = null;

	private List<RPanelListener> listeners = new ArrayList<>();

	public RPanelContainer() {
		ui = new JPanel();
		ui.setLayout(new BorderLayout());
	}

	public JPanel getUi() {
		return ui;
	}

	@Override
	public void attach(RPanel panel) {
		if (owns(panel)) return;

		JPanel dummy = new JPanel();
		StandaloneRootPane rp = new StandaloneRootPane();
		rp.addCloseListener(() -> detach(panel));
		rp.setTitle(panel.getTitle());
		rp.setContentPane(panel.getContentPane());
		dummy.add(rp);
		rp.setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
		RootPaneUtil.registerDragListener(rp, new WindowDragListener() {
			@Override
			public void onDragMove(Window w) {
				panel.windowize();
			}
		});
		dummy.remove(rp);

		panels.put(panel, rp);
		panel.setOwner(this);

		listeners.forEach(l -> l.onAttach(this, panel));

		if (openPanel == null) {
			activate(panel);
		}
	}

	@Override
	public void detach(RPanel panel) {
		if (!owns(panel)) return;

		hide(panel);

		StandaloneRootPane rp = panels.remove(panel);
		panel.setOwner(null);

		rp.setContentPane(new JPanel());

		listeners.forEach(l -> l.onDetach(this, panel));

		this.ui.validate();
	}

	@Override
	public boolean owns(RPanel panel) {
		return panel != null && panels.containsKey(panel);
	}

	@Override
	public void titleChanged(RPanel panel) {
		StandaloneRootPane pane = this.panels.get(panel);

		if (pane != null) {
			pane.setTitle(panel.getTitle());
		}
	}

	@Override
	public void activate(RPanel panel) {
		if (!owns(panel)) return;

		hide(openPanel);
		openPanel = panel;

		StandaloneRootPane rp = panels.get(panel);
		this.ui.add(rp, BorderLayout.CENTER);

		listeners.forEach(l -> l.onActivate(this, panel));

		this.ui.validate();
		this.ui.repaint();
	}

	@Override
	public void hide(RPanel panel) {
		if (!owns(panel)) return;
		if (openPanel != panel) return;

		this.ui.remove(panels.get(openPanel));
		openPanel = null;

		listeners.forEach(l -> l.onHide(this, panel));

		this.ui.repaint();
	}

	@Override
	public void addRPanelListener(RPanelListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeRPanelListener(RPanelListener listener) {
		listeners.remove(listener);
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
		// no
	}

	@Override
	public Rectangle getDragInsertBounds() {
		Rectangle bounds = this.ui.getBounds();
		bounds.setLocation(this.ui.getLocationOnScreen());
		return bounds;
	}

	@Override
	public boolean onDragOver(RPanel panel) {
		// TODO draw
		return true;
	}

	@Override
	public boolean isDedicatedHost() {
		return false;
	}

}
