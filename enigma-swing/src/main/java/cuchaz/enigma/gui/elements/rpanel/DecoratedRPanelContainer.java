package cuchaz.enigma.gui.elements.rpanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class DecoratedRPanelContainer {

	private final RPanelContainer inner;

	private final JPanel ui;
	private final JPanel buttonPanel;

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

		initListenerForButtonBar(ui, inner, buttonPanel::add, buttonPanel::remove);
	}

	public JPanel getUi() {
		return ui;
	}

	public RPanelContainer getInner() {
		return inner;
	}

	public void addToGroup(RPanelGroup group) {
		group.addHost(this.inner);
	}

	public static void initListenerForButtonBar(JComponent ui, RPanelHost panelHost, Consumer<JToggleButton> addCallback, Consumer<JToggleButton> removeCallback) {
		Map<RPanel, JToggleButton> buttons = new HashMap<>();
		boolean[] updatingButtons = new boolean[] { false };

		panelHost.addRPanelListener(new RPanelListener() {
			private void addButtonFor(RPanelHost host, RPanel panel) {
				if (buttons.containsKey(panel)) return;
				JToggleButton button = new JToggleButton(panel.getTitle());
				if (host.getActivePanel() == panel) {
					button.setSelected(true);
				}
				button.addItemListener(event -> {
					if (updatingButtons[0]) return;

					if (event.getStateChange() == ItemEvent.SELECTED) {
						panelHost.activate(panel);
					} else if (event.getStateChange() == ItemEvent.DESELECTED) {
						panelHost.minimize(panel);
					}
				});
				addCallback.accept(button);
				buttons.put(panel, button);
			}

			private void removeButtonFor(RPanelHost host, RPanel panel) {
				JToggleButton button = buttons.remove(panel);
				removeCallback.accept(button);
			}

			@Override
			public void onAttach(RPanelHost host, RPanel panel) {
				if (panel.isVisible()) {
					addButtonFor(host, panel);
				}

				ui.validate();
				ui.repaint();
			}

			@Override
			public void onDetach(RPanelHost host, RPanel panel) {
				removeButtonFor(host, panel);

				ui.validate();
				ui.repaint();
			}

			@Override
			public void onActivate(RPanelHost host, RPanel panel) {
				try {
					updatingButtons[0] = true;
					JToggleButton button = buttons.get(panel);
					if (button != null) {
						button.setSelected(true);
					}
				} finally {
					updatingButtons[0] = false;
				}
			}

			@Override
			public void onMinimize(RPanelHost host, RPanel panel) {
				try {
					updatingButtons[0] = true;
					JToggleButton button = buttons.get(panel);
					if (button != null) {
						button.setSelected(false);
					}
				} finally {
					updatingButtons[0] = false;
				}
			}

			@Override
			public void onVisibleStateChange(RPanelHost host, RPanel panel) {
				try {
					updatingButtons[0] = true;
					if (panel.isVisible()) {
						addButtonFor(host, panel);
					} else {
						removeButtonFor(host, panel);
					}
				} finally {
					updatingButtons[0] = false;
				}
			}

			@Override
			public void onTitleChange(RPanelHost host, RPanel panel) {
				JToggleButton button = buttons.get(panel);

				if (button != null) {
					button.setText(panel.getTitle());
					ui.validate();
					ui.repaint();
				}
			}
		});
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
