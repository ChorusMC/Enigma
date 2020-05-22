package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Rectangle;

import javax.annotation.Nullable;

public interface RPanelHost {

	void attach(RPanel panel);

	void detach(RPanel panel);

	boolean owns(RPanel panel);

	void titleChanged(RPanel panel);

	@Nullable
	RPanel getActivePanel();

	void activate(RPanel panel);

	void minimize(RPanel panel);

	void addRPanelListener(RPanelListener listener);

	void removeRPanelListener(RPanelListener listener);

	void updateVisibleState(RPanel panel);

	Rectangle getPanelLocation(RPanel panel);

	void tryMoveTo(RPanel panel, Rectangle rect);

	Rectangle getDragInsertBounds();

	boolean onDragOver(RPanel panel);

	boolean isDedicatedHost();

}
