package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Container;

import javax.swing.JPanel;

public class WorkspaceRPanelContainer {

	private final RPanelContainer leftTop;
	private final RPanelContainer leftBottom;
	private final RPanelContainer bottomLeft;
	private final RPanelContainer bottomRight;
	private final RPanelContainer rightBottom;
	private final RPanelContainer rightTop;
	private final RPanelContainer topRight;
	private final RPanelContainer topLeft;

	private final JPanel ui;
	private Container workArea;

	public WorkspaceRPanelContainer() {
		leftTop = new RPanelContainer();
		leftBottom = new RPanelContainer();
		bottomLeft = new RPanelContainer();
		bottomRight = new RPanelContainer();
		rightBottom = new RPanelContainer();
		rightTop = new RPanelContainer();
		topRight = new RPanelContainer();
		topLeft = new RPanelContainer();

		ui = new JPanel();
		workArea = new JPanel();
	}

	public JPanel getUi() {
		return ui;
	}

	public Container getWorkArea() {
		return workArea;
	}

	public void setWorkArea(Container workArea) {
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

	public void addDragTarget(RPanel panel) {
		panel.addDragTarget(leftTop);
		panel.addDragTarget(leftBottom);
		panel.addDragTarget(bottomLeft);
		panel.addDragTarget(bottomRight);
		panel.addDragTarget(rightBottom);
		panel.addDragTarget(rightTop);
		panel.addDragTarget(topRight);
		panel.addDragTarget(topLeft);
	}

}
