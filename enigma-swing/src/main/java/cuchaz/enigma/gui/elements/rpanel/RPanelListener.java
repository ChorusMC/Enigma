package cuchaz.enigma.gui.elements.rpanel;

public interface RPanelListener {

	default void onAttach(RPanelHost host, RPanel panel) {
	}

	default void onDetach(RPanelHost host, RPanel panel) {
	}

	default void onActivate(RPanelHost host, RPanel panel) {
	}

	default void onMinimize(RPanelHost host, RPanel panel) {
	}

	default void onVisibleStateChange(RPanelHost host, RPanel panel) {
	}

	default void onTitleChange(RPanelHost host, RPanel panel) {
	}

}
