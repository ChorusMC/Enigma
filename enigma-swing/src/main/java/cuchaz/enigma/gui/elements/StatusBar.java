package cuchaz.enigma.gui.elements;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Implements a generic status bar for use in windows. The API is loosely based
 * on Qt's QStatusBar.
 */
public class StatusBar {
	private final JPanel ui = new JPanel(new BorderLayout());
	private final JPanel leftPanel = new JPanel(new GridLayout(1, 1, 0, 0));
	private final JPanel components = new JPanel();
	private final JPanel permanentComponents = new JPanel();

	private final JLabel temporaryMessage = new JLabel();
	private final Timer timer = new Timer(0, e -> this.clearMessage());

	public StatusBar() {
		this.timer.setRepeats(false);

		this.components.setLayout(new BoxLayout(this.components, BoxLayout.LINE_AXIS));
		this.permanentComponents.setLayout(new BoxLayout(this.permanentComponents, BoxLayout.LINE_AXIS));
		this.permanentComponents.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

		this.leftPanel.add(this.components);
		this.temporaryMessage.setHorizontalTextPosition(JLabel.LEFT);
		this.ui.add(this.leftPanel, BorderLayout.CENTER);
		this.ui.add(this.permanentComponents, BorderLayout.EAST);
	}

	/**
	 * Displays a temporary message in the status bar. The message is displayed
	 * until it is explicitly cleared through {@link #clearMessage()} or a new
	 * message is displayed.
	 *
	 * @param message the message to display
	 */
	public void showMessage(String message) {
		this.showMessage(message, 0);
	}

	/**
	 * Displays a temporary message in the status bar. The message is displayed
	 * until it is explicitly cleared through {@link #clearMessage()}, a new
	 * message is displayed, or the timeout, if any, is reached.
	 *
	 * @param message the message to display
	 * @param timeout the timeout in milliseconds to wait until clearing the
	 *                message; if 0, the message is not automatically cleared
	 */
	public void showMessage(String message, int timeout) {
		this.timer.stop();

		this.temporaryMessage.setText(message);
		this.leftPanel.removeAll();
		this.leftPanel.add(this.temporaryMessage);
		this.leftPanel.revalidate();

		if (timeout > 0) {
			this.timer.setInitialDelay(timeout);
			this.timer.start();
		}
	}

	/**
	 * Clears any currently displayed temporary message.
	 */
	public void clearMessage() {
		this.timer.stop();

		this.leftPanel.removeAll();
		this.leftPanel.add(this.components);
		this.leftPanel.revalidate();
		this.temporaryMessage.setText("");
	}

	/**
	 * Returns the currently displayed message, or the empty string otherwise.
	 *
	 * @return the currently displayed message
	 */
	public String currentMessage() {
		return this.temporaryMessage.getText();
	}

	/**
	 * Adds a component to the status bar. These components are positioned on
	 * the left side of the status bar. When a temporary message is displayed,
	 * the component will be hidden until the message is cleared.
	 *
	 * @param comp the component to add
	 */
	public void addComponent(Component comp) {
		this.components.add(comp);
	}

	/**
	 * Removes a component from the status bar.
	 *
	 * @param comp the component to remove
	 */
	public void removeComponent(Component comp) {
		this.components.remove(comp);
	}

	/**
	 * Adds a permanent component to the status bar. These components will not
	 * be hidden by temporary messages and will be displayed on the right side
	 * of the status bar.
	 *
	 * @param comp the component to add
	 */
	public void addPermanentComponent(Component comp) {
		this.permanentComponents.add(comp);
	}

	/**
	 * Removes a permanent component from the status bar.
	 *
	 * @param comp the component to remove
	 */
	public void removePermanentComponent(Component comp) {
		this.permanentComponents.remove(comp);
	}

	public JPanel getUi() {
		return this.ui;
	}
}
