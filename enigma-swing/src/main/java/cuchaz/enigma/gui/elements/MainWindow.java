package cuchaz.enigma.gui.elements;

import java.awt.*;
import java.util.OptionalInt;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import cuchaz.enigma.config.ConfigSection;
import cuchaz.enigma.gui.elements.rpanel.RPanel;
import cuchaz.enigma.gui.elements.rpanel.RPanelGroup;
import cuchaz.enigma.gui.elements.rpanel.WorkspaceRPanelContainer;
import cuchaz.enigma.gui.elements.rpanel.WorkspaceRPanelContainer.DockPosition;

public class MainWindow {
	private final JFrame frame;
	private final WorkspaceRPanelContainer workspace = new WorkspaceRPanelContainer();
	private final RPanelGroup panelGroup = new RPanelGroup();

	private final JMenuBar menuBar = new JMenuBar();
	private final StatusBar statusBar = new StatusBar();

	public MainWindow(String title) {
		this.frame = new JFrame(title);
		this.frame.setJMenuBar(this.menuBar);

		Container contentPane = this.frame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(this.workspace.getUi(), BorderLayout.CENTER);
		contentPane.add(this.statusBar.getUi(), BorderLayout.SOUTH);

		this.workspace.addToGroup(this.panelGroup);
	}

	public void setVisible(boolean visible) {
		this.frame.setVisible(visible);
	}

	/**
	 * Saves the state (window geometry and RPanel positions) of this main
	 * window.
	 *
	 * @param section the configuration section to write to
	 */
	public void saveState(ConfigSection section) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Point location = this.frame.getLocationOnScreen();
		Dimension dim = this.frame.getSize();

		section.setInt("X %s".formatted(screenSize.width), location.x);
		section.setInt("Y %d".formatted(screenSize.height), location.y);
		section.setInt("Width %d".formatted(screenSize.width), dim.width);
		section.setInt("Height %d".formatted(screenSize.height), dim.height);

		this.panelGroup.saveState(section.section("Dock Panels"));
	}

	public void restoreState(ConfigSection section) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		OptionalInt width = section.getInt("Width %d".formatted(screenSize.width));
		OptionalInt height = section.getInt("Height %d".formatted(screenSize.height));
		OptionalInt x = section.getInt("X %d".formatted(screenSize.width));
		OptionalInt y = section.getInt("Y %d".formatted(screenSize.height));

		if (width.isPresent() && height.isPresent()) {
			this.frame.setSize(new Dimension(width.getAsInt(), height.getAsInt()));
		}

		if (x.isPresent() && y.isPresent()) {
			int ix = x.getAsInt();
			int iy = y.getAsInt();

			// Ensure that the position is on the screen.
			if (ix >= 0 && iy >= 0 && ix <= screenSize.width && iy <= screenSize.height) {
				this.frame.setLocation(ix, iy);
			}
		}

		this.panelGroup.restoreState(section.section("Dock Panels"));
	}

	public WorkspaceRPanelContainer workspace() {
		return this.workspace;
	}

	public void addPanel(DockPosition dp, RPanel panel) {
		this.panelGroup.addPanel(panel);
		this.workspace.get(dp).attach(panel);
	}

	public JMenuBar menuBar() {
		return this.menuBar;
	}

	public StatusBar statusBar() {
		return this.statusBar;
	}

	public Container workArea() {
		return this.workspace.getWorkArea();
	}

	public JFrame frame() {
		return this.frame;
	}

	public void setTitle(String title) {
		this.frame.setTitle(title);
	}
}
