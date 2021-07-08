/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.google.common.collect.Lists;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.dialog.JavadocDialog;
import cuchaz.enigma.gui.dialog.SearchDialog;
import cuchaz.enigma.gui.elements.*;
import cuchaz.enigma.gui.elements.rpanel.RPanel;
import cuchaz.enigma.gui.elements.rpanel.WorkspaceRPanelContainer;
import cuchaz.enigma.gui.panels.*;
import cuchaz.enigma.gui.renderer.MessageListCellRenderer;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.LanguageUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.network.Message;
import cuchaz.enigma.network.packet.MessageC2SPacket;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.ValidationContext;

public class Gui {

	private final MainWindow mainWindow = new MainWindow(Enigma.NAME);
	private final GuiController controller;

	private ConnectionState connectionState;
	private boolean isJarOpen;
	private final Set<EditableType> editableTypes;
	private boolean singleClassTree;

	private final RPanel messagePanel = new RPanel();
	private final RPanel userPanel = new RPanel();

	private final MenuBar menuBar;
	private final ObfPanel obfPanel;
	private final DeobfPanel deobfPanel;
	private final IdentifierPanel infoPanel;
	private final StructurePanel structurePanel;
	private final InheritanceTree inheritanceTree;
	private final ImplementationsTree implementationsTree;
	private final CallsTree callsTree;

	private final EditorTabbedPane editorTabbedPane;

	private final DefaultListModel<String> userModel = new DefaultListModel<>();
	private final DefaultListModel<Message> messageModel = new DefaultListModel<>();
	private final JList<String> users = new JList<>(userModel);
	private final JList<Message> messages = new JList<>(messageModel);
	private final JScrollPane messageScrollPane = new JScrollPane(this.messages);
	private final JTextField chatBox = new JTextField();

	private final JLabel connectionStatusLabel = new JLabel();

	public final JFileChooser jarFileChooser = new JFileChooser();
	public final JFileChooser tinyMappingsFileChooser = new JFileChooser();
	public final JFileChooser enigmaMappingsFileChooser = new JFileChooser();
	public final JFileChooser exportSourceFileChooser = new JFileChooser();
	public final JFileChooser exportJarFileChooser = new JFileChooser();
	public SearchDialog searchDialog;

	public Gui(EnigmaProfile profile, Set<EditableType> editableTypes) {
		this.editableTypes = editableTypes;
		this.controller = new GuiController(this, profile);
		this.structurePanel = new StructurePanel(this);
		this.deobfPanel = new DeobfPanel(this);
		this.infoPanel = new IdentifierPanel(this);
		this.obfPanel = new ObfPanel(this);
		this.menuBar = new MenuBar(this);
		this.inheritanceTree = new InheritanceTree(this);
		this.implementationsTree = new ImplementationsTree(this);
		this.callsTree = new CallsTree(this);
		this.editorTabbedPane = new EditorTabbedPane(this);

		this.setupUi();

		LanguageUtil.addListener(this::retranslateUi);
		Themes.addListener((lookAndFeel, boxHighlightPainters) -> SwingUtilities.updateComponentTreeUI(this.getFrame()));

		this.mainWindow.setVisible(true);
	}

	private void setupUi() {
		this.jarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		this.tinyMappingsFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		this.enigmaMappingsFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		this.enigmaMappingsFileChooser.setAcceptAllFileFilterUsed(false);

		this.exportSourceFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		this.exportSourceFileChooser.setAcceptAllFileFilterUsed(false);

		this.exportJarFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		// layout controls
		Container workArea = this.mainWindow.workArea();
		workArea.setLayout(new BorderLayout());
		workArea.add(infoPanel.getUi(), BorderLayout.NORTH);
		workArea.add(this.editorTabbedPane.getUi(), BorderLayout.CENTER);

		// left.getUi().setPreferredSize(ScaleUtil.getDimension(300, 0));
		// right.getUi().setPreferredSize(ScaleUtil.getDimension(250, 0));

		userPanel.getContentPane().setLayout(new BorderLayout());
		userPanel.getContentPane().add(new JScrollPane(this.users));

		messagePanel.getContentPane().setLayout(new BorderLayout());
		messages.setCellRenderer(new MessageListCellRenderer());
		JPanel chatPanel = new JPanel(new BorderLayout());
		AbstractAction sendListener = new AbstractAction("Send") {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		};
		chatBox.addActionListener(sendListener);
		JButton chatSendButton = new JButton(sendListener);
		chatPanel.add(chatBox, BorderLayout.CENTER);
		chatPanel.add(chatSendButton, BorderLayout.EAST);
		messagePanel.getContentPane().add(messageScrollPane, BorderLayout.CENTER);
		messagePanel.getContentPane().add(chatPanel, BorderLayout.SOUTH);

		// restore state
		int[] layout = UiConfig.getLayout();
		if (layout.length >= 4) {
			// this.splitClasses.setDividerLocation(layout[0]);
			// this.splitCenter.setDividerLocation(layout[1]);
			// this.splitRight.setDividerLocation(layout[2]);
			// this.logSplit.setDividerLocation(layout[3]);
		}

		WorkspaceRPanelContainer workspace = this.mainWindow.workspace();
		workspace.getRightTop().attach(structurePanel.getPanel());
		workspace.getRightTop().attach(inheritanceTree.getPanel());
		workspace.getRightTop().attach(implementationsTree.getPanel());
		workspace.getRightTop().attach(callsTree.getPanel());
		workspace.getLeftTop().attach(obfPanel.getPanel());
		workspace.getLeftBottom().attach(deobfPanel.getPanel());
		workspace.getRightTop().attach(messagePanel);
		workspace.getRightBottom().attach(userPanel);

		workspace.addDragTarget(structurePanel.getPanel());
		workspace.addDragTarget(inheritanceTree.getPanel());
		workspace.addDragTarget(implementationsTree.getPanel());
		workspace.addDragTarget(callsTree.getPanel());
		workspace.addDragTarget(obfPanel.getPanel());
		workspace.addDragTarget(deobfPanel.getPanel());
		workspace.addDragTarget(messagePanel);
		workspace.addDragTarget(userPanel);

		this.mainWindow.statusBar().addPermanentComponent(this.connectionStatusLabel);

		// init state
		setConnectionState(ConnectionState.NOT_CONNECTED);
		onCloseJar();

		JFrame frame = this.mainWindow.frame();
		frame.addWindowListener(GuiUtil.onWindowClose(e -> this.close()));

		frame.setSize(ScaleUtil.getDimension(1024, 576));
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		UiConfig.restoreWindowState(this.mainWindow);

		this.retranslateUi();
	}

	public MainWindow getMainWindow() {
		return this.mainWindow;
	}

	public JFrame getFrame() {
		return this.mainWindow.frame();
	}

	public GuiController getController() {
		return this.controller;
	}

	public void setSingleClassTree(boolean singleClassTree) {
		this.singleClassTree = singleClassTree;
		// this.classesPanel.removeAll();
		// this.classesPanel.add(isSingleClassTree() ? deobfPanel : splitClasses);
		getController().refreshClasses();
		retranslateUi();
	}

	public boolean isSingleClassTree() {
		return singleClassTree;
	}

	public void onStartOpenJar() {
//		this.classesPanel.removeAll();
		redraw();
	}

	public void onFinishOpenJar(String jarName) {
		// update gui
		this.mainWindow.setTitle(Enigma.NAME + " - " + jarName);
//		this.classesPanel.removeAll();
//		this.classesPanel.add(isSingleClassTree() ? deobfPanel : splitClasses);
		this.editorTabbedPane.closeAllEditorTabs();

		// update menu
		isJarOpen = true;

		updateUiState();
		redraw();
	}

	public void onCloseJar() {
		// update gui
		this.mainWindow.setTitle(Enigma.NAME);
		setObfClasses(null);
		setDeobfClasses(null);
		this.editorTabbedPane.closeAllEditorTabs();
//		this.classesPanel.removeAll();

		// update menu
		isJarOpen = false;
		setMappingsFile(null);

		updateUiState();
		redraw();
	}

	public EditorPanel openClass(ClassEntry entry) {
		return this.editorTabbedPane.openClass(entry);
	}

	@Nullable
	public EditorPanel getActiveEditor() {
		return this.editorTabbedPane.getActiveEditor();
	}

	public void closeEditor(EditorPanel editor) {
		this.editorTabbedPane.closeEditor(editor);
	}

	/**
	 * Navigates to the reference without modifying history. If the class is not currently loaded, it will be loaded.
	 *
	 * @param reference the reference
	 */
	public void showReference(EntryReference<Entry<?>, Entry<?>> reference) {
		this.editorTabbedPane.openClass(reference.getLocationClassEntry().getOutermostClass()).showReference(reference);
	}

	public void setObfClasses(Collection<ClassEntry> obfClasses) {
		this.obfPanel.obfClasses.setClasses(obfClasses);
	}

	public void setDeobfClasses(Collection<ClassEntry> deobfClasses) {
		this.deobfPanel.deobfClasses.setClasses(deobfClasses);
	}

	public void setMappingsFile(Path path) {
		this.enigmaMappingsFileChooser.setSelectedFile(path != null ? path.toFile() : null);
		updateUiState();
	}

	public void showTokens(EditorPanel editor, List<Token> tokens) {
		if (tokens.size() > 1) {
			this.controller.setTokenHandle(editor.getClassHandle().copy());
			this.callsTree.showTokens(tokens);
		} else {
			this.callsTree.clearTokens();
		}

		// show the first token
		editor.navigateToToken(tokens.get(0));
	}

	public void showCursorReference(EntryReference<Entry<?>, Entry<?>> reference) {
		infoPanel.setReference(reference == null ? null : reference.entry);
	}

	@Nullable
	public EntryReference<Entry<?>, Entry<?>> getCursorReference() {
		EditorPanel activeEditor = this.editorTabbedPane.getActiveEditor();
		return activeEditor == null ? null : activeEditor.getCursorReference();
	}

	public void startDocChange(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null || !this.isEditable(EditableType.JAVADOC)) return;
		JavadocDialog.show(mainWindow.frame(), getController(), cursorReference);
	}

	public void startRename(EditorPanel editor, String text) {
		if (editor != this.editorTabbedPane.getActiveEditor()) return;

		infoPanel.startRenaming(text);
	}

	public void startRename(EditorPanel editor) {
		if (editor != this.editorTabbedPane.getActiveEditor()) return;

		infoPanel.startRenaming();
	}

	public void showStructure(EditorPanel editor) {
		this.structurePanel.showStructure(editor);
	}

	public void showInheritance(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.inheritanceTree.display(cursorReference.entry);
	}

	public void showImplementations(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.implementationsTree.display(cursorReference.entry);
	}

	public void showCalls(EditorPanel editor, boolean recurse) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.callsTree.showCalls(cursorReference.entry, recurse);
	}

	public void toggleMapping(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		Entry<?> obfEntry = cursorReference.entry;

		if (this.controller.project.getMapper().getDeobfMapping(obfEntry).targetName() != null) {
			validateImmediateAction(vc -> this.controller.applyChange(vc, EntryChange.modify(obfEntry).clearDeobfName()));
		} else {
			validateImmediateAction(vc -> this.controller.applyChange(vc, EntryChange.modify(obfEntry).withDefaultDeobfName(this.getController().project)));
		}
	}

	private TreePath getPathToRoot(TreeNode node) {
		List<TreeNode> nodes = Lists.newArrayList();
		TreeNode n = node;
		do {
			nodes.add(n);
			n = n.getParent();
		} while (n != null);
		Collections.reverse(nodes);
		return new TreePath(nodes.toArray());
	}

	public void showDiscardDiag(Function<Integer, Void> callback, String... options) {
		int response = JOptionPane.showOptionDialog(this.mainWindow.frame(), I18n.translate("prompt.close.summary"), I18n.translate("prompt.close.title"), JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
		callback.apply(response);
	}

	public CompletableFuture<Void> saveMapping() {
		if (this.enigmaMappingsFileChooser.getSelectedFile() != null || this.enigmaMappingsFileChooser.showSaveDialog(this.mainWindow.frame()) == JFileChooser.APPROVE_OPTION)
			return this.controller.saveMappings(this.enigmaMappingsFileChooser.getSelectedFile().toPath());
		return CompletableFuture.completedFuture(null);
	}

	public void close() {
		if (!this.controller.isDirty()) {
			// everything is saved, we can exit safely
			exit();
		} else {
			// ask to save before closing
			showDiscardDiag((response) -> {
				if (response == JOptionPane.YES_OPTION) {
					this.saveMapping().thenRun(this::exit);
					// do not join, as join waits on swing to clear events
				} else if (response == JOptionPane.NO_OPTION) {
					exit();
				}

				return null;
			}, I18n.translate("prompt.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.cancel"));
		}
	}

	private void exit() {
		UiConfig.saveWindowState(this.mainWindow);
		UiConfig.save();

		if (searchDialog != null) {
			searchDialog.dispose();
		}
		this.mainWindow.frame().dispose();
		System.exit(0);
	}

	public void redraw() {
		JFrame frame = this.mainWindow.frame();

		frame.validate();
		frame.repaint();
	}

	public void onRenameFromClassTree(ValidationContext vc, Object prevData, Object data, DefaultMutableTreeNode node) {
		if (data instanceof String) {
			// package rename
			for (int i = 0; i < node.getChildCount(); i++) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
				ClassEntry prevDataChild = (ClassEntry) childNode.getUserObject();
				ClassEntry dataChild = new ClassEntry(data + "/" + prevDataChild.getSimpleName());

				onRenameFromClassTree(vc, prevDataChild, dataChild, node);
			}
			node.setUserObject(data);
			// Ob package will never be modified, just reload deob view
			this.deobfPanel.deobfClasses.reload();
		} else if (data instanceof ClassEntry) {
			// class rename

			// TODO optimize reverse class lookup, although it looks like it's
			//      fast enough for now
			EntryRemapper mapper = this.controller.project.getMapper();
			ClassEntry deobf = (ClassEntry) prevData;
			ClassEntry obf = mapper.getObfToDeobf().getAllEntries()
					.filter(e -> e instanceof ClassEntry)
					.map(e -> (ClassEntry) e)
					.filter(e -> mapper.deobfuscate(e).equals(deobf))
					.findAny().orElse(deobf);

			this.controller.applyChange(vc, EntryChange.modify(obf).withDeobfName(((ClassEntry) data).getFullName()));
		} else {
			throw new IllegalStateException(String.format("unhandled rename object data: '%s'", data));
		}
	}

	public void moveClassTree(Entry<?> obfEntry, String newName) {
		String oldEntry = obfEntry.getContainingClass().getPackageName();
		String newEntry = new ClassEntry(newName).getPackageName();
		moveClassTree(obfEntry, oldEntry == null, newEntry == null);
	}

	// TODO: getExpansionState will *not* actually update itself based on name changes!
	public void moveClassTree(Entry<?> obfEntry, boolean isOldOb, boolean isNewOb) {
		ClassEntry classEntry = obfEntry.getContainingClass();

		List<ClassSelector.StateEntry> stateDeobf = this.deobfPanel.deobfClasses.getExpansionState(this.deobfPanel.deobfClasses);
		List<ClassSelector.StateEntry> stateObf = this.obfPanel.obfClasses.getExpansionState(this.obfPanel.obfClasses);

		// Ob -> deob
		if (!isNewOb) {
			this.deobfPanel.deobfClasses.moveClassIn(classEntry);
			this.obfPanel.obfClasses.moveClassOut(classEntry);
			this.deobfPanel.deobfClasses.reload();
			this.obfPanel.obfClasses.reload();
		}
		// Deob -> ob
		else if (!isOldOb) {
			this.obfPanel.obfClasses.moveClassIn(classEntry);
			this.deobfPanel.deobfClasses.moveClassOut(classEntry);
			this.deobfPanel.deobfClasses.reload();
			this.obfPanel.obfClasses.reload();
		}
		// Local move
		else if (isOldOb) {
			this.obfPanel.obfClasses.moveClassIn(classEntry);
			this.obfPanel.obfClasses.reload();
		} else {
			this.deobfPanel.deobfClasses.moveClassIn(classEntry);
			this.deobfPanel.deobfClasses.reload();
		}

		this.deobfPanel.deobfClasses.restoreExpansionState(this.deobfPanel.deobfClasses, stateDeobf);
		this.obfPanel.obfClasses.restoreExpansionState(this.obfPanel.obfClasses, stateObf);
	}

	public ObfPanel getObfPanel() {
		return obfPanel;
	}

	public DeobfPanel getDeobfPanel() {
		return deobfPanel;
	}

	public SearchDialog getSearchDialog() {
		if (searchDialog == null) {
			searchDialog = new SearchDialog(this);
		}
		return searchDialog;
	}

	public void addMessage(Message message) {
		JScrollBar verticalScrollBar = messageScrollPane.getVerticalScrollBar();
		boolean isAtBottom = verticalScrollBar.getValue() >= verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent();
		messageModel.addElement(message);

		if (isAtBottom) {
			SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent()));
		}

		this.mainWindow.statusBar().showMessage(message.translate(), 5000);
	}

	public void setUserList(List<String> users) {
		userModel.clear();
		users.forEach(userModel::addElement);
		connectionStatusLabel.setText(String.format(I18n.translate("status.connected_user_count"), users.size()));
	}

	private void sendMessage() {
		String text = chatBox.getText().trim();
		if (!text.isEmpty()) {
			getController().sendPacket(new MessageC2SPacket(text));
		}
		chatBox.setText("");
	}

	/**
	 * Updates the state of the UI elements (button text, enabled state, ...) to reflect the current program state.
	 * This is a central place to update the UI state to prevent multiple code paths from changing the same state,
	 * causing inconsistencies.
	 */
	public void updateUiState() {
		menuBar.updateUiState();

		connectionStatusLabel.setText(I18n.translate(connectionState == ConnectionState.NOT_CONNECTED ? "status.disconnected" : "status.connected"));

		if (connectionState == ConnectionState.NOT_CONNECTED) {
			userPanel.setVisible(false);
			messagePanel.setVisible(false);
		} else {
			userPanel.setVisible(true);
			messagePanel.setVisible(true);
		}
	}

	public void retranslateUi() {
		this.jarFileChooser.setDialogTitle(I18n.translate("menu.file.jar.open"));
		this.exportJarFileChooser.setDialogTitle(I18n.translate("menu.file.export.jar"));
		this.userPanel.setTitle(I18n.translate("log_panel.users"));
		this.messagePanel.setTitle(I18n.translate("log_panel.messages"));
		this.connectionStatusLabel.setText(I18n.translate(connectionState == ConnectionState.NOT_CONNECTED ? "status.disconnected" : "status.connected"));

		this.updateUiState();

		this.menuBar.retranslateUi();
		this.obfPanel.retranslateUi();
		this.deobfPanel.retranslateUi();
		this.infoPanel.retranslateUi();
		this.structurePanel.retranslateUi();
		this.editorTabbedPane.retranslateUi();
		this.inheritanceTree.retranslateUi();
		this.implementationsTree.retranslateUi();
		this.structurePanel.retranslateUi();
		this.callsTree.retranslateUi();
	}

	public void setConnectionState(ConnectionState state) {
		connectionState = state;
		updateUiState();
	}

	public boolean isJarOpen() {
		return isJarOpen;
	}

	public ConnectionState getConnectionState() {
		return this.connectionState;
	}

	public boolean validateImmediateAction(Consumer<ValidationContext> op) {
		ValidationContext vc = new ValidationContext();
		op.accept(vc);
		if (!vc.canProceed()) {
			List<ParameterizedMessage> messages = vc.getMessages();
			String text = ValidatableUi.formatMessages(messages);
			JOptionPane.showMessageDialog(this.getFrame(), text, String.format("%d message(s)", messages.size()), JOptionPane.ERROR_MESSAGE);
		}
		return vc.canProceed();
	}

	public boolean isEditable(EditableType t) {
		return this.editableTypes.contains(t);
	}

}
