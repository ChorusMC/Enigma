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
import java.awt.Point;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.classhandle.ClassHandle;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.dialog.JavadocDialog;
import cuchaz.enigma.gui.dialog.SearchDialog;
import cuchaz.enigma.gui.elements.*;
import cuchaz.enigma.gui.elements.rpanel.RPanel;
import cuchaz.enigma.gui.elements.rpanel.WorkspaceRPanelContainer;
import cuchaz.enigma.gui.events.EditorActionListener;
import cuchaz.enigma.gui.panels.*;
import cuchaz.enigma.gui.renderer.CallsTreeCellRenderer;
import cuchaz.enigma.gui.renderer.MessageListCellRenderer;
import cuchaz.enigma.gui.util.LanguageUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.gui.util.SingleTreeSelectionModel;
import cuchaz.enigma.network.Message;
import cuchaz.enigma.network.packet.MessageC2SPacket;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
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

	private final RPanel callPanel = new RPanel();
	private final RPanel messagePanel = new RPanel();
	private final RPanel userPanel = new RPanel();

	private final MenuBar menuBar;
	private final ObfPanel obfPanel;
	private final DeobfPanel deobfPanel;
	private final IdentifierPanel infoPanel;
	private final StructurePanel structurePanel;
	private final InheritanceTree inheritanceTree;
	private final ImplementationsTree implementationsTree;

	private final JTree callsTree = new JTree();
	private final JList<Token> tokens = new JList<>();

	private final DefaultListModel<String> userModel = new DefaultListModel<>();
	private final DefaultListModel<Message> messageModel = new DefaultListModel<>();
	private final JList<String> users = new JList<>(userModel);
	private final JList<Message> messages = new JList<>(messageModel);
	private final JScrollPane messageScrollPane = new JScrollPane(this.messages);
	private final JTextField chatBox = new JTextField();

	private final JLabel connectionStatusLabel = new JLabel();

	private final EditorTabPopupMenu editorTabPopupMenu;
	private final DeobfPanelPopupMenu deobfPanelPopupMenu;
	private final JTabbedPane openFiles = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	private final HashBiMap<ClassEntry, EditorPanel> editors = HashBiMap.create();

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
		this.editorTabPopupMenu = new EditorTabPopupMenu(this);
		this.deobfPanelPopupMenu = new DeobfPanelPopupMenu(this);
		this.inheritanceTree = new InheritanceTree(this);
		this.implementationsTree = new ImplementationsTree(this);

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

		callsTree.setModel(null);
		callsTree.setCellRenderer(new CallsTreeCellRenderer(this));
		callsTree.setSelectionModel(new SingleTreeSelectionModel());
		callsTree.setShowsRootHandles(true);
		callsTree.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() >= 2 && event.getButton() == MouseEvent.BUTTON1) {
					// get the selected node
					TreePath path = callsTree.getSelectionPath();
					if (path == null) {
						return;
					}

					Object node = path.getLastPathComponent();
					if (node instanceof ReferenceTreeNode referenceNode) {
						if (referenceNode.getReference() != null) {
							controller.navigateTo(referenceNode.getReference());
						} else {
							controller.navigateTo(referenceNode.getEntry());
						}
					}
				}
			}
		});
		tokens.setCellRenderer(new TokenListCellRenderer(controller));
		tokens.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tokens.setLayoutOrientation(JList.VERTICAL);
		tokens.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() == 2) {
					Token selected = tokens.getSelectedValue();
					if (selected != null) {
						openClass(controller.getTokenHandle().getRef()).navigateToToken(selected);
					}
				}
			}
		});
		tokens.setPreferredSize(ScaleUtil.getDimension(0, 200));
		tokens.setMinimumSize(ScaleUtil.getDimension(0, 200));
		JSplitPane callPanelContentPane = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT,
				true,
				new JScrollPane(callsTree),
				new JScrollPane(tokens)
		);
		callPanelContentPane.setResizeWeight(1); // let the top side take all the slack
		callPanelContentPane.resetToPreferredSizes();
		callPanel.setContentPane(callPanelContentPane);

		openFiles.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					int i = openFiles.getUI().tabForCoordinate(openFiles, e.getX(), e.getY());
					if (i != -1) {
						editorTabPopupMenu.show(openFiles, e.getX(), e.getY(), EditorPanel.byUi(openFiles.getComponentAt(i)));
					}
				}

				showStructure(getActiveEditor());
			}
		});

		deobfPanel.deobfClasses.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					deobfPanel.deobfClasses.setSelectionRow(deobfPanel.deobfClasses.getClosestRowForLocation(e.getX(), e.getY()));
					int i = deobfPanel.deobfClasses.getRowForPath(deobfPanel.deobfClasses.getSelectionPath());
					if (i != -1) {
						deobfPanelPopupMenu.show(deobfPanel.deobfClasses, e.getX(), e.getY());
					}
				}
			}
		});

		// layout controls
		Container workArea = this.mainWindow.workArea();
		workArea.setLayout(new BorderLayout());
		workArea.add(infoPanel.getUi(), BorderLayout.NORTH);
		workArea.add(openFiles, BorderLayout.CENTER);

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
		workspace.getRightTop().attach(callPanel);
		workspace.getLeftTop().attach(obfPanel.getPanel());
		workspace.getLeftBottom().attach(deobfPanel.getPanel());
		workspace.getRightTop().attach(messagePanel);
		workspace.getRightBottom().attach(userPanel);

		workspace.addDragTarget(structurePanel.getPanel());
		workspace.addDragTarget(inheritanceTree.getPanel());
		workspace.addDragTarget(implementationsTree.getPanel());
		workspace.addDragTarget(callPanel);
		workspace.addDragTarget(obfPanel.getPanel());
		workspace.addDragTarget(deobfPanel.getPanel());
		workspace.addDragTarget(messagePanel);
		workspace.addDragTarget(userPanel);

		this.mainWindow.statusBar().addPermanentComponent(this.connectionStatusLabel);

		// init state
		setConnectionState(ConnectionState.NOT_CONNECTED);
		onCloseJar();

		JFrame frame = this.mainWindow.frame();
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				close();
			}
		});

		frame.setSize(UiConfig.getWindowSize("Main Window", ScaleUtil.getDimension(1024, 576)));
		frame.setMinimumSize(ScaleUtil.getDimension(640, 480));
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		Point windowPos = UiConfig.getWindowPos("Main Window", null);
		if (windowPos != null) {
			frame.setLocation(windowPos);
		} else {
			frame.setLocationRelativeTo(null);
		}

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
		closeAllEditorTabs();

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
		closeAllEditorTabs();
//		this.classesPanel.removeAll();

		// update menu
		isJarOpen = false;
		setMappingsFile(null);

		updateUiState();
		redraw();
	}

	public EditorPanel openClass(ClassEntry entry) {
		EditorPanel editorPanel = editors.computeIfAbsent(entry, e -> {
			ClassHandle ch = controller.getClassHandleProvider().openClass(entry);
			if (ch == null) return null;
			EditorPanel ed = new EditorPanel(this);
			ed.setup();
			ed.setClassHandle(ch);
			openFiles.addTab(ed.getFileName(), ed.getUi());

			ClosableTabTitlePane titlePane = new ClosableTabTitlePane(ed.getFileName(), () -> closeEditor(ed));
			openFiles.setTabComponentAt(openFiles.indexOfComponent(ed.getUi()), titlePane.getUi());
			titlePane.setTabbedPane(openFiles);

			ed.addListener(new EditorActionListener() {
				@Override
				public void onCursorReferenceChanged(EditorPanel editor, EntryReference<Entry<?>, Entry<?>> ref) {
					updateSelectedReference(editor, ref);
				}

				@Override
				public void onClassHandleChanged(EditorPanel editor, ClassEntry old, ClassHandle ch) {
					editors.remove(old);
					editors.put(ch.getRef(), editor);
				}

				@Override
				public void onTitleChanged(EditorPanel editor, String title) {
					titlePane.setText(editor.getFileName());
				}
			});

			ed.getEditor().addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_4 && (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
						closeEditor(ed);
					}
				}
			});

			return ed;
		});
		if (editorPanel != null) {
			openFiles.setSelectedComponent(editors.get(entry).getUi());
			showStructure(editorPanel);
		}

		return editorPanel;
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

	public void closeEditor(EditorPanel ed) {
		openFiles.remove(ed.getUi());
		editors.inverse().remove(ed);
		showStructure(getActiveEditor());
		ed.destroy();
	}

	public void closeAllEditorTabs() {
		for (Iterator<EditorPanel> iter = editors.values().iterator(); iter.hasNext(); ) {
			EditorPanel e = iter.next();
			openFiles.remove(e.getUi());
			e.destroy();
			iter.remove();
		}
	}

	public void closeTabsLeftOf(EditorPanel ed) {
		int index = openFiles.indexOfComponent(ed.getUi());
		for (int i = index - 1; i >= 0; i--) {
			closeEditor(EditorPanel.byUi(openFiles.getComponentAt(i)));
		}
	}

	public void closeTabsRightOf(EditorPanel ed) {
		int index = openFiles.indexOfComponent(ed.getUi());
		for (int i = openFiles.getTabCount() - 1; i > index; i--) {
			closeEditor(EditorPanel.byUi(openFiles.getComponentAt(i)));
		}
	}

	public void closeTabsExcept(EditorPanel ed) {
		int index = openFiles.indexOfComponent(ed.getUi());
		for (int i = openFiles.getTabCount() - 1; i >= 0; i--) {
			if (i == index) continue;
			closeEditor(EditorPanel.byUi(openFiles.getComponentAt(i)));
		}
	}

	public void showTokens(EditorPanel editor, Collection<Token> tokens) {
		Vector<Token> sortedTokens = new Vector<>(tokens);
		Collections.sort(sortedTokens);
		if (sortedTokens.size() > 1) {
			// sort the tokens and update the tokens panel
			this.controller.setTokenHandle(editor.getClassHandle().copy());
			this.tokens.setListData(sortedTokens);
			this.tokens.setSelectedIndex(0);
		} else {
			this.tokens.setListData(new Vector<>());
		}

		// show the first token
		editor.navigateToToken(sortedTokens.get(0));
	}

	private void updateSelectedReference(EditorPanel editor, EntryReference<Entry<?>, Entry<?>> ref) {
		if (editor != getActiveEditor()) return;

		showCursorReference(ref);
	}

	private void showCursorReference(EntryReference<Entry<?>, Entry<?>> reference) {
		infoPanel.setReference(reference == null ? null : reference.entry);
	}

	@Nullable
	public EditorPanel getActiveEditor() {
		return EditorPanel.byUi(openFiles.getSelectedComponent());
	}

	@Nullable
	public EntryReference<Entry<?>, Entry<?>> getCursorReference() {
		EditorPanel activeEditor = getActiveEditor();
		return activeEditor == null ? null : activeEditor.getCursorReference();
	}

	public void startDocChange(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null || !this.isEditable(EditableType.JAVADOC)) return;
		JavadocDialog.show(mainWindow.frame(), getController(), cursorReference);
	}

	public void startRename(EditorPanel editor, String text) {
		if (editor != getActiveEditor()) return;

		infoPanel.startRenaming(text);
	}

	public void startRename(EditorPanel editor) {
		if (editor != getActiveEditor()) return;

		infoPanel.startRenaming();
	}

	public void showStructure(EditorPanel editor) {
		JTree structureTree = this.structurePanel.getStructureTree();
		structureTree.setModel(null);

		if (editor == null) {
			this.structurePanel.getSortingPanel().setVisible(false);
			return;
		}

		ClassEntry classEntry = editor.getClassHandle().getRef();
		if (classEntry == null) return;

		this.structurePanel.getSortingPanel().setVisible(true);

		// get the class structure
		StructureTreeNode node = this.controller.getClassStructure(classEntry, this.structurePanel.getOptions());

		// show the tree at the root
		TreePath path = getPathToRoot(node);
		structureTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
		structureTree.expandPath(path);
		structureTree.setSelectionRow(structureTree.getRowForPath(path));

		redraw();
	}

	public void showInheritance(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.inheritanceTree.display(cursorReference.entry);

		redraw();
	}

	public void showImplementations(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		this.implementationsTree.display(cursorReference.entry);

		redraw();
	}

	public void showCalls(EditorPanel editor, boolean recurse) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		if (cursorReference.entry instanceof ClassEntry) {
			ClassReferenceTreeNode node = this.controller.getClassReferences((ClassEntry) cursorReference.entry);
			callsTree.setModel(new DefaultTreeModel(node));
		} else if (cursorReference.entry instanceof FieldEntry) {
			FieldReferenceTreeNode node = this.controller.getFieldReferences((FieldEntry) cursorReference.entry);
			callsTree.setModel(new DefaultTreeModel(node));
		} else if (cursorReference.entry instanceof MethodEntry) {
			MethodReferenceTreeNode node = this.controller.getMethodReferences((MethodEntry) cursorReference.entry, recurse);
			callsTree.setModel(new DefaultTreeModel(node));
		}

		callPanel.show();

		redraw();
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
		UiConfig.setWindowPos("Main Window", this.mainWindow.frame().getLocationOnScreen());
		UiConfig.setWindowSize("Main Window", this.mainWindow.frame().getSize());
		// UiConfig.setLayout(
		// 		this.splitClasses.getDividerLocation(),
		// 		this.splitCenter.getDividerLocation(),
		// 		this.splitRight.getDividerLocation(),
		// 		this.logSplit.getDividerLocation());
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
		this.callPanel.setTitle(I18n.translate("info_panel.tree.calls"));
		this.userPanel.setTitle(I18n.translate("log_panel.users"));
		this.messagePanel.setTitle(I18n.translate("log_panel.messages"));
		this.connectionStatusLabel.setText(I18n.translate(connectionState == ConnectionState.NOT_CONNECTED ? "status.disconnected" : "status.connected"));

		this.updateUiState();

		this.menuBar.retranslateUi();
		this.obfPanel.retranslateUi();
		this.deobfPanel.retranslateUi();
		this.deobfPanelPopupMenu.retranslateUi();
		this.infoPanel.retranslateUi();
		this.structurePanel.retranslateUi();
		this.editorTabPopupMenu.retranslateUi();
		this.editors.values().forEach(EditorPanel::retranslateUi);
		this.inheritanceTree.retranslateUi();
		this.implementationsTree.retranslateUi();
		this.structurePanel.retranslateUi();
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
