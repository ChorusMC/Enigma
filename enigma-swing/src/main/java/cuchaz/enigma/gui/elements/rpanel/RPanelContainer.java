package cuchaz.enigma.gui.elements.rpanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class RPanelContainer extends JPanel implements RPanelHost {

	private final ButtonLocation buttonLocation;

	private final List<RPanel> panels = new ArrayList<>();
	private RPanel openPanel = null;

	private final Map<RPanel, JToggleButton> buttons;
	private final JPanel buttonPanel;

	private boolean updatingButtons;

	public RPanelContainer() {
		this(ButtonLocation.NONE);
	}

	public RPanelContainer(ButtonLocation buttonLocation) {
		this.setLayout(new BorderLayout());

		this.buttonLocation = buttonLocation;

		if (buttonLocation == ButtonLocation.NONE) {
			buttonPanel = null;
			buttons = null;
		} else {
			buttons = new HashMap<>();
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			JLayer<JPanel> layer = new JLayer<>(buttonPanel);
			layer.setUI(new RotationLayerUI(buttonLocation.getRotation()));

			switch (buttonLocation) {
				case TOP:
					this.add(layer, BorderLayout.NORTH);
					break;
				case BOTTOM:
					this.add(layer, BorderLayout.SOUTH);
					break;
				case LEFT:
					this.add(layer, BorderLayout.WEST);
					break;
				case RIGHT:
					this.add(layer, BorderLayout.EAST);
					break;
			}
		}

	}

	@Override
	public void attach(RPanel panel) {
		if (owns(panel)) return;

		panels.add(panel);
		panel.setOwner(this);

		if (buttonLocation != ButtonLocation.NONE) {
			JToggleButton button = new JToggleButton(panel.getTitle());
			button.addItemListener(event -> {
				if (updatingButtons) return;

				if (event.getStateChange() == ItemEvent.SELECTED) {
					activate(panel);
				} else if (event.getStateChange() == ItemEvent.DESELECTED) {
					hide(panel);
				}
			});
			buttons.put(panel, button);
			buttonPanel.add(button);
		}

		activate(panel);
	}

	@Override
	public void detach(RPanel panel) {
		if (!owns(panel)) return;

		hide(panel);

		panels.remove(panel);
		panel.setOwner(null);

		if (buttonLocation != ButtonLocation.NONE) {
			JToggleButton button = buttons.remove(panel);
			buttonPanel.remove(button);
		}
	}

	@Override
	public boolean owns(RPanel panel) {
		return panel != null && panels.contains(panel);
	}

	@Override
	public void titleChanged(RPanel panel) {
		JToggleButton button = this.buttons.get(panel);

		if (button != null) {
			button.setText(panel.getTitle());
		}
	}

	@Override
	public Rectangle getPanelLocation(RPanel panel) {
		if (!owns(panel)) return null;

		return getBounds();
	}

	@Override
	public void tryMoveTo(RPanel panel, Rectangle rect) {
		// no
	}

	@Override
	public void activate(RPanel panel) {
		if (!owns(panel)) return;

		hide(openPanel);
		openPanel = panel;
		this.add(panel.getContentPane(), BorderLayout.CENTER);

		if (buttonLocation != ButtonLocation.NONE) {
			try {
				updatingButtons = true;
				buttons.get(panel).setSelected(true);
			} finally {
				updatingButtons = false;
			}
		}
	}

	@Override
	public void hide(RPanel panel) {
		if (!owns(panel)) return;
		if (openPanel != panel) return;

		this.remove(openPanel.getContentPane());
		openPanel = null;

		if (buttonLocation != ButtonLocation.NONE) {
			try {
				updatingButtons = true;
				buttons.get(panel).setSelected(false);
			} finally {
				updatingButtons = false;
			}
		}
	}

	@Override
	public boolean isDedicatedHost() {
		return false;
	}

	public enum ButtonLocation {
		NONE, TOP(2), BOTTOM(0), LEFT(1), RIGHT(3);

		private final int rotation;

		ButtonLocation() {
			this(0);
		}

		ButtonLocation(int rotation) {
			this.rotation = rotation;
		}

		public int getRotation() {
			return rotation;
		}
	}

}
