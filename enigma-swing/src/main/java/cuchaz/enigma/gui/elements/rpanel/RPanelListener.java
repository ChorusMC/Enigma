package cuchaz.enigma.gui.elements.rpanel;

import java.util.function.BiConsumer;

public interface RPanelListener {

	default void onAttach(RPanelHost host, RPanel panel) {

	}

	default void onDetach(RPanelHost host, RPanel panel) {

	}

	default void onActivate(RPanelHost host, RPanel panel) {

	}

	default void onHide(RPanelHost host, RPanel panel) {

	}

	static RPanelListener forAttach(BiConsumer<RPanelHost, RPanel> op) {
		return new RPanelListener() {
			@Override
			public void onAttach(RPanelHost host, RPanel panel) {
				op.accept(host, panel);
			}
		};
	}

	static  RPanelListener forDetach(BiConsumer<RPanelHost, RPanel> op) {
		return new RPanelListener() {
			@Override
			public void onDetach(RPanelHost host, RPanel panel) {
				op.accept(host, panel);
			}
		};
	}

	static  RPanelListener forActivate(BiConsumer<RPanelHost, RPanel> op) {
		return new RPanelListener() {
			@Override
			public void onActivate(RPanelHost host, RPanel panel) {
				op.accept(host, panel);
			}
		};
	}

	static  RPanelListener forHide(BiConsumer<RPanelHost, RPanel> op) {
		return new RPanelListener() {
			@Override
			public void onHide(RPanelHost host, RPanel panel) {
				op.accept(host, panel);
			}
		};
	}

}
