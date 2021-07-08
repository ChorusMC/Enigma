package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.DeobfPanelPopupMenu;
import cuchaz.enigma.gui.elements.rpanel.RPanel;
import cuchaz.enigma.gui.util.MouseListenerUtil;
import cuchaz.enigma.utils.I18n;

public class DeobfPanel {

	private final RPanel panel = new RPanel();

	public final ClassSelector deobfClasses;

	public final DeobfPanelPopupMenu deobfPanelPopupMenu;

	private final Gui gui;

	public DeobfPanel(Gui gui) {
		this.gui = gui;
		this.deobfPanelPopupMenu = new DeobfPanelPopupMenu(gui);

		Container contentPane = panel.getContentPane();

		this.deobfClasses = new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR, true);
		this.deobfClasses.setSelectionListener(gui.getController()::navigateTo);
		this.deobfClasses.setRenameSelectionListener(gui::onRenameFromClassTree);

		contentPane.setLayout(new BorderLayout());
		contentPane.add(new JScrollPane(this.deobfClasses), BorderLayout.CENTER);

		this.deobfClasses.addMouseListener(MouseListenerUtil.onPress(this::onPress));

		this.retranslateUi();
	}

	private void onPress(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			deobfClasses.setSelectionRow(deobfClasses.getClosestRowForLocation(e.getX(), e.getY()));
			int i = deobfClasses.getRowForPath(deobfClasses.getSelectionPath());
			if (i != -1) {
				deobfPanelPopupMenu.show(deobfClasses, e.getX(), e.getY());
			}
		}
	}

	public void retranslateUi() {
		this.panel.setTitle(I18n.translate(gui.isSingleClassTree() ? "info_panel.classes" : "info_panel.classes.deobfuscated"));
		this.deobfPanelPopupMenu.retranslateUi();
	}

	public RPanel getPanel() {
		return panel;
	}

}
