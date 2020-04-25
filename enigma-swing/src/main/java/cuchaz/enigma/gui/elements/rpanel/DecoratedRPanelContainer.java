package cuchaz.enigma.gui.elements.rpanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JToggleButton;

public class DecoratedRPanelContainer {

	private final RPanelContainer inner;

	private final JPanel ui;
	private final JPanel buttonPanel;

	private final Map<RPanel, JToggleButton> buttons = new HashMap<>();

	private boolean updatingButtons;

	public DecoratedRPanelContainer(ButtonLocation buttonLocation) {
		inner = new RPanelContainer();

		ui = new JPanel();
		ui.setLayout(new BorderLayout());

		buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JLayer<JPanel> layer = new JLayer<>(buttonPanel);
		layer.setUI(new RotationLayerUI(buttonLocation.getRotation()));

		switch (buttonLocation) {
			case TOP:
				ui.add(layer, BorderLayout.NORTH);
				break;
			case BOTTOM:
				ui.add(layer, BorderLayout.SOUTH);
				break;
			case LEFT:
				ui.add(layer, BorderLayout.WEST);
				break;
			case RIGHT:
				ui.add(layer, BorderLayout.EAST);
				break;
		}

		ui.add(inner.getUi(), BorderLayout.CENTER);

		inner.addRPanelListener(new RPanelListener() {
			@Override
			public void onAttach(RPanelHost host, RPanel panel) {
				JToggleButton button = new JToggleButton(panel.getTitle());
				button.addItemListener(event -> {
					if (updatingButtons) return;

					if (event.getStateChange() == ItemEvent.SELECTED) {
						inner.activate(panel);
					} else if (event.getStateChange() == ItemEvent.DESELECTED) {
						inner.hide(panel);
					}
				});
				buttonPanel.add(button);
				buttons.put(panel, button);

				ui.validate();
			}

			@Override
			public void onDetach(RPanelHost host, RPanel panel) {
				JToggleButton button = buttons.remove(panel);
				buttonPanel.remove(button);

				ui.validate();
			}

			@Override
			public void onActivate(RPanelHost host, RPanel panel) {
				try {
					updatingButtons = true;
					buttons.get(panel).setSelected(true);
				} finally {
					updatingButtons = false;
				}
			}

			@Override
			public void onHide(RPanelHost host, RPanel panel) {
				try {
					updatingButtons = true;
					buttons.get(panel).setSelected(false);
				} finally {
					updatingButtons = false;
				}
			}
		});
	}

	public JPanel getUi() {
		return ui;
	}

	public RPanelContainer getInner() {
		return inner;
	}

	public enum ButtonLocation {
		TOP(2), BOTTOM(0), LEFT(1), RIGHT(3);

		private final int rotation;

		ButtonLocation(int rotation) {
			this.rotation = rotation;
		}

		public int getRotation() {
			return rotation;
		}
	}

}
