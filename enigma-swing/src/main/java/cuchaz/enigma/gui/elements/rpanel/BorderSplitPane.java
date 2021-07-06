package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

public class BorderSplitPane {
	private final ManagedSplitPane left;
	private final ManagedSplitPane right;
	private final ManagedSplitPane top;
	private final ManagedSplitPane bottom;

	public BorderSplitPane(
			Component top,
			Component left,
			Component bottom,
			Component right,
			Component center
	) {
		this.left = new ManagedSplitPane(JSplitPane.VERTICAL_SPLIT, left, center);
		this.right = new ManagedSplitPane(JSplitPane.VERTICAL_SPLIT, this.left.getUi(), right);
		this.top = new ManagedSplitPane(JSplitPane.HORIZONTAL_SPLIT, top, this.right.getUi());
		this.bottom = new ManagedSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.top.getUi(), bottom);
	}

	public void setState(boolean top, boolean left, boolean bottom, boolean right) {
		this.top.setState(top, true);
		this.left.setState(left, true);
		this.bottom.setState(true, bottom);
		this.right.setState(true, right);
	}

	public void setCenterComponent(Component center) {
		this.left.setRightComponent(center);
	}

	public Component getCenterComponent() {
		return this.left.getRightComponent();
	}

	public JPanel getUi() {
		return this.bottom.getUi();
	}
}
