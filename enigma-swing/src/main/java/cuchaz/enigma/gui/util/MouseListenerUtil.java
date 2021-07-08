package cuchaz.enigma.gui.util;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

public final class MouseListenerUtil {
	private MouseListenerUtil() {
	}

	public static MouseListener onClick(Consumer<MouseEvent> op) {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				op.accept(e);
			}
		};
	}

	public static MouseListener onPress(Consumer<MouseEvent> op) {
		return new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				op.accept(e);
			}
		};
	}
}
