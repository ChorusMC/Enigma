package cuchaz.enigma.gui.elements;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import cuchaz.enigma.analysis.ClassImplementationsTreeNode;
import cuchaz.enigma.analysis.MethodImplementationsTreeNode;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.rpanel.RPanel;
import cuchaz.enigma.gui.renderer.ImplementationsTreeCellRenderer;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.MouseListenerUtil;
import cuchaz.enigma.gui.util.SingleTreeSelectionModel;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;

public class ImplementationsTree {
	private final RPanel panel = new RPanel();

	private final JTree tree = new JTree();

	private final Gui gui;

	public ImplementationsTree(Gui gui) {
		this.gui = gui;

		this.tree.setModel(null);
		this.tree.setCellRenderer(new ImplementationsTreeCellRenderer(gui));
		this.tree.setSelectionModel(new SingleTreeSelectionModel());
		this.tree.setShowsRootHandles(true);
		this.tree.addMouseListener(MouseListenerUtil.onClick(this::onClick));

		this.panel.getContentPane().setLayout(new BorderLayout());
		this.panel.getContentPane().add(new JScrollPane(this.tree));
	}

	private void onClick(MouseEvent event) {
		if (event.getClickCount() >= 2 && event.getButton() == MouseEvent.BUTTON1) {
			// get the selected node
			TreePath path = this.tree.getSelectionPath();
			if (path == null) {
				return;
			}

			Object node = path.getLastPathComponent();
			if (node instanceof ClassImplementationsTreeNode classNode) {
				this.gui.getController().navigateTo(classNode.getClassEntry());
			} else if (node instanceof MethodImplementationsTreeNode methodNode) {
				this.gui.getController().navigateTo(methodNode.getMethodEntry());
			}
		}
	}

	public void display(Entry<?> entry) {
		this.tree.setModel(null);

		DefaultMutableTreeNode node = null;

		if (entry instanceof ClassEntry classEntry) {
			node = this.gui.getController().getClassImplementations(classEntry);
		} else if (entry instanceof MethodEntry methodEntry) {
			node = this.gui.getController().getMethodImplementations(methodEntry);
		}

		if (node != null) {
			// show the tree at the root
			TreePath path = GuiUtil.getPathToRoot(node);
			this.tree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
			this.tree.expandPath(path);
			this.tree.setSelectionRow(this.tree.getRowForPath(path));
		}

		this.panel.show();
	}

	public void retranslateUi() {
		this.panel.setTitle(I18n.translate("info_panel.tree.implementations"));
	}

	public RPanel getPanel() {
		return this.panel;
	}
}
