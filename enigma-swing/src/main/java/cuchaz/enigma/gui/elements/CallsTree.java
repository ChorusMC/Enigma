package cuchaz.enigma.gui.elements;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Vector;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import cuchaz.enigma.analysis.ReferenceTreeNode;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.TokenListCellRenderer;
import cuchaz.enigma.gui.elements.rpanel.RPanel;
import cuchaz.enigma.gui.renderer.CallsTreeCellRenderer;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.gui.util.SingleTreeSelectionModel;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;

public class CallsTree {
	private final RPanel panel = new RPanel();

	private final JTree callsTree = new JTree();
	private final JList<Token> tokens = new JList<>();

	private final Gui gui;

	public CallsTree(Gui gui) {
		this.gui = gui;

		this.callsTree.setModel(null);
		this.callsTree.setCellRenderer(new CallsTreeCellRenderer(gui));
		this.callsTree.setSelectionModel(new SingleTreeSelectionModel());
		this.callsTree.setShowsRootHandles(true);
		this.callsTree.addMouseListener(GuiUtil.onMouseClick(this::onTreeClicked));

		this.tokens.setCellRenderer(new TokenListCellRenderer(gui.getController()));
		this.tokens.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.tokens.setLayoutOrientation(JList.VERTICAL);
		this.tokens.addMouseListener(GuiUtil.onMouseClick(this::onTokenClicked));
		this.tokens.setPreferredSize(ScaleUtil.getDimension(0, 200));
		this.tokens.setMinimumSize(ScaleUtil.getDimension(0, 200));

		JSplitPane contentPane = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT,
				true,
				new JScrollPane(this.callsTree),
				new JScrollPane(this.tokens)
		);

		contentPane.setResizeWeight(1); // let the top side take all the slack
		contentPane.resetToPreferredSizes();
		this.panel.setContentPane(contentPane);
	}

	public void showCalls(Entry<?> entry, boolean recurse) {
		TreeNode node = null;

		if (entry instanceof ClassEntry classEntry) {
			node = this.gui.getController().getClassReferences(classEntry);
		} else if (entry instanceof FieldEntry fieldEntry) {
			node = this.gui.getController().getFieldReferences(fieldEntry);
		} else if (entry instanceof MethodEntry methodEntry) {
			node = this.gui.getController().getMethodReferences(methodEntry, recurse);
		}

		this.callsTree.setModel(new DefaultTreeModel(node));

		this.panel.show();
	}

	public void showTokens(Collection<Token> tokens) {
		this.tokens.setListData(new Vector<>(tokens));
		this.tokens.setSelectedIndex(0);
	}

	public void clearTokens() {
		this.tokens.setListData(new Vector<>());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void onTreeClicked(MouseEvent event) {
		if (event.getClickCount() >= 2 && event.getButton() == MouseEvent.BUTTON1) {
			// get the selected node
			TreePath path = this.callsTree.getSelectionPath();

			if (path == null) {
				return;
			}

			Object node = path.getLastPathComponent();

			if (node instanceof ReferenceTreeNode referenceNode) {
				if (referenceNode.getReference() != null) {
					this.gui.getController().navigateTo(referenceNode.getReference());
				} else {
					this.gui.getController().navigateTo(referenceNode.getEntry());
				}
			}
		}
	}

	private void onTokenClicked(MouseEvent event) {
		if (event.getClickCount() == 2) {
			Token selected = this.tokens.getSelectedValue();
			if (selected != null) {
				this.gui.openClass(this.gui.getController().getTokenHandle().getRef()).navigateToToken(selected);
			}
		}
	}

	public void retranslateUi() {
		this.panel.setTitle(I18n.translate("info_panel.tree.calls"));
	}

	public RPanel getPanel() {
		return this.panel;
	}
}
