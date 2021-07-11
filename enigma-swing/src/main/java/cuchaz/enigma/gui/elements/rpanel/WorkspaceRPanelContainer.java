package cuchaz.enigma.gui.elements.rpanel;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import cuchaz.enigma.gui.elements.rpanel.DecoratedRPanelContainer.ButtonLocation;

public final class WorkspaceRPanelContainer {
	private final RPanelContainer leftTop = new RPanelContainer("Left Top");
	private final RPanelContainer leftBottom = new RPanelContainer("Left Bottom");
	private final RPanelContainer bottomLeft = new RPanelContainer("Bottom Left");
	private final RPanelContainer bottomRight = new RPanelContainer("Bottom Right");
	private final RPanelContainer rightBottom = new RPanelContainer("Right Bottom");
	private final RPanelContainer rightTop = new RPanelContainer("Right Top");
	private final RPanelContainer topRight = new RPanelContainer("Top Right");
	private final RPanelContainer topLeft = new RPanelContainer("Top Left");

	private final JPanel ui = new JPanel(new BorderLayout());

	private final JPanel leftButtonPanel = new JPanel(new BorderLayout());
	private final JPanel rightButtonPanel = new JPanel(new BorderLayout());
	private final JPanel bottomButtonPanel = new JPanel(new BorderLayout());
	private final JPanel topButtonPanel = new JPanel(new BorderLayout());

	private Container workArea = new JPanel();

	private final ManagedSplitPane leftSplit = new ManagedSplitPane(JSplitPane.VERTICAL_SPLIT, this.leftTop.getUi(), this.leftBottom.getUi());
	private final ManagedSplitPane rightSplit = new ManagedSplitPane(JSplitPane.VERTICAL_SPLIT, this.rightTop.getUi(), this.rightBottom.getUi());
	private final ManagedSplitPane bottomSplit = new ManagedSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.bottomLeft.getUi(), this.bottomRight.getUi());
	private final ManagedSplitPane topSplit = new ManagedSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.topLeft.getUi(), this.topRight.getUi());
	private final BorderSplitPane split = new BorderSplitPane(
			this.topSplit.getUi(),
			this.leftSplit.getUi(),
			this.bottomSplit.getUi(),
			this.rightSplit.getUi(),
			this.workArea
	);

	public WorkspaceRPanelContainer() {
		this.leftSplit.setResizeWeight(0.5);
		this.leftSplit.resetToPreferredSizes();
		this.rightSplit.setResizeWeight(0.5);
		this.rightSplit.resetToPreferredSizes();
		this.bottomSplit.setResizeWeight(0.5);
		this.bottomSplit.resetToPreferredSizes();
		this.topSplit.setResizeWeight(0.5);
		this.topSplit.resetToPreferredSizes();

		JPanel leftTopButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel leftBottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JPanel bottomRightButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel bottomLeftButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JPanel rightBottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel rightTopButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JPanel topLeftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel topRightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		this.leftButtonPanel.add(leftTopButtonPanel, BorderLayout.EAST);
		this.leftButtonPanel.add(leftBottomButtonPanel, BorderLayout.WEST);
		this.bottomButtonPanel.add(bottomRightButtonPanel, BorderLayout.EAST);
		this.bottomButtonPanel.add(bottomLeftButtonPanel, BorderLayout.WEST);
		this.rightButtonPanel.add(rightBottomButtonPanel, BorderLayout.EAST);
		this.rightButtonPanel.add(rightTopButtonPanel, BorderLayout.WEST);
		this.topButtonPanel.add(topLeftButtonPanel, BorderLayout.EAST);
		this.topButtonPanel.add(topRightButtonPanel, BorderLayout.WEST);

		JLayer<JPanel> leftLayer = new JLayer<>(this.leftButtonPanel);
		leftLayer.setUI(new RotationLayerUI(ButtonLocation.LEFT.getRotation()));
		JLayer<JPanel> rightLayer = new JLayer<>(this.rightButtonPanel);
		rightLayer.setUI(new RotationLayerUI(ButtonLocation.RIGHT.getRotation()));
		JLayer<JPanel> bottomLayer = new JLayer<>(this.bottomButtonPanel);
		bottomLayer.setUI(new RotationLayerUI(ButtonLocation.BOTTOM.getRotation()));
		JLayer<JPanel> topLayer = new JLayer<>(this.topButtonPanel);
		topLayer.setUI(new RotationLayerUI(ButtonLocation.TOP.getRotation()));

		this.ui.add(this.split.getUi(), BorderLayout.CENTER);
		this.ui.add(leftLayer, BorderLayout.WEST);
		this.ui.add(rightLayer, BorderLayout.EAST);
		this.ui.add(bottomLayer, BorderLayout.SOUTH);
		this.ui.add(topLayer, BorderLayout.NORTH);

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

		this.leftBottom.addRPanelListener(listener);
		this.leftTop.addRPanelListener(listener);
		this.rightBottom.addRPanelListener(listener);
		this.rightTop.addRPanelListener(listener);
		this.bottomLeft.addRPanelListener(listener);
		this.bottomRight.addRPanelListener(listener);
		this.topLeft.addRPanelListener(listener);
		this.topRight.addRPanelListener(listener);

		DecoratedRPanelContainer.initListenerForButtonBar(this.ui, this.leftBottom, leftBottomButtonPanel::add, leftBottomButtonPanel::remove);
		DecoratedRPanelContainer.initListenerForButtonBar(this.ui, this.leftTop, leftTopButtonPanel::add, leftTopButtonPanel::remove);
		DecoratedRPanelContainer.initListenerForButtonBar(this.ui, this.rightBottom, rightBottomButtonPanel::add, rightBottomButtonPanel::remove);
		DecoratedRPanelContainer.initListenerForButtonBar(this.ui, this.rightTop, rightTopButtonPanel::add, rightTopButtonPanel::remove);
		DecoratedRPanelContainer.initListenerForButtonBar(this.ui, this.bottomLeft, bottomLeftButtonPanel::add, bottomLeftButtonPanel::remove);
		DecoratedRPanelContainer.initListenerForButtonBar(this.ui, this.bottomRight, bottomRightButtonPanel::add, bottomRightButtonPanel::remove);
		DecoratedRPanelContainer.initListenerForButtonBar(this.ui, this.topLeft, topLeftButtonPanel::add, topLeftButtonPanel::remove);
		DecoratedRPanelContainer.initListenerForButtonBar(this.ui, this.topRight, topRightButtonPanel::add, topRightButtonPanel::remove);
	}

	private void updateUiState() {
		boolean leftBottomHasPanel = this.leftBottom.getActivePanel() != null;
		boolean leftTopHasPanel = this.leftTop.getActivePanel() != null;
		boolean rightBottomHasPanel = this.rightBottom.getActivePanel() != null;
		boolean rightTopHasPanel = this.rightTop.getActivePanel() != null;
		boolean bottomLeftHasPanel = this.bottomLeft.getActivePanel() != null;
		boolean bottomRightHasPanel = this.bottomRight.getActivePanel() != null;
		boolean topLeftHasPanel = this.topLeft.getActivePanel() != null;
		boolean topRightHasPanel = this.topRight.getActivePanel() != null;

		this.leftSplit.setState(leftTopHasPanel, leftBottomHasPanel);
		this.rightSplit.setState(rightTopHasPanel, rightBottomHasPanel);
		this.bottomSplit.setState(bottomLeftHasPanel, bottomRightHasPanel);
		this.topSplit.setState(topLeftHasPanel, topRightHasPanel);

		this.leftButtonPanel.setVisible((this.leftBottom.getVisiblePanelCount() | this.leftTop.getVisiblePanelCount()) != 0);
		this.rightButtonPanel.setVisible((this.rightBottom.getVisiblePanelCount() | this.rightTop.getVisiblePanelCount()) != 0);
		this.bottomButtonPanel.setVisible((this.bottomLeft.getVisiblePanelCount() | this.bottomRight.getVisiblePanelCount()) != 0);
		this.topButtonPanel.setVisible((this.topLeft.getVisiblePanelCount() | this.topRight.getVisiblePanelCount()) != 0);

		this.split.setState(
				topLeftHasPanel || topRightHasPanel,
				leftTopHasPanel || leftBottomHasPanel,
				bottomLeftHasPanel || bottomRightHasPanel,
				rightTopHasPanel || rightBottomHasPanel
		);

		this.ui.validate();
		this.ui.repaint();
	}

	public JPanel getUi() {
		return ui;
	}

	public Container getWorkArea() {
		return workArea;
	}

	public void setWorkArea(Container workArea) {
		this.split.setCenterComponent(workArea);
		this.workArea = workArea;
	}

	public RPanelContainer getLeftTop() {
		return leftTop;
	}

	public RPanelContainer getLeftBottom() {
		return leftBottom;
	}

	public RPanelContainer getBottomLeft() {
		return bottomLeft;
	}

	public RPanelContainer getBottomRight() {
		return bottomRight;
	}

	public RPanelContainer getRightBottom() {
		return rightBottom;
	}

	public RPanelContainer getRightTop() {
		return rightTop;
	}

	public RPanelContainer getTopRight() {
		return topRight;
	}

	public RPanelContainer getTopLeft() {
		return topLeft;
	}

	public RPanelContainer get(DockPosition pos) {
		return switch(pos) {
			case TOP_LEFT -> this.topLeft;
			case TOP_RIGHT -> this.topRight;
			case LEFT_TOP -> this.leftTop;
			case LEFT_BOTTOM -> this.leftBottom;
			case BOTTOM_LEFT -> this.bottomLeft;
			case BOTTOM_RIGHT -> this.bottomRight;
			case RIGHT_TOP -> this.rightTop;
			case RIGHT_BOTTOM -> this.rightBottom;
		};
	}

	public void addToGroup(RPanelGroup group) {
		group.addHost(this.leftTop);
		group.addHost(this.leftBottom);
		group.addHost(this.bottomLeft);
		group.addHost(this.bottomRight);
		group.addHost(this.rightBottom);
		group.addHost(this.rightTop);
		group.addHost(this.topRight);
		group.addHost(this.topLeft);
	}

	public enum DockPosition {
		TOP_LEFT,
		TOP_RIGHT,
		LEFT_TOP,
		LEFT_BOTTOM,
		BOTTOM_LEFT,
		BOTTOM_RIGHT,
		RIGHT_TOP,
		RIGHT_BOTTOM,
	}
}
