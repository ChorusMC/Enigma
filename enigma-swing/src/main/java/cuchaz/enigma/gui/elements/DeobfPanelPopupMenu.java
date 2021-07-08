package cuchaz.enigma.gui.elements;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.panels.DeobfPanel;
import cuchaz.enigma.utils.I18n;

public class DeobfPanelPopupMenu {

    private final JPopupMenu ui;
    private final JMenuItem renamePackage = new JMenuItem();
    private final JMenuItem renameClass = new JMenuItem();
    private final JMenuItem expandAll = new JMenuItem();
    private final JMenuItem collapseAll = new JMenuItem();

    public DeobfPanelPopupMenu(DeobfPanel panel) {
        this.ui = new JPopupMenu();

        this.ui.add(this.renamePackage);
        this.ui.add(this.renameClass);
        this.ui.addSeparator();
        this.ui.add(this.expandAll);
        this.ui.add(this.collapseAll);

        ClassSelector deobfClasses = panel.deobfClasses;

        this.renamePackage.addActionListener(a -> {
            TreePath path;

            if (deobfClasses.getSelectedClass() != null) {
                // Rename parent package if selected path is a class
                path = deobfClasses.getSelectionPath().getParentPath();
            } else {
                // Rename selected path if it's already a package
                path = deobfClasses.getSelectionPath();
            }

            deobfClasses.getUI().startEditingAtPath(deobfClasses, path);
        });
        this.renameClass.addActionListener(a -> deobfClasses.getUI().startEditingAtPath(deobfClasses, deobfClasses.getSelectionPath()));
        this.expandAll.addActionListener(a -> deobfClasses.expandAll());
        this.collapseAll.addActionListener(a -> deobfClasses.collapseAll());

        this.retranslateUi();
    }

    public void show(ClassSelector deobfClasses, int x, int y) {
        // Only enable rename class if selected path is a class
        this.renameClass.setEnabled(deobfClasses.getSelectedClass() != null);

        this.ui.show(deobfClasses, x, y);
    }

    public void retranslateUi() {
        this.renamePackage.setText(I18n.translate("popup_menu.deobf_panel.rename_package"));
        this.renameClass.setText(I18n.translate("popup_menu.deobf_panel.rename_class"));
        this.expandAll.setText(I18n.translate("popup_menu.deobf_panel.expand_all"));
        this.collapseAll.setText(I18n.translate("popup_menu.deobf_panel.collapse_all"));
    }
}
