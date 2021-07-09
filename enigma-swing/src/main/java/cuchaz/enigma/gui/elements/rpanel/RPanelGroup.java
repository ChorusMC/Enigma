package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cuchaz.enigma.config.ConfigSection;

public class RPanelGroup {
	private final Set<RPanel> panels = new HashSet<>();
	private final Set<RPanel> panelsView = Collections.unmodifiableSet(this.panels);
	private final Set<RPanelHost> hosts = new HashSet<>();
	private final Set<RPanelHost> hostsView = Collections.unmodifiableSet(this.hosts);

	public void addPanel(RPanel panel) {
		if (!this.contains(panel)) {
			this.panels.add(panel);
			panel.setGroup(this);
		}
	}

	public void removePanel(RPanel panel) {
		if (this.panels.remove(panel)) {
			panel.setGroup(null);
		}
	}

	public boolean contains(RPanel panel) {
		return this.panels.contains(panel);
	}

	public boolean containsHost(RPanelHost host) {
		return this.hosts.contains(host);
	}

	public void addHost(RPanelHost host) {
		this.hosts.add(host);
	}

	public void removeHost(RPanelHost host) {
		this.hosts.remove(host);
	}

	public void saveState(ConfigSection cs) {
		for (RPanel panel : this.panels) {
			var id = panel.getId();

			if (id == null) {
				System.err.printf("WARNING: Trying to save panel group state but panel with title '%s' in the group has no ID! Fix your shit\n", panel.getTitle());
				continue;
			}

			RPanelHost host = panel.getHost();

			if (host == null || !this.containsHost(host)) {
				continue;
			}

			// clear previous saved state if present
			cs.removeSection(id);
			var section = cs.section(id);

			section.setBool("Window", host.isDedicatedHost());

			Rectangle panelLocation = host.getPanelLocation(panel);

			if (host.isDedicatedHost()) {
				section.setIntArray("Position", new int[] { panelLocation.x, panelLocation.y });
			} else {
				section.setString("Attached To", host.getId());
			}

			section.setIntArray("Size", new int[] { panelLocation.width, panelLocation.height });
			section.setBool("Visible", panel.isVisible());
		}
	}

	public void restoreState(ConfigSection cs) {

	}

	public Set<RPanel> panels() {
		return this.panelsView;
	}

	public Set<RPanelHost> hosts() {
		return this.hostsView;
	}
}
