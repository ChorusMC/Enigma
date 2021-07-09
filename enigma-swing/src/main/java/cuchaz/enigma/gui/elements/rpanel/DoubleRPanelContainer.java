package cuchaz.enigma.gui.elements.rpanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import cuchaz.enigma.gui.elements.rpanel.DecoratedRPanelContainer.ButtonLocation;

public class DoubleRPanelContainer {

	private final RPanelContainer left;
	private final RPanelContainer right;
	private final JSplitPane splitPane;

	private final JPanel ui;
	private final JPanel buttonPanel;

	private final JPanel centerPanel;
	private final JPanel leftPanel;
	private final JPanel rightPanel;

	public DoubleRPanelContainer(ButtonLocation buttonLocation) {
		left = new RPanelContainer();
		right = new RPanelContainer();

		ui = new JPanel(new BorderLayout());
		centerPanel = new JPanel(new GridLayout(1, 1, 0, 0));
		leftPanel = new JPanel(new GridLayout(1, 1, 0, 0));
		rightPanel = new JPanel(new GridLayout(1, 1, 0, 0));
		buttonPanel = new JPanel(new BorderLayout());

		JPanel leftButtonPanel = new JPanel();
		JPanel rightButtonPanel = new JPanel();
		leftButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		rightButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		buttonPanel.add(leftButtonPanel, BorderLayout.WEST);
		buttonPanel.add(rightButtonPanel, BorderLayout.EAST);

		JLayer<JPanel> layer = new JLayer<>(buttonPanel);
		layer.setUI(new RotationLayerUI(buttonLocation.getRotation()));

		switch (buttonLocation) {
			case TOP:
				ui.add(layer, BorderLayout.NORTH);
				splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, rightPanel, leftPanel);
				break;
			case BOTTOM:
				ui.add(layer, BorderLayout.SOUTH);
				splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, leftPanel, rightPanel);
				break;
			case LEFT:
				ui.add(layer, BorderLayout.WEST);
				splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, rightPanel, leftPanel);
				break;
			case RIGHT:
				ui.add(layer, BorderLayout.EAST);
				splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, leftPanel, rightPanel);
				break;
			default:
				throw new IllegalStateException("unreachable");
		}
		splitPane.setResizeWeight(0.5);
		splitPane.resetToPreferredSizes();

		ui.add(centerPanel, BorderLayout.CENTER);

		RPanelListener listener = new RPanelListener() {
			@Override
			public void onActivate(RPanelHost host, RPanel panel) {
				updateUiState();
			}

			@Override
			public void onMinimize(RPanelHost host, RPanel panel) {
				updateUiState();
			}
		};

		left.addRPanelListener(listener);
		right.addRPanelListener(listener);
		updateUiState();

		DecoratedRPanelContainer.initListenerForButtonBar(ui, left, leftButtonPanel::add, leftButtonPanel::remove);
		DecoratedRPanelContainer.initListenerForButtonBar(ui, right, rightButtonPanel::add, rightButtonPanel::remove);
	}

	private void updateUiState() {
		boolean leftHasPanel = left.getActivePanel() != null;
		boolean rightHasPanel = right.getActivePanel() != null;
		centerPanel.removeAll();
		leftPanel.removeAll();
		rightPanel.removeAll();
		if (leftHasPanel && rightHasPanel) {
			leftPanel.add(left.getUi());
			rightPanel.add(right.getUi());
			centerPanel.add(splitPane);
		} else if (!rightHasPanel) {
			centerPanel.add(left.getUi());
		} else {
			centerPanel.add(right.getUi());
		}

		buttonPanel.setVisible((left.getVisiblePanelCount() | right.getVisiblePanelCount()) != 0);

		this.ui.validate();
		this.ui.repaint();
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

	public void addToGroup(RPanelGroup group) {
		group.addHost(this.left);
		group.addHost(this.right);
	}

}
