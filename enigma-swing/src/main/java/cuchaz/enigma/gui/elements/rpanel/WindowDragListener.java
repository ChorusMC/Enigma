package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Window;

public interface WindowDragListener {

	default void onStartDrag(Window w) {
	}

	default void onStopDrag(Window w) {
	}

	default void onDragMove(Window w) {
	}

}
