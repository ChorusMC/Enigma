package cuchaz.enigma.gui.panels;

import java.awt.*;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import cuchaz.enigma.analysis.StructureTreeNode;
import cuchaz.enigma.analysis.StructureTreeOptions;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.rpanel.RPanel;
import cuchaz.enigma.gui.renderer.StructureOptionListCellRenderer;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.SingleTreeSelectionModel;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;
import cuchaz.enigma.utils.I18n;

public class StructurePanel {
    private final Gui gui;

    private final RPanel panel = new RPanel("Structure");

    private final JPanel optionsPanel;

    private final JLabel obfuscationVisibilityLabel = new JLabel();
    private final JLabel documentationVisibilityLabel = new JLabel();
    private final JLabel sortingOrderLabel = new JLabel();

    private final JComboBox<StructureTreeOptions.ObfuscationVisibility> obfuscationVisibility;
    private final JComboBox<StructureTreeOptions.DocumentationVisibility> documentationVisibility;
    private final JComboBox<StructureTreeOptions.SortingOrder> sortingOrder;

    private final JTree structureTree;

    public StructurePanel(Gui gui) {
        this.gui = gui;

        this.optionsPanel = new JPanel(new GridBagLayout());
        this.optionsPanel.setVisible(false);

        GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(5).fill(GridBagConstraints.HORIZONTAL);

        this.optionsPanel.add(this.obfuscationVisibilityLabel, cb.pos(0, 0).build());
        this.obfuscationVisibility = new JComboBox<>(StructureTreeOptions.ObfuscationVisibility.values());
        this.obfuscationVisibility.setRenderer(new StructureOptionListCellRenderer());
        this.obfuscationVisibility.addActionListener(event -> this.showStructure(gui.getActiveEditor()));
        this.optionsPanel.add(this.obfuscationVisibility, cb.pos(1, 0).build());

        this.optionsPanel.add(this.documentationVisibilityLabel, cb.pos(0, 1).build());
        this.documentationVisibility = new JComboBox<>(StructureTreeOptions.DocumentationVisibility.values());
        this.documentationVisibility.setRenderer(new StructureOptionListCellRenderer());
        this.documentationVisibility.addActionListener(event -> this.showStructure(gui.getActiveEditor()));
        this.optionsPanel.add(this.documentationVisibility, cb.pos(1, 1).build());

        this.optionsPanel.add(this.sortingOrderLabel, cb.pos(0, 2).build());
        this.sortingOrder = new JComboBox<>(StructureTreeOptions.SortingOrder.values());
        this.sortingOrder.setRenderer(new StructureOptionListCellRenderer());
        this.sortingOrder.addActionListener(event -> this.showStructure(gui.getActiveEditor()));
        this.optionsPanel.add(this.sortingOrder, cb.pos(1, 2).build());

        this.structureTree = new JTree();
        this.structureTree.setModel(null);
        this.structureTree.setCellRenderer(new StructureTreeCellRenderer(gui));
        this.structureTree.setSelectionModel(new SingleTreeSelectionModel());
        this.structureTree.setShowsRootHandles(true);
        this.structureTree.addMouseListener(GuiUtil.onMouseClick(this::onClick));

        this.retranslateUi();

        Container contentPane = this.panel.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(this.optionsPanel, BorderLayout.NORTH);
        contentPane.add(new JScrollPane(this.structureTree));
    }

    public void showStructure(EditorPanel editor) {
        structureTree.setModel(null);

        if (editor == null) {
            this.optionsPanel.setVisible(false);
            return;
        }

        ClassEntry classEntry = editor.getClassHandle().getRef();
        if (classEntry == null) return;

        this.optionsPanel.setVisible(true);

        // get the class structure
        StructureTreeNode node = this.gui.getController().getClassStructure(classEntry, this.getOptions());

        // show the tree at the root
        TreePath path = GuiUtil.getPathToRoot(node);
        structureTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
        structureTree.expandPath(path);
        structureTree.setSelectionRow(structureTree.getRowForPath(path));
    }

    private void onClick(MouseEvent event) {
        if (event.getClickCount() >= 2 && event.getButton() == MouseEvent.BUTTON1) {
            // get the selected node
            TreePath path = structureTree.getSelectionPath();
            if (path == null) {
                return;
            }

            Object node = path.getLastPathComponent();

            if (node instanceof StructureTreeNode) {
                this.gui.getController().navigateTo(((StructureTreeNode) node).getEntry());
            }
        }
    }

    /**
     * Creates and returns the options of this structure panel.
     */
    private StructureTreeOptions getOptions() {
        return new StructureTreeOptions(
                (StructureTreeOptions.ObfuscationVisibility) this.obfuscationVisibility.getSelectedItem(),
                (StructureTreeOptions.DocumentationVisibility) this.documentationVisibility.getSelectedItem(),
                (StructureTreeOptions.SortingOrder) this.sortingOrder.getSelectedItem()
        );
    }

    public void retranslateUi() {
        this.panel.setTitle(I18n.translate("info_panel.tree.structure"));
        this.obfuscationVisibilityLabel.setText(I18n.translate("structure.options.obfuscation"));
        this.documentationVisibilityLabel.setText(I18n.translate("structure.options.documentation"));
        this.sortingOrderLabel.setText(I18n.translate("structure.options.sorting"));
    }

    public RPanel getPanel() {
        return this.panel;
    }

    private static class StructureTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Gui gui;

        StructureTreeCellRenderer(Gui gui) {
            this.gui = gui;
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            ParentedEntry<?> entry = ((StructureTreeNode) value).getEntry();

            if (entry instanceof ClassEntry classEntry) {
                this.setIcon(GuiUtil.getClassIcon(gui, classEntry));
            } else if (entry instanceof MethodEntry methodEntry) {
                this.setIcon(GuiUtil.getMethodIcon(methodEntry));
            } else if (entry instanceof FieldEntry) {
                this.setIcon(GuiUtil.FIELD_ICON);
            }

            this.setText("<html>" + ((StructureTreeNode) value).toHtml());

            return c;
        }
    }
}
