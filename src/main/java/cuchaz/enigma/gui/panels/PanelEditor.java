package cuchaz.enigma.gui.panels;

import cuchaz.enigma.config.Config;
import cuchaz.enigma.gui.BrowserCaret;
import cuchaz.enigma.gui.Gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PanelEditor extends JEditorPane {
	public PanelEditor(Gui gui) {
		this.setEditable(false);
		this.setSelectionColor(new Color(31, 46, 90));
		this.setCaret(new BrowserCaret());
		this.addCaretListener(event -> gui.onCaretMove(event.getDot()));
		final PanelEditor self = this;
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3)
					self.setCaretPosition(self.viewToModel(e.getPoint()));
			}
		});
		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.isControlDown()) {
					switch (event.getKeyCode()) {
						case KeyEvent.VK_R:
							gui.popupMenu.renameMenu.doClick();
							break;

						case KeyEvent.VK_I:
							gui.popupMenu.showInheritanceMenu.doClick();
							break;

						case KeyEvent.VK_M:
							gui.popupMenu.showImplementationsMenu.doClick();
							break;

						case KeyEvent.VK_N:
							gui.popupMenu.openEntryMenu.doClick();
							break;

						case KeyEvent.VK_P:
							gui.popupMenu.openPreviousMenu.doClick();
							break;

						case KeyEvent.VK_C:
							gui.popupMenu.showCallsMenu.doClick();
							break;

						case KeyEvent.VK_O:
							gui.popupMenu.toggleMappingMenu.doClick();
							break;
						case KeyEvent.VK_F5:
							gui.getController().refreshCurrentClass();
							break;
						default:
							break;
					}
				}

				gui.setShouldNavigateOnClick(event.isControlDown());
			}

			@Override
			public void keyTyped(KeyEvent event) {
				if (!gui.popupMenu.isEnabled()) return;
				if (!event.isControlDown() && !event.isAltDown()) {
					gui.popupMenu.renameMenu.doClick();
					gui.renameTextField.setText(String.valueOf(event.getKeyChar()));
				}
			}

			@Override
			public void keyReleased(KeyEvent event) {
				gui.setShouldNavigateOnClick(event.isControlDown());
			}
		});
	}

	@Override
	public Color getCaretColor() {
		return new Color(Config.getInstance().caretColor);
	}
}
