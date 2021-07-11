package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Container;
import java.awt.Rectangle;

import javax.annotation.Nullable;
import javax.swing.JPanel;

public class RPanel {
	private final String id;
	private Container contentPane = new JPanel();
	private String title;

	private RPanelHost host;
	private RPanelGroup group;

	private boolean visible = true;

	public RPanel() {
		this(null);
	}

	public RPanel(String id) {
		this.id = id;
	}

	public Container getContentPane() {
		return contentPane;
	}

	public void setContentPane(Container contentPane) {
		this.contentPane = contentPane;

		RPanelHost host = this.host;
		if (host != null) {
			System.err.println("WARNING: Set RPanel's content pane while it was attached. This is currently buggy!");
			detach();
			host.attach(this);
		}
	}

	public void windowize() {
		if (host != null && host.isDedicatedHost()) return;

		RPanelWindow window = new RPanelWindow();
		attach(window);
	}

	public void show() {
		if (host == null) {
			windowize();
		} else {
			host.activate(this);
		}
	}

	public void detach() {
		if (this.host != null) {
			this.host.detach(this);
			this.host = null;
		}
	}

	public void attach(RPanelHost host) {
		if (host == null) {
			detach();
			return;
		}

		Rectangle currentPos = null;

		if (this.host != null) {
			currentPos = this.host.getPanelLocation(this);
		}

		host.attach(this);

		if (currentPos != null) host.tryMoveTo(this, currentPos);
	}

	public void setOwner(@Nullable RPanelHost host) {
		if (host != null && !host.owns(this)) throw new IllegalStateException(String.format("Received request to set owner of panel to %s, but that says it doesn't own this panel!", host));

		if (this.host != null) {
			this.host.detach(this);
		}

		this.host = host;
	}

	@Nullable
	public RPanelHost getHost() {
		return this.host;
	}

	public void setGroup(@Nullable RPanelGroup group) {
		if (group != null && !group.contains(this)) {
			throw new IllegalStateException("Received request to set group of panel to %s, but that says it doesn't contain this panel!".formatted(group));
		}

		if (this.group != null) {
			this.group.removePanel(this);
		}

		this.group = group;
	}

	public void setVisible(boolean visible) {
		if (this.visible != visible) {
			this.visible = visible;
			if (host != null) {
				host.updateVisibleState(this);
			}
		}
	}

	public boolean isVisible() {
		return visible;
	}

	public void setTitle(String title) {
		this.title = title;

		if (this.host != null) {
			this.host.titleChanged(this);
		}
	}

	public String getTitle() {
		return title;
	}

	@Nullable
	public String getId() {
		return this.id;
	}

	public RPanelGroup getGroup() {
		return this.group;
	}
}
