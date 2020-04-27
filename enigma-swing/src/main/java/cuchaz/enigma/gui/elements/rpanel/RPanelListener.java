package cuchaz.enigma.gui.elements.rpanel;

public interface RPanelListener {

	default void onAttach(RPanelHost host, RPanel panel) {
	}

	default void onDetach(RPanelHost host, RPanel panel) {
	}

	default void onActivate(RPanelHost host, RPanel panel) {
	}

	default void onHide(RPanelHost host, RPanel panel) {
	}

}
