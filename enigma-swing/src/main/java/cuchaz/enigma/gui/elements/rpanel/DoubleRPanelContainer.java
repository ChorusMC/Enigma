package cuchaz.enigma.gui.elements.rpanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import cuchaz.enigma.gui.elements.rpanel.DecoratedRPanelContainer.ButtonLocation;

public class DoubleRPanelContainer {

	private final RPanelContainer left;
	private final RPanelContainer right;

	private final JPanel ui;
	private final JPanel buttonPanel;

	public DoubleRPanelContainer(ButtonLocation buttonLocation) {
		left = new RPanelContainer();
		right = new RPanelContainer();

		ui = new JPanel();
		ui.setLayout(new BorderLayout());

		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());

		JPanel leftButtonPanel = new JPanel();
		JPanel rightButtonPanel = new JPanel();
		leftButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		rightButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		buttonPanel.add(leftButtonPanel, BorderLayout.WEST);
		buttonPanel.add(rightButtonPanel, BorderLayout.EAST);

		JLayer<JPanel> layer = new JLayer<>(buttonPanel);
		layer.setUI(new RotationLayerUI(buttonLocation.getRotation()));

		JSplitPane sp = null;

		switch (buttonLocation) {
			case TOP:
				ui.add(layer, BorderLayout.NORTH);
				sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, right.getUi(), left.getUi());
				break;
			case BOTTOM:
				ui.add(layer, BorderLayout.SOUTH);
				sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, left.getUi(), right.getUi());
				break;
			case LEFT:
				ui.add(layer, BorderLayout.WEST);
				sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, right.getUi(), left.getUi());
				break;
			case RIGHT:
				ui.add(layer, BorderLayout.EAST);
				sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, left.getUi(), right.getUi());
				break;
		}
		sp.setResizeWeight(0.5);
		sp.resetToPreferredSizes();

		ui.add(sp, BorderLayout.CENTER);

		DecoratedRPanelContainer.initListenerForButtonBar(ui, left, leftButtonPanel::add, leftButtonPanel::remove);
		DecoratedRPanelContainer.initListenerForButtonBar(ui, right, rightButtonPanel::add, rightButtonPanel::remove);
	}

	public JPanel getUi() {
		return ui;
	}

	public RPanelHost getLeft() {
		return left;
	}

	public RPanelHost getRight() {
		return right;
	}

}
