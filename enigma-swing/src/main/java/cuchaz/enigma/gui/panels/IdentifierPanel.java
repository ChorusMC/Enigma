package cuchaz.enigma.gui.panels;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.gui.EditableType;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.ConvertingTextField;
import cuchaz.enigma.gui.events.ConvertingTextFieldListener;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.entry.*;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.ValidationContext;

public class IdentifierPanel {

	private final Gui gui;

	private final JPanel ui = new JPanel();

	private Entry<?> entry;
	private Entry<?> deobfEntry;

	private ConvertingTextField nameField;

	private final ValidationContext vc = new ValidationContext();

	public IdentifierPanel(Gui gui) {
		this.gui = gui;

		this.ui.setLayout(new GridBagLayout());
		this.ui.setPreferredSize(ScaleUtil.getDimension(0, 120));
		this.ui.setBorder(BorderFactory.createTitledBorder(I18n.translate("info_panel.identifier")));
		this.ui.setEnabled(false);
	}

	public void setReference(Entry<?> entry) {
		this.entry = entry;
		refreshReference();
	}

	public boolean startRenaming() {
		if (this.nameField == null) return false;

		this.nameField.startEditing();

		return true;
	}

	public boolean startRenaming(String text) {
		if (this.nameField == null) return false;

		this.nameField.startEditing();
		this.nameField.setEditText(text);

		return true;
	}

	private void onModifierChanged(AccessModifier modifier) {
		gui.validateImmediateAction(vc -> this.gui.getController().applyChange(vc, EntryChange.modify(entry).withAccess(modifier)));
	}

	public void refreshReference() {
		this.deobfEntry = entry == null ? null : gui.getController().project.getMapper().deobfuscate(this.entry);

		this.nameField = null;

		TableHelper th = new TableHelper(this.ui, this.entry, this.gui);
		th.begin();
		if (this.entry == null) {
			this.ui.setEnabled(false);
		} else {
			this.ui.setEnabled(true);

			if (deobfEntry instanceof ClassEntry) {
				ClassEntry ce = (ClassEntry) deobfEntry;
				String name = ce.isInnerClass() ? ce.getName() : ce.getFullName();
				this.nameField = th.addRenameTextField(EditableType.CLASS, name);
				th.addModifierRow(I18n.translate("info_panel.identifier.modifier"), EditableType.CLASS, this::onModifierChanged);
			} else if (deobfEntry instanceof FieldEntry) {
				FieldEntry fe = (FieldEntry) deobfEntry;
				this.nameField = th.addRenameTextField(EditableType.FIELD, fe.getName());
				th.addStringRow(I18n.translate("info_panel.identifier.class"), fe.getParent().getFullName());
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.type_descriptor"), fe.getDesc().toString());
				th.addModifierRow(I18n.translate("info_panel.identifier.modifier"), EditableType.FIELD, this::onModifierChanged);
			} else if (deobfEntry instanceof MethodEntry) {
				MethodEntry me = (MethodEntry) deobfEntry;
				if (me.isConstructor()) {
					ClassEntry ce = me.getParent();
					if (ce != null) {
						String name = ce.isInnerClass() ? ce.getName() : ce.getFullName();
						this.nameField = th.addRenameTextField(EditableType.CLASS, name);
					}
				} else {
					this.nameField = th.addRenameTextField(EditableType.METHOD, me.getName());
					th.addStringRow(I18n.translate("info_panel.identifier.class"), me.getParent().getFullName());
				}
				th.addCopiableStringRow(I18n.translate("info_panel.identifier.method_descriptor"), me.getDesc().toString());
				th.addModifierRow(I18n.translate("info_panel.identifier.modifier"), EditableType.METHOD, this::onModifierChanged);
			} else if (deobfEntry instanceof LocalVariableEntry) {
				LocalVariableEntry lve = (LocalVariableEntry) deobfEntry;
				EditableType type;

				if (lve.isArgument()) {
					type = EditableType.PARAMETER;
				} else {
					type = EditableType.LOCAL_VARIABLE;
				}

				this.nameField = th.addRenameTextField(type, lve.getName());
				th.addStringRow(I18n.translate("info_panel.identifier.class"), lve.getContainingClass().getFullName());
				th.addStringRow(I18n.translate("info_panel.identifier.method"), lve.getParent().getName());
				th.addStringRow(I18n.translate("info_panel.identifier.index"), Integer.toString(lve.getIndex()));
			} else {
				throw new IllegalStateException("unreachable");
			}
		}
		th.end();

		if (this.nameField != null) {
			this.nameField.addListener(new ConvertingTextFieldListener() {
				@Override
				public void onStartEditing(ConvertingTextField field) {
					int i = field.getText().lastIndexOf('/');
					if (i != -1) {
						field.selectSubstring(i + 1);
					}
				}

				@Override
				public boolean tryStopEditing(ConvertingTextField field, boolean abort) {
					if (abort) return true;
					vc.reset();
					vc.setActiveElement(field);
					validateRename(field.getText());
					return vc.canProceed();
				}

				@Override
				public void onStopEditing(ConvertingTextField field, boolean abort) {
					if (!abort) {
						vc.reset();
						vc.setActiveElement(field);
						doRename(field.getText());
					}

					EditorPanel e = gui.getActiveEditor();
					if (e != null) {
						e.getEditor().requestFocusInWindow();
					}
				}
			});
		}

		this.ui.validate();
		this.ui.repaint();
	}

	private void validateRename(String newName) {
		gui.getController().validateChange(vc, EntryChange.modify(entry).withDeobfName(newName));
	}

	private void doRename(String newName) {
		EntryChange<? extends Entry<?>> change = EntryChange.modify(entry).withDeobfName(newName);
		gui.getController().applyChange(vc, change);
	}

	public void retranslateUi() {
		this.ui.setBorder(BorderFactory.createTitledBorder(I18n.translate("info_panel.identifier")));
		this.refreshReference();
	}

	public JPanel getUi() {
		return ui;
	}

	private static final class TableHelper {

		private final Container c;
		private final Entry<?> e;
		private final Gui gui;
		private int row;

		public TableHelper(Container c, Entry<?> e, Gui gui) {
			this.c = c;
			this.e = e;
			this.gui = gui;
		}

		public void begin() {
			c.removeAll();
			c.setLayout(new GridBagLayout());
		}

		public void addRow(Component c1, Component c2) {
			GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create()
					.insets(2)
					.anchor(GridBagConstraints.WEST);
			c.add(c1, cb.pos(0, this.row).build());
			c.add(c2, cb.pos(1, this.row).weightX(1.0).fill(GridBagConstraints.HORIZONTAL).build());

			this.row += 1;
		}

		public void addCopiableRow(JLabel c1, JLabel c2) {
			c2.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1) {
						GuiUtil.copyToClipboard(c2.getText());
						GuiUtil.showPopup(c2, I18n.translate("popup.copied"), e.getXOnScreen(), e.getYOnScreen());
					}
				}
			});
			addRow(c1, c2);
		}

		public ConvertingTextField addConvertingTextField(String c1, String c2) {
			ConvertingTextField textField = new ConvertingTextField(c2);
			addRow(new JLabel(c1), textField.getUi());
			return textField;
		}

		public ConvertingTextField addRenameTextField(EditableType type, String c2) {
			String description = switch(type) {
				case CLASS -> I18n.translate("info_panel.identifier.class");
				case METHOD -> I18n.translate("info_panel.identifier.method");
				case FIELD -> I18n.translate("info_panel.identifier.field");
				case PARAMETER, LOCAL_VARIABLE -> I18n.translate("info_panel.identifier.variable");
				default -> throw new IllegalStateException("Unexpected value: " + type);
			};

			if (this.gui.getController().project.isRenamable(e)) {
				ConvertingTextField field = addConvertingTextField(description, c2);
				field.setEditable(this.gui.isEditable(type));
				return field;
			} else {
				addStringRow(description, c2);
				return null;
			}
		}

		public void addStringRow(String c1, String c2) {
			addRow(new JLabel(c1), GuiUtil.unboldLabel(new JLabel(c2)));
		}

		public void addCopiableStringRow(String c1, String c2) {
			addCopiableRow(new JLabel(c1), GuiUtil.unboldLabel(new JLabel(c2)));
		}

		public JComboBox<AccessModifier> addModifierRow(String c1, EditableType type, Consumer<AccessModifier> changeListener) {
			EnigmaProject project = this.gui.getController().project;

			if (!project.isRenamable(e)) {
				return null;
			}

			JComboBox<AccessModifier> combo = new JComboBox<>(AccessModifier.values());
			EntryMapping mapping = project.getMapper().getDeobfMapping(e);
			combo.setSelectedIndex(mapping.accessModifier().ordinal());

			if (this.gui.isEditable(type)) {
				combo.addItemListener(event -> {
					if (event.getStateChange() == ItemEvent.SELECTED) {
						AccessModifier modifier = (AccessModifier) event.getItem();
						changeListener.accept(modifier);
					}
				});
			} else {
				combo.setEnabled(false);
			}

			addRow(new JLabel(c1), combo);

			return combo;
		}

		public void end() {
			// Add an empty panel with y-weight=1 so that all the other elements get placed at the top edge
			c.add(new JPanel(), GridBagConstraintsBuilder.create().pos(0, row).weight(0.0, 1.0).build());
		}

	}

}
