package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.rpanel.RPanel;
import cuchaz.enigma.utils.I18n;

public class DeobfPanel {

	private final RPanel panel;

	public final ClassSelector deobfClasses;

	private final Gui gui;

	public DeobfPanel(Gui gui) {
		this.gui = gui;
		this.panel = new RPanel(I18n.translate("info_panel.classes.deobfuscated"));
		JPanel contentPane = panel.getContentPane();

		this.deobfClasses = new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR, true);
		this.deobfClasses.setSelectionListener(gui.getController()::navigateTo);
		this.deobfClasses.setRenameSelectionListener(gui::onRenameFromClassTree);

		contentPane.setLayout(new BorderLayout());
		contentPane.add(new JScrollPane(this.deobfClasses), BorderLayout.CENTER);

		this.retranslateUi();
	}

	public void retranslateUi() {
		this.panel.setTitle(I18n.translate(gui.isSingleClassTree() ? "info_panel.classes" : "info_panel.classes.deobfuscated"));
	}

	public RPanel getPanel() {
		return panel;
	}

}
