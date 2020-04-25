package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class RPanelWindow extends JFrame implements RPanelHost {

	private RPanel panel;

	@Override
	public void attach(RPanel panel) {
		if (owns(panel)) return;

		if (this.panel != null) {
			detach(this.panel);
		}

		this.panel = panel;
		panel.setOwner(this);
		this.setContentPane(panel.getContentPane());
		this.setTitle(panel.getTitle());
		activate(panel);
	}

	@Override
	public void detach(RPanel panel) {
		if (!owns(panel)) return;

		this.panel = null;
		panel.setOwner(null);
		this.setContentPane(new JPanel());
		this.setTitle("");
		this.setVisible(false);
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
	public boolean isDedicatedHost() {
		return true;
	}

	@Override
	public Rectangle getPanelLocation(RPanel panel) {
		if (!owns(panel)) return null;

		return this.getBounds();
	}

	@Override
	public void tryMoveTo(RPanel panel, Rectangle rect) {
		if (!owns(panel)) return;

		setBounds(rect);
	}

	@Override
	public void activate(RPanel panel) {
		if (!owns(panel)) return;

		setVisible(true);
		requestFocus();
	}

	@Override
	public void hide(RPanel panel) {
		if (!owns(panel)) return;

		setVisible(false);
	}
}
