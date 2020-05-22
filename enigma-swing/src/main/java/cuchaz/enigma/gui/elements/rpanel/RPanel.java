package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Container;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;

public class RPanel {

	private Container contentPane;
	private String title;

	private RPanelHost host;
	private Set<RPanelHost> dndContainers = new HashSet<>();

	private boolean visible = true;

	public RPanel() {
		this("");
	}

	public RPanel(String title) {
		this.contentPane = new JPanel();
		this.title = title;
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

	public void setOwner(RPanelHost host) {
		if (host != null && !host.owns(this)) throw new IllegalStateException(String.format("Received request to set owner of panel to %s, but that says it doesn't own this panel!", host));

		if (this.host != null) {
			this.host.detach(this);
		}

		this.host = host;
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
		this.host.titleChanged(this);
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void addDragTarget(RPanelHost host) {
		dndContainers.add(host);
	}

	public void removeDragTarget(RPanelHost host) {
		dndContainers.remove(host);
	}

	public Set<RPanelHost> getDragTargets() {
		return Collections.unmodifiableSet(dndContainers);
	}

}
