package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

public class ManagedSplitPane {
	private final JPanel ui = new JPanel(new GridLayout(1, 1, 0, 0));
	private final JPanel leftPanel = new JPanel(new GridLayout(1, 1, 0, 0));
	private final JPanel rightPanel = new JPanel(new GridLayout(1, 1, 0, 0));
	private final JSplitPane splitPane;
	private final Component left;
	private final Component right;

	public ManagedSplitPane(int newOrientation, Component left, Component right) {
		this.splitPane = new JSplitPane(newOrientation, true, this.leftPanel, this.rightPanel);
		this.left = left;
		this.right = right;

		this.setState(true, true);
		this.splitPane.setResizeWeight(0.5);
		this.splitPane.resetToPreferredSizes();
	}

	public void setState(boolean leftActive, boolean rightActive) {
		this.ui.removeAll();
		this.leftPanel.removeAll();
		this.rightPanel.removeAll();

		if (leftActive && rightActive) {
			this.leftPanel.add(this.left);
			this.rightPanel.add(this.right);
			this.ui.add(this.splitPane);
		} else if (leftActive) {
			this.ui.add(this.left);
		} else if (rightActive) {
			this.ui.add(this.right);
		}

		this.ui.validate();
		this.ui.repaint();
	}

	public JPanel getUi() {
		return this.ui;
	}
}
