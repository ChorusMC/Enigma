package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.Comparator;

import javax.swing.JScrollPane;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.rpanel.RPanel;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.I18n;

public class ObfPanel {

	private final RPanel panel = new RPanel();

	public final ClassSelector obfClasses;

	private final Gui gui;

	public ObfPanel(Gui gui) {
		this.gui = gui;
		Container contentPane = panel.getContentPane();

		Comparator<ClassEntry> obfClassComparator = (a, b) -> {
			String aname = a.getFullName();
			String bname = b.getFullName();
			if (aname.length() != bname.length()) {
				return aname.length() - bname.length();
			}
			return aname.compareTo(bname);
		};

		this.obfClasses = new ClassSelector(gui, obfClassComparator, false);
		this.obfClasses.setSelectionListener(gui.getController()::navigateTo);
		this.obfClasses.setRenameSelectionListener(gui::onRenameFromClassTree);

		contentPane.setLayout(new BorderLayout());
		contentPane.add(new JScrollPane(this.obfClasses), BorderLayout.CENTER);

		this.retranslateUi();
	}

	public void retranslateUi() {
		this.panel.setTitle(I18n.translate("info_panel.classes.obfuscated"));
	}

	public RPanel getPanel() {
		return panel;
	}

}
