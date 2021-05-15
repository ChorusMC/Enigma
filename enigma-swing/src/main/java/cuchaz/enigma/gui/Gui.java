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
import java.awt.FileDialog;
import java.awt.Point;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.*;
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
import cuchaz.enigma.gui.dialog.CrashDialog;
import cuchaz.enigma.gui.dialog.JavadocDialog;
import cuchaz.enigma.gui.dialog.SearchDialog;
import cuchaz.enigma.gui.elements.*;
import cuchaz.enigma.gui.events.EditorActionListener;
import cuchaz.enigma.gui.panels.*;
import cuchaz.enigma.gui.renderer.CallsTreeCellRenderer;
import cuchaz.enigma.gui.renderer.ImplementationsTreeCellRenderer;
import cuchaz.enigma.gui.renderer.InheritanceTreeCellRenderer;
import cuchaz.enigma.gui.renderer.MessageListCellRenderer;
import cuchaz.enigma.gui.util.*;
import cuchaz.enigma.network.Message;
import cuchaz.enigma.network.packet.MarkDeobfuscatedC2SPacket;
import cuchaz.enigma.network.packet.MessageC2SPacket;
import cuchaz.enigma.network.packet.RemoveMappingC2SPacket;
import cuchaz.enigma.network.packet.RenameC2SPacket;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Os;
import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.ValidationContext;

public class Gui implements LanguageChangeListener {

	private final ObfPanel obfPanel;
	private final DeobfPanel deobfPanel;

	private final MenuBar menuBar;

	// state
	public History<EntryReference<Entry<?>, Entry<?>>> referenceHistory;
	private ConnectionState connectionState;
	private boolean isJarOpen;

	public FileDialog jarFileChooser;
	public FileDialog tinyMappingsFileChooser;
	public JFileChooser enigmaMappingsFileChooser;
	public JFileChooser exportSourceFileChooser;
	public FileDialog exportJarFileChooser;
	public SearchDialog searchDialog;
	private GuiController controller;
	private JFrame frame;
	private JPanel classesPanel;
	private JSplitPane splitClasses;
	private IdentifierPanel infoPanel;
	private StructurePanel structurePanel;
	private JTree inheritanceTree;
	private JTree implementationsTree;
	private JTree callsTree;
	private JList<Token> tokens;
	private JTabbedPane tabs;

	private JSplitPane splitCenter;
	private JSplitPane splitRight;
	private JSplitPane logSplit;
	private CollapsibleTabbedPane logTabs;
	private JList<String> users;
	private DefaultListModel<String> userModel;
	private JScrollPane messageScrollPane;
	private JList<Message> messages;
	private DefaultListModel<Message> messageModel;
	private JTextField chatBox;

	private JPanel statusBar;
	private JLabel connectionStatusLabel;
	private JLabel statusLabel;

	private final EditorTabPopupMenu editorTabPopupMenu;
	private final DeobfPanelPopupMenu deobfPanelPopupMenu;
	private final JTabbedPane openFiles;
	private final HashBiMap<ClassEntry, EditorPanel> editors = HashBiMap.create();

	public Gui(EnigmaProfile profile) {
		// init frame
		this.frame = new JFrame(Enigma.NAME);
		final Container pane = this.frame.getContentPane();
		pane.setLayout(new BorderLayout());

		if (Boolean.parseBoolean(System.getProperty("enigma.catchExceptions", "true"))) {
			// install a global exception handler to the event thread
			CrashDialog.init(this.frame);
			Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
				t.printStackTrace(System.err);
				if (!ExceptionIgnorer.shouldIgnore(t)) {
					CrashDialog.show(t);
				}
			});
		}

		Themes.addListener((lookAndFeel, boxHighlightPainters) -> SwingUtilities.updateComponentTreeUI(this.getFrame()));

		this.controller = new GuiController(this, profile);

		// init file choosers
		this.jarFileChooser = new FileDialog(getFrame(), I18n.translate("menu.file.jar.open"), FileDialog.LOAD);

		this.tinyMappingsFileChooser = new FileDialog(getFrame(), "Open tiny Mappings", FileDialog.LOAD);

		this.enigmaMappingsFileChooser = new JFileChooser();
		this.enigmaMappingsFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		this.enigmaMappingsFileChooser.setAcceptAllFileFilterUsed(false);

		this.exportSourceFileChooser = new JFileChooser();
		this.exportSourceFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		this.exportSourceFileChooser.setAcceptAllFileFilterUsed(false);

		this.exportJarFileChooser = new FileDialog(getFrame(), I18n.translate("menu.file.export.jar"), FileDialog.SAVE);

		this.obfPanel = new ObfPanel(this);
		this.deobfPanel = new DeobfPanel(this);

		// set up classes panel (don't add the splitter yet)
		splitClasses = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.obfPanel, this.deobfPanel);
		splitClasses.setResizeWeight(0.3);
		this.classesPanel = new JPanel();
		this.classesPanel.setLayout(new BorderLayout());
		this.classesPanel.setPreferredSize(ScaleUtil.getDimension(250, 0));

		// init info panel
		infoPanel = new IdentifierPanel(this);

		// init structure panel
		this.structurePanel = new StructurePanel(this);

		// init inheritance panel
		inheritanceTree = new JTree();
		inheritanceTree.setModel(null);
		inheritanceTree.setCellRenderer(new InheritanceTreeCellRenderer(this));
		inheritanceTree.setSelectionModel(new SingleTreeSelectionModel());
		inheritanceTree.setShowsRootHandles(true);
		inheritanceTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() >= 2 && event.getButton() == MouseEvent.BUTTON1) {
					// get the selected node
					TreePath path = inheritanceTree.getSelectionPath();
					if (path == null) {
						return;
					}

					Object node = path.getLastPathComponent();
					if (node instanceof ClassInheritanceTreeNode) {
						ClassInheritanceTreeNode classNode = (ClassInheritanceTreeNode) node;
						controller.navigateTo(new ClassEntry(classNode.getObfClassName()));
					} else if (node instanceof MethodInheritanceTreeNode) {
						MethodInheritanceTreeNode methodNode = (MethodInheritanceTreeNode) node;
						if (methodNode.isImplemented()) {
							controller.navigateTo(methodNode.getMethodEntry());
						}
					}
				}
			}
		});

		JPanel inheritancePanel = new JPanel();
		inheritancePanel.setLayout(new BorderLayout());
		inheritancePanel.add(new JScrollPane(inheritanceTree));

		// init implementations panel
		implementationsTree = new JTree();
		implementationsTree.setModel(null);
		implementationsTree.setCellRenderer(new ImplementationsTreeCellRenderer(this));
		implementationsTree.setSelectionModel(new SingleTreeSelectionModel());
		implementationsTree.setShowsRootHandles(true);
		implementationsTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (event.getClickCount() >= 2 && event.getButton() == MouseEvent.BUTTON1) {
					// get the selected node
					TreePath path = implementationsTree.getSelectionPath();
					if (path == null) {
						return;
					}

					Object node = path.getLastPathComponent();
					if (node instanceof ClassImplementationsTreeNode) {
						ClassImplementationsTreeNode classNode = (ClassImplementationsTreeNode) node;
						controller.navigateTo(classNode.getClassEntry());
					} else if (node instanceof MethodImplementationsTreeNode) {
						MethodImplementationsTreeNode methodNode = (MethodImplementationsTreeNode) node;
						controller.navigateTo(methodNode.getMethodEntry());
					}
				}
			}
		});
		JPanel implementationsPanel = new JPanel();
		implementationsPanel.setLayout(new BorderLayout());
		implementationsPanel.add(new JScrollPane(implementationsTree));

		// init call panel
		callsTree = new JTree();
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
					if (node instanceof ReferenceTreeNode) {
						ReferenceTreeNode<Entry<?>, Entry<?>> referenceNode = ((ReferenceTreeNode<Entry<?>, Entry<?>>) node);
						if (referenceNode.getReference() != null) {
							controller.navigateTo(referenceNode.getReference());
						} else {
							controller.navigateTo(referenceNode.getEntry());
						}
					}
				}
			}
		});
		tokens = new JList<>();
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
		JSplitPane callPanel = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT,
				true,
				new JScrollPane(callsTree),
				new JScrollPane(tokens)
		);
		callPanel.setResizeWeight(1); // let the top side take all the slack
		callPanel.resetToPreferredSizes();

		editorTabPopupMenu = new EditorTabPopupMenu(this);
		openFiles = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
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

		deobfPanelPopupMenu = new DeobfPanelPopupMenu(this);
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
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		centerPanel.add(infoPanel.getUi(), BorderLayout.NORTH);
		centerPanel.add(openFiles, BorderLayout.CENTER);
		tabs = new JTabbedPane();
		tabs.setPreferredSize(ScaleUtil.getDimension(250, 0));
		tabs.addTab(I18n.translate("info_panel.tree.structure"), structurePanel);
		tabs.addTab(I18n.translate("info_panel.tree.inheritance"), inheritancePanel);
		tabs.addTab(I18n.translate("info_panel.tree.implementations"), implementationsPanel);
		tabs.addTab(I18n.translate("info_panel.tree.calls"), callPanel);
		logTabs = new CollapsibleTabbedPane(JTabbedPane.BOTTOM);
		userModel = new DefaultListModel<>();
		users = new JList<>(userModel);
		messageModel = new DefaultListModel<>();
		messages = new JList<>(messageModel);
		messages.setCellRenderer(new MessageListCellRenderer());
		JPanel messagePanel = new JPanel(new BorderLayout());
		messageScrollPane = new JScrollPane(this.messages);
		messagePanel.add(messageScrollPane, BorderLayout.CENTER);
		JPanel chatPanel = new JPanel(new BorderLayout());
		chatBox = new JTextField();
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
		messagePanel.add(chatPanel, BorderLayout.SOUTH);
		logTabs.addTab(I18n.translate("log_panel.users"), new JScrollPane(this.users));
		logTabs.addTab(I18n.translate("log_panel.messages"), messagePanel);
		logSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, tabs, logTabs);
		logSplit.setResizeWeight(0.5);
		logSplit.resetToPreferredSizes();
		splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, centerPanel, this.logSplit);
		splitRight.setResizeWeight(1); // let the left side take all the slack
		splitRight.resetToPreferredSizes();
		splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, this.classesPanel, splitRight);
		splitCenter.setResizeWeight(0); // let the right side take all the slack
		pane.add(splitCenter, BorderLayout.CENTER);

		// restore state
		int[] layout = UiConfig.getLayout();
		if (layout.length >= 4) {
			this.splitClasses.setDividerLocation(layout[0]);
			this.splitCenter.setDividerLocation(layout[1]);
			this.splitRight.setDividerLocation(layout[2]);
			this.logSplit.setDividerLocation(layout[3]);
		}

		// init menus
		this.menuBar = new MenuBar(this);
		this.frame.setJMenuBar(this.menuBar.getUi());

		// init status bar
		statusBar = new JPanel(new BorderLayout());
		statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
		connectionStatusLabel = new JLabel();
		statusLabel = new JLabel();
		statusBar.add(statusLabel, BorderLayout.CENTER);
		statusBar.add(connectionStatusLabel, BorderLayout.EAST);
		pane.add(statusBar, BorderLayout.SOUTH);

		// init state
		setConnectionState(ConnectionState.NOT_CONNECTED);
		onCloseJar();

		this.frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				close();
			}
		});

		// show the frame
		pane.doLayout();
		this.frame.setSize(UiConfig.getWindowSize("Main Window", ScaleUtil.getDimension(1024, 576)));
		this.frame.setMinimumSize(ScaleUtil.getDimension(640, 480));
		this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		Point windowPos = UiConfig.getWindowPos("Main Window", null);
		if (windowPos != null) {
			this.frame.setLocation(windowPos);
		} else {
			this.frame.setLocationRelativeTo(null);
		}

		this.frame.setVisible(true);

		// init touch bar
		// must be called after frame is visible
		if (Os.getOs() == Os.MAC) {
			this.menuBar.initTouchBar(this.frame);
		}

		LanguageUtil.addListener(this);
	}

	public JFrame getFrame() {
		return this.frame;
	}

	public GuiController getController() {
		return this.controller;
	}

	public void onStartOpenJar() {
		this.classesPanel.removeAll();
		redraw();
	}

	public void onFinishOpenJar(String jarName) {
		// update gui
		this.frame.setTitle(Enigma.NAME + " - " + jarName);
		this.classesPanel.removeAll();
		this.classesPanel.add(splitClasses);
		closeAllEditorTabs();

		// update menu
		isJarOpen = true;

		updateUiState();
		redraw();
	}

	public void onCloseJar() {
		// update gui
		this.frame.setTitle(Enigma.NAME);
		setObfClasses(null);
		setDeobfClasses(null);
		closeAllEditorTabs();
		this.classesPanel.removeAll();

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
		if (cursorReference == null) return;
		JavadocDialog.show(frame, getController(), cursorReference);
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
		StructureTreeNode node = this.controller.getClassStructure(classEntry, this.structurePanel.shouldHideDeobfuscated());

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

		inheritanceTree.setModel(null);

		if (cursorReference.entry instanceof ClassEntry) {
			// get the class inheritance
			ClassInheritanceTreeNode classNode = this.controller.getClassInheritance((ClassEntry) cursorReference.entry);

			// show the tree at the root
			TreePath path = getPathToRoot(classNode);
			inheritanceTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
			inheritanceTree.expandPath(path);
			inheritanceTree.setSelectionRow(inheritanceTree.getRowForPath(path));
		} else if (cursorReference.entry instanceof MethodEntry) {
			// get the method inheritance
			MethodInheritanceTreeNode classNode = this.controller.getMethodInheritance((MethodEntry) cursorReference.entry);

			// show the tree at the root
			TreePath path = getPathToRoot(classNode);
			inheritanceTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
			inheritanceTree.expandPath(path);
			inheritanceTree.setSelectionRow(inheritanceTree.getRowForPath(path));
		}

		tabs.setSelectedIndex(1);

		redraw();
	}

	public void showImplementations(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		implementationsTree.setModel(null);

		DefaultMutableTreeNode node = null;

		// get the class implementations
		if (cursorReference.entry instanceof ClassEntry)
			node = this.controller.getClassImplementations((ClassEntry) cursorReference.entry);
		else // get the method implementations
			if (cursorReference.entry instanceof MethodEntry)
				node = this.controller.getMethodImplementations((MethodEntry) cursorReference.entry);

		if (node != null) {
			// show the tree at the root
			TreePath path = getPathToRoot(node);
			implementationsTree.setModel(new DefaultTreeModel((TreeNode) path.getPathComponent(0)));
			implementationsTree.expandPath(path);
			implementationsTree.setSelectionRow(implementationsTree.getRowForPath(path));
		}

		tabs.setSelectedIndex(2);

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

		tabs.setSelectedIndex(3);

		redraw();
	}

	public void toggleMapping(EditorPanel editor) {
		EntryReference<Entry<?>, Entry<?>> cursorReference = editor.getCursorReference();
		if (cursorReference == null) return;

		Entry<?> obfEntry = cursorReference.entry;

		if (controller.project.getMapper().extendedDeobfuscate(obfEntry).isDeobfuscated()) {
			if (!validateImmediateAction(vc -> this.controller.removeMapping(vc, cursorReference))) return;
			this.controller.sendPacket(new RemoveMappingC2SPacket(cursorReference.getNameableEntry()));
		} else {
			if (!validateImmediateAction(vc -> this.controller.markAsDeobfuscated(vc, cursorReference))) return;
			this.controller.sendPacket(new MarkDeobfuscatedC2SPacket(cursorReference.getNameableEntry()));
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
		int response = JOptionPane.showOptionDialog(this.frame, I18n.translate("prompt.close.summary"), I18n.translate("prompt.close.title"), JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
		callback.apply(response);
	}

	public void saveMapping() {
		if (this.enigmaMappingsFileChooser.getSelectedFile() != null || this.enigmaMappingsFileChooser.showSaveDialog(this.frame) == JFileChooser.APPROVE_OPTION)
			this.controller.saveMappings(this.enigmaMappingsFileChooser.getSelectedFile().toPath());
	}

	public void close() {
		if (!this.controller.isDirty()) {
			// everything is saved, we can exit safely
			exit();
		} else {
			// ask to save before closing
			showDiscardDiag((response) -> {
				if (response == JOptionPane.YES_OPTION) {
					this.saveMapping();
					exit();
				} else if (response == JOptionPane.NO_OPTION) {
					exit();
				}

				return null;
			}, I18n.translate("prompt.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.cancel"));
		}
	}

	private void exit() {
		UiConfig.setWindowPos("Main Window", this.frame.getLocationOnScreen());
		UiConfig.setWindowSize("Main Window", this.frame.getSize());
		UiConfig.setLayout(
				this.splitClasses.getDividerLocation(),
				this.splitCenter.getDividerLocation(),
				this.splitRight.getDividerLocation(),
				this.logSplit.getDividerLocation());
		UiConfig.save();

		if (searchDialog != null) {
			searchDialog.dispose();
		}
		this.frame.dispose();
		System.exit(0);
	}

	public void redraw() {
		this.frame.validate();
		this.frame.repaint();
	}

	public void onPanelRename(ValidationContext vc, Object prevData, Object data, DefaultMutableTreeNode node) {
		if (data instanceof String) {
			// package rename
			for (int i = 0; i < node.getChildCount(); i++) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
				ClassEntry prevDataChild = (ClassEntry) childNode.getUserObject();
				ClassEntry dataChild = new ClassEntry(data + "/" + prevDataChild.getSimpleName());

				onPanelRename(vc, prevDataChild, dataChild, node);
			}
			node.setUserObject(data);
			// Ob package will never be modified, just reload deob view
			this.deobfPanel.deobfClasses.reload();
		} else if (data instanceof ClassEntry) {
			// class rename

			// assume this is deobf since the obf tree doesn't allow renaming in
			// the first place
			// TODO optimize reverse class lookup, although it looks like it's
			//      fast enough for now
			EntryRemapper mapper = this.controller.project.getMapper();
			ClassEntry deobf = (ClassEntry) prevData;
			ClassEntry obf = mapper.getObfToDeobf().getAllEntries()
					.filter(e -> e instanceof ClassEntry)
					.map(e -> (ClassEntry) e)
					.filter(e -> mapper.deobfuscate(e).equals(deobf))
					.findAny().get();

			this.controller.rename(vc, new EntryReference<>(obf, obf.getFullName()), ((ClassEntry) data).getFullName(), false);
			if (!vc.canProceed()) return;
			this.controller.sendPacket(new RenameC2SPacket(obf, ((ClassEntry) data).getFullName(), false));
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


	public MenuBar getMenuBar() {
		return menuBar;
	}

	public void addMessage(Message message) {
		JScrollBar verticalScrollBar = messageScrollPane.getVerticalScrollBar();
		boolean isAtBottom = verticalScrollBar.getValue() >= verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent();
		messageModel.addElement(message);
		if (isAtBottom) {
			SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent()));
		}
		statusLabel.setText(message.translate());
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
			logSplit.setLeftComponent(null);
			splitRight.setRightComponent(tabs);
		} else {
			splitRight.setRightComponent(logSplit);
			logSplit.setLeftComponent(tabs);
		}

		splitRight.setDividerLocation(splitRight.getDividerLocation());
	}

	@Override
	public void retranslateUi() {
		this.jarFileChooser.setTitle(I18n.translate("menu.file.jar.open"));
		this.exportJarFileChooser.setTitle(I18n.translate("menu.file.export.jar"));
		this.tabs.setTitleAt(0, I18n.translate("info_panel.tree.structure"));
		this.tabs.setTitleAt(1, I18n.translate("info_panel.tree.inheritance"));
		this.tabs.setTitleAt(2, I18n.translate("info_panel.tree.implementations"));
		this.tabs.setTitleAt(3, I18n.translate("info_panel.tree.calls"));
		this.logTabs.setTitleAt(0, I18n.translate("log_panel.users"));
		this.logTabs.setTitleAt(1, I18n.translate("log_panel.messages"));
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
	}

	public void setConnectionState(ConnectionState state) {
		connectionState = state;
		statusLabel.setText(I18n.translate("status.ready"));
		updateUiState();
	}

	public boolean isJarOpen() {
		return isJarOpen;
	}

	public ConnectionState getConnectionState() {
		return this.connectionState;
	}

	public IdentifierPanel getInfoPanel() {
		return infoPanel;
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

}
