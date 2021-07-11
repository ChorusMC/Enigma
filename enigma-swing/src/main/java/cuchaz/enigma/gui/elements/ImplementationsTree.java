package cuchaz.enigma.gui.elements;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.renderer.ImplementationsTreeCellRenderer;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;

public class ImplementationsTree extends AbstractInheritanceTree {
	public ImplementationsTree(Gui gui) {
		super("Implementations", gui, new ImplementationsTreeCellRenderer(gui));
	}

	@Nullable
	@Override
	protected DefaultMutableTreeNode getNodeFor(Entry<?> entry) {
		if (entry instanceof ClassEntry classEntry) {
			return this.gui.getController().getClassImplementations(classEntry);
		} else if (entry instanceof MethodEntry methodEntry) {
			return this.gui.getController().getMethodImplementations(methodEntry);
		}

		return null;
	}

	@Override
	protected String getPanelName() {
		return I18n.translate("info_panel.tree.implementations");
	}
}
