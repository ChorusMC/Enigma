package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Container;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JRootPane;

// This entire class is pretty much a major hack around JRootPane to get its
// title bar to not delegate to the outer window, but instead use custom
// information. It uses a dummy window that tracks the desired state, and
// returns that as parent element if addNotify is called, where (at least in
// the look and feels I've tested) the RootPaneUI sets the window to use for
// determining the window decoration state.
public class StandaloneRootPane extends JRootPane {

	private JFrame dummy;
	private boolean inAddNotify;
	private String title;

	private final List<Runnable> closeListeners = new ArrayList<>();

	public StandaloneRootPane() {
		dummy = new DummyFrame(this);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
		dummy.setTitle(title);
	}

	public void addCloseListener(Runnable r) {
		closeListeners.add(r);
	}

	@Override
	public void addNotify() {
		try {
			inAddNotify = true;
			super.addNotify();
		} finally {
			inAddNotify = false;
		}
	}

	@Override
	public Container getParent() {
		if (inAddNotify) return dummy;

		return getActualParent();
	}

	public Container getActualParent() {
		return super.getParent();
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);

		dummy.setBounds(x, y, width, height);
	}

	private static class DummyFrame extends JFrame {

		private final StandaloneRootPane owner;

		private DummyFrame(StandaloneRootPane owner) {
			this.owner = owner;

			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					owner.closeListeners.forEach(Runnable::run);
				}
			});
		}

		public Window getActualWindow() {
			Container parent = owner.getActualParent();
			while (parent != null) {
				if (parent instanceof Window) {
					return (Window) parent;
				}
				parent = parent.getParent();
			}
			return null;
		}

		@Override
		public boolean isActive() {
			// TODO make a change in this cause a redraw
			return getActualWindow().isActive();
		}

		@Override
		public boolean isFocused() {
			// TODO make a change in this cause a redraw
			return getActualWindow().isFocused();
		}

	}

}
