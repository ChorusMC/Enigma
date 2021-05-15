package cuchaz.enigma.gui.elements;

import java.awt.FileDialog;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.*;

import cuchaz.enigma.gui.ConnectionState;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.Decompiler;
import cuchaz.enigma.gui.config.LookAndFeel;
import cuchaz.enigma.gui.config.NetConfig;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.dialog.*;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.LanguageUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Pair;

import com.thizzer.jtouchbar.JTouchBar;
import com.thizzer.jtouchbar.common.Image;
import com.thizzer.jtouchbar.item.TouchBarItem;
import com.thizzer.jtouchbar.item.view.TouchBarButton;
import com.thizzer.jtouchbar.item.view.action.TouchBarViewAction;

public class MenuBar {

	private final JMenuBar ui = new JMenuBar();

	private final JMenu fileMenu = new JMenu();
	private final JMenuItem jarOpenItem = new JMenuItem();
	private final JMenuItem jarCloseItem = new JMenuItem();
	private final JMenu openMenu = new JMenu();
	private final JMenuItem saveMappingsItem = new JMenuItem();
	private final JMenu saveMappingsAsMenu = new JMenu();
	private final JMenuItem closeMappingsItem = new JMenuItem();
	private final JMenuItem dropMappingsItem = new JMenuItem();
	private final JMenuItem reloadMappingsItem = new JMenuItem();
	private final JMenuItem reloadAllItem = new JMenuItem();
	private final JMenuItem exportSourceItem = new JMenuItem();
	private final JMenuItem exportJarItem = new JMenuItem();
	private final JMenuItem statsItem = new JMenuItem();
	private final JMenuItem exitItem = new JMenuItem();

	private final JMenu decompilerMenu = new JMenu();

	private final JMenu viewMenu = new JMenu();
	private final JMenu themesMenu = new JMenu();
	private final JMenu languagesMenu = new JMenu();
	private final JMenu scaleMenu = new JMenu();
	private final JMenuItem fontItem = new JMenuItem();
	private final JMenuItem customScaleItem = new JMenuItem();

	private final JMenu searchMenu = new JMenu();
	private final JMenuItem searchClassItem = new JMenuItem();
	private final JMenuItem searchMethodItem = new JMenuItem();
	private final JMenuItem searchFieldItem = new JMenuItem();

	private final JMenu collabMenu = new JMenu();
	private final JMenuItem connectItem = new JMenuItem();
	private final JMenuItem startServerItem = new JMenuItem();

	private final JMenu helpMenu = new JMenu();
	private final JMenuItem aboutItem = new JMenuItem();
	private final JMenuItem githubItem = new JMenuItem();

	private final Gui gui;

	public MenuBar(Gui gui) {
		this.gui = gui;

		this.retranslateUi();

		prepareOpenMenu(this.openMenu, gui);
		prepareSaveMappingsAsMenu(this.saveMappingsAsMenu, this.saveMappingsItem, gui);
		prepareDecompilerMenu(this.decompilerMenu, gui);
		prepareThemesMenu(this.themesMenu, gui);
		prepareLanguagesMenu(this.languagesMenu);
		prepareScaleMenu(this.scaleMenu, gui);

		this.fileMenu.add(this.jarOpenItem);
		this.fileMenu.add(this.jarCloseItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.openMenu);
		this.fileMenu.add(this.saveMappingsItem);
		this.fileMenu.add(this.saveMappingsAsMenu);
		this.fileMenu.add(this.closeMappingsItem);
		this.fileMenu.add(this.dropMappingsItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.reloadMappingsItem);
		this.fileMenu.add(this.reloadAllItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.exportSourceItem);
		this.fileMenu.add(this.exportJarItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.statsItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.exitItem);
		this.ui.add(this.fileMenu);

		this.ui.add(this.decompilerMenu);

		this.viewMenu.add(this.themesMenu);
		this.viewMenu.add(this.languagesMenu);
		this.scaleMenu.add(this.customScaleItem);
		this.viewMenu.add(this.scaleMenu);
		this.viewMenu.add(this.fontItem);
		this.ui.add(this.viewMenu);

		this.searchMenu.add(this.searchClassItem);
		this.searchMenu.add(this.searchMethodItem);
		this.searchMenu.add(this.searchFieldItem);
		this.ui.add(this.searchMenu);

		this.collabMenu.add(this.connectItem);
		this.collabMenu.add(this.startServerItem);
		this.ui.add(this.collabMenu);

		this.helpMenu.add(this.aboutItem);
		this.helpMenu.add(this.githubItem);
		this.ui.add(this.helpMenu);

		this.saveMappingsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		this.searchClassItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK));

		this.jarOpenItem.addActionListener(_e -> this.onOpenJarClicked());
		this.jarCloseItem.addActionListener(_e -> this.gui.getController().closeJar());
		this.saveMappingsItem.addActionListener(_e -> this.onSaveMappingsClicked());
		this.closeMappingsItem.addActionListener(_e -> this.onCloseMappingsClicked());
		this.dropMappingsItem.addActionListener(_e -> this.gui.getController().dropMappings());
		this.reloadMappingsItem.addActionListener(_e -> this.onReloadMappingsClicked());
		this.reloadAllItem.addActionListener(_e -> this.onReloadAllClicked());
		this.exportSourceItem.addActionListener(_e -> this.onExportSourceClicked());
		this.exportJarItem.addActionListener(_e -> this.onExportJarClicked());
		this.statsItem.addActionListener(_e -> StatsDialog.show(this.gui));
		this.exitItem.addActionListener(_e -> this.gui.close());
		this.customScaleItem.addActionListener(_e -> this.onCustomScaleClicked());
		this.fontItem.addActionListener(_e -> this.onFontClicked(this.gui));
		this.searchClassItem.addActionListener(_e -> this.onSearchClicked(SearchDialog.Type.CLASS));
		this.searchMethodItem.addActionListener(_e -> this.onSearchClicked(SearchDialog.Type.METHOD));
		this.searchFieldItem.addActionListener(_e -> this.onSearchClicked(SearchDialog.Type.FIELD));
		this.connectItem.addActionListener(_e -> this.onConnectClicked());
		this.startServerItem.addActionListener(_e -> this.onStartServerClicked());
		this.aboutItem.addActionListener(_e -> AboutDialog.show(this.gui.getFrame()));
		this.githubItem.addActionListener(_e -> this.onGithubClicked());
	}

	public void updateUiState() {
		boolean jarOpen = this.gui.isJarOpen();
		ConnectionState connectionState = this.gui.getConnectionState();

		this.connectItem.setEnabled(jarOpen && connectionState != ConnectionState.HOSTING);
		this.connectItem.setText(I18n.translate(connectionState != ConnectionState.CONNECTED ? "menu.collab.connect" : "menu.collab.disconnect"));
		this.startServerItem.setEnabled(jarOpen && connectionState != ConnectionState.CONNECTED);
		this.startServerItem.setText(I18n.translate(connectionState != ConnectionState.HOSTING ? "menu.collab.server.start" : "menu.collab.server.stop"));

		this.jarCloseItem.setEnabled(jarOpen);
		this.openMenu.setEnabled(jarOpen);
		this.saveMappingsItem.setEnabled(jarOpen && this.gui.enigmaMappingsFileChooser.getSelectedFile() != null && connectionState != ConnectionState.CONNECTED);
		this.saveMappingsAsMenu.setEnabled(jarOpen);
		this.closeMappingsItem.setEnabled(jarOpen);
		this.reloadMappingsItem.setEnabled(jarOpen);
		this.reloadAllItem.setEnabled(jarOpen);
		this.exportSourceItem.setEnabled(jarOpen);
		this.exportJarItem.setEnabled(jarOpen);
		this.statsItem.setEnabled(jarOpen);
	}

	public void retranslateUi() {
		this.fileMenu.setText(I18n.translate("menu.file"));
		this.jarOpenItem.setText(I18n.translate("menu.file.jar.open"));
		this.jarCloseItem.setText(I18n.translate("menu.file.jar.close"));
		this.openMenu.setText(I18n.translate("menu.file.mappings.open"));
		this.saveMappingsItem.setText(I18n.translate("menu.file.mappings.save"));
		this.saveMappingsAsMenu.setText(I18n.translate("menu.file.mappings.save_as"));
		this.closeMappingsItem.setText(I18n.translate("menu.file.mappings.close"));
		this.dropMappingsItem.setText(I18n.translate("menu.file.mappings.drop"));
		this.reloadMappingsItem.setText(I18n.translate("menu.file.reload_mappings"));
		this.reloadAllItem.setText(I18n.translate("menu.file.reload_all"));
		this.exportSourceItem.setText(I18n.translate("menu.file.export.source"));
		this.exportJarItem.setText(I18n.translate("menu.file.export.jar"));
		this.statsItem.setText(I18n.translate("menu.file.stats"));
		this.exitItem.setText(I18n.translate("menu.file.exit"));

		this.decompilerMenu.setText(I18n.translate("menu.decompiler"));

		this.viewMenu.setText(I18n.translate("menu.view"));
		this.themesMenu.setText(I18n.translate("menu.view.themes"));
		this.languagesMenu.setText(I18n.translate("menu.view.languages"));
		this.scaleMenu.setText(I18n.translate("menu.view.scale"));
		this.fontItem.setText(I18n.translate("menu.view.font"));
		this.customScaleItem.setText(I18n.translate("menu.view.scale.custom"));

		this.searchMenu.setText(I18n.translate("menu.search"));
		this.searchClassItem.setText(I18n.translate("menu.search.class"));
		this.searchMethodItem.setText(I18n.translate("menu.search.method"));
		this.searchFieldItem.setText(I18n.translate("menu.search.field"));

		this.collabMenu.setText(I18n.translate("menu.collab"));
		this.connectItem.setText(I18n.translate("menu.collab.connect"));
		this.startServerItem.setText(I18n.translate("menu.collab.server.start"));

		this.helpMenu.setText(I18n.translate("menu.help"));
		this.aboutItem.setText(I18n.translate("menu.help.about"));
		this.githubItem.setText(I18n.translate("menu.help.github"));
	}

	private static void addTouchBarButton(JTouchBar touchBar, String id, String labelKey, String icon, TouchBarViewAction action) {
		Image image = new Image(icon, false);

		TouchBarButton button = new TouchBarButton();
		button.setImage(image);
		button.setAction(action);

		TouchBarItem item = new TouchBarItem(id, button, true);
		item.setCustomizationLabel(I18n.translate(labelKey));

		touchBar.addItem(item);
	}

	public void initTouchBar(JFrame frame) {
		JTouchBar touchBar = new JTouchBar();
		touchBar.setCustomizationIdentifier("Enigma");

		touchBar.show(frame);
	}

	public JMenuBar getUi() {
		return this.ui;
	}

	private void onOpenJarClicked() {
		FileDialog d = this.gui.jarFileChooser;
		d.setDirectory(UiConfig.getLastSelectedDir());
		d.setVisible(true);
		String file = d.getFile();
		// checks if the file name is not empty
		if (file != null) {
			Path path = Paths.get(d.getDirectory()).resolve(file);
			// checks if the file name corresponds to an existing file
			if (Files.exists(path)) {
				this.gui.getController().openJar(path);
			}
			UiConfig.setLastSelectedDir(d.getDirectory());
		}
	}

	private void onSaveMappingsClicked() {
		this.gui.getController().saveMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile().toPath());
	}

	private void openMappingsDiscardPrompt(Runnable then) {
		if (this.gui.getController().isDirty()) {
			this.gui.showDiscardDiag((response -> {
				if (response == JOptionPane.YES_OPTION) {
					this.gui.saveMapping();
					then.run();
				} else if (response == JOptionPane.NO_OPTION)
					then.run();
				return null;
			}), I18n.translate("prompt.close.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.cancel"));
		} else {
			then.run();
		}
	}

	private void onCloseMappingsClicked() {
		openMappingsDiscardPrompt(() -> this.gui.getController().closeMappings());
	}

	private void onReloadMappingsClicked() {
		openMappingsDiscardPrompt(() -> this.gui.getController().reloadMappings());
	}

	private void onReloadAllClicked() {
		openMappingsDiscardPrompt(() -> this.gui.getController().reloadAll());
	}

	private void onExportSourceClicked() {
		this.gui.exportSourceFileChooser.setCurrentDirectory(new File(UiConfig.getLastSelectedDir()));
		if (this.gui.exportSourceFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
			UiConfig.setLastSelectedDir(this.gui.exportSourceFileChooser.getCurrentDirectory().toString());
			this.gui.getController().exportSource(this.gui.exportSourceFileChooser.getSelectedFile().toPath());
		}
	}

	private void onExportJarClicked() {
		this.gui.exportJarFileChooser.setDirectory(UiConfig.getLastSelectedDir());
		this.gui.exportJarFileChooser.setVisible(true);
		if (this.gui.exportJarFileChooser.getFile() != null) {
			Path path = Paths.get(this.gui.exportJarFileChooser.getDirectory(), this.gui.exportJarFileChooser.getFile());
			this.gui.getController().exportJar(path);
			UiConfig.setLastSelectedDir(this.gui.exportJarFileChooser.getDirectory());
		}
	}

	private void onCustomScaleClicked() {
		String answer = (String) JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("menu.view.scale.custom.title"), I18n.translate("menu.view.scale.custom.title"),
				JOptionPane.QUESTION_MESSAGE, null, null, Float.toString(UiConfig.getScaleFactor() * 100));
		if (answer == null) return;
		float newScale = 1.0f;
		try {
			newScale = Float.parseFloat(answer) / 100f;
		} catch (NumberFormatException ignored) {
		}
		ScaleUtil.setScaleFactor(newScale);
		ChangeDialog.show(this.gui.getFrame());
	}

	private void onFontClicked(Gui gui) {
//		FontDialog fd = new FontDialog(gui.getFrame(), "Choose Font", true);
//		fd.setLocationRelativeTo(gui.getFrame());
//		fd.setSelectedFont(UiConfig.getEditorFont());
//		fd.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
//		fd.setVisible(true);
//
//		if (!fd.isCancelSelected()) {
//			UiConfig.setEditorFont(fd.getSelectedFont());
//			UiConfig.save();
//		}
		FontDialog.display(gui.getFrame());
	}

	private void onSearchClicked(SearchDialog.Type type) {
		if (this.gui.getController().project != null) {
			this.gui.getSearchDialog().show(type);
		}
	}

	private void onConnectClicked() {
		if (this.gui.getController().getClient() != null) {
			this.gui.getController().disconnectIfConnected(null);
			return;
		}
		ConnectToServerDialog.Result result = ConnectToServerDialog.show(this.gui.getFrame());
		if (result == null) {
			return;
		}
		this.gui.getController().disconnectIfConnected(null);
		try {
			this.gui.getController().createClient(result.getUsername(), result.getAddress().address, result.getAddress().port, result.getPassword());
			NetConfig.setUsername(result.getUsername());
			NetConfig.setRemoteAddress(result.getAddressStr());
			NetConfig.setPassword(String.valueOf(result.getPassword()));
			NetConfig.save();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.collab.connect.error"), JOptionPane.ERROR_MESSAGE);
			this.gui.getController().disconnectIfConnected(null);
		}
		Arrays.fill(result.getPassword(), (char) 0);
	}

	private void onStartServerClicked() {
		if (this.gui.getController().getServer() != null) {
			this.gui.getController().disconnectIfConnected(null);
			return;
		}
		CreateServerDialog.Result result = CreateServerDialog.show(this.gui.getFrame());
		if (result == null) {
			return;
		}
		this.gui.getController().disconnectIfConnected(null);
		try {
			this.gui.getController().createServer(result.getPort(), result.getPassword());
			NetConfig.setServerPort(result.getPort());
			NetConfig.setServerPassword(String.valueOf(result.getPassword()));
			NetConfig.save();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.collab.server.start.error"), JOptionPane.ERROR_MESSAGE);
			this.gui.getController().disconnectIfConnected(null);
		}
	}

	private void onGithubClicked() {
		GuiUtil.openUrl("https://github.com/FabricMC/Enigma");
	}

	private static void prepareOpenMenu(JMenu openMenu, Gui gui) {
		for (MappingFormat format : MappingFormat.values()) {
			if (format.getReader() != null) {
				JMenuItem item = new JMenuItem(I18n.translate("mapping_format." + format.name().toLowerCase(Locale.ROOT)));
				item.addActionListener(event -> {
					gui.enigmaMappingsFileChooser.setCurrentDirectory(new File(UiConfig.getLastSelectedDir()));
					if (gui.enigmaMappingsFileChooser.showOpenDialog(gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
						File selectedFile = gui.enigmaMappingsFileChooser.getSelectedFile();
						gui.getController().openMappings(format, selectedFile.toPath());
						UiConfig.setLastSelectedDir(gui.enigmaMappingsFileChooser.getCurrentDirectory().toString());
					}
				});
				openMenu.add(item);
			}
		}
	}

	private static void prepareSaveMappingsAsMenu(JMenu saveMappingsAsMenu, JMenuItem saveMappingsItem, Gui gui) {
		for (MappingFormat format : MappingFormat.values()) {
			if (format.getWriter() != null) {
				JMenuItem item = new JMenuItem(I18n.translate("mapping_format." + format.name().toLowerCase(Locale.ROOT)));
				item.addActionListener(event -> {
					// TODO: Use a specific file chooser for it
					if (gui.enigmaMappingsFileChooser.getCurrentDirectory() == null) {
						gui.enigmaMappingsFileChooser.setCurrentDirectory(new File(UiConfig.getLastSelectedDir()));
					}

					if (gui.enigmaMappingsFileChooser.showSaveDialog(gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
						gui.getController().saveMappings(gui.enigmaMappingsFileChooser.getSelectedFile().toPath(), format);
						saveMappingsItem.setEnabled(true);
						UiConfig.setLastSelectedDir(gui.enigmaMappingsFileChooser.getCurrentDirectory().toString());
					}
				});
				saveMappingsAsMenu.add(item);
			}
		}
	}

	private static void prepareDecompilerMenu(JMenu decompilerMenu, Gui gui) {
		ButtonGroup decompilerGroup = new ButtonGroup();

		for (Decompiler decompiler : Decompiler.values()) {
			JRadioButtonMenuItem decompilerButton = new JRadioButtonMenuItem(decompiler.name);
			decompilerGroup.add(decompilerButton);
			if (decompiler.equals(UiConfig.getDecompiler())) {
				decompilerButton.setSelected(true);
			}
			decompilerButton.addActionListener(event -> {
				gui.getController().setDecompiler(decompiler.service);

				UiConfig.setDecompiler(decompiler);
				UiConfig.save();
			});
			decompilerMenu.add(decompilerButton);
		}
	}

	private static void prepareThemesMenu(JMenu themesMenu, Gui gui) {
		ButtonGroup themeGroup = new ButtonGroup();
		for (LookAndFeel lookAndFeel : LookAndFeel.values()) {
			JRadioButtonMenuItem themeButton = new JRadioButtonMenuItem(I18n.translate("menu.view.themes." + lookAndFeel.name().toLowerCase(Locale.ROOT)));
			themeGroup.add(themeButton);
			if (lookAndFeel.equals(UiConfig.getLookAndFeel())) {
				themeButton.setSelected(true);
			}
			themeButton.addActionListener(_e -> {
				UiConfig.setLookAndFeel(lookAndFeel);
				UiConfig.save();
				ChangeDialog.show(gui.getFrame());
			});
			themesMenu.add(themeButton);
		}
	}

	private static void prepareLanguagesMenu(JMenu languagesMenu) {
		ButtonGroup languageGroup = new ButtonGroup();
		for (String lang : I18n.getAvailableLanguages()) {
			JRadioButtonMenuItem languageButton = new JRadioButtonMenuItem(I18n.getLanguageName(lang));
			languageGroup.add(languageButton);
			if (lang.equals(UiConfig.getLanguage())) {
				languageButton.setSelected(true);
			}
			languageButton.addActionListener(event -> {
				UiConfig.setLanguage(lang);
				I18n.setLanguage(lang);
				LanguageUtil.dispatchLanguageChange();
				UiConfig.save();
			});
			languagesMenu.add(languageButton);
		}
	}

	private static void prepareScaleMenu(JMenu scaleMenu, Gui gui) {
		ButtonGroup scaleGroup = new ButtonGroup();
		Map<Float, JRadioButtonMenuItem> scaleButtons = IntStream.of(100, 125, 150, 175, 200)
				.mapToObj(scaleFactor -> {
					float realScaleFactor = scaleFactor / 100f;
					JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(String.format("%d%%", scaleFactor));
					menuItem.addActionListener(event -> ScaleUtil.setScaleFactor(realScaleFactor));
					menuItem.addActionListener(event -> ChangeDialog.show(gui.getFrame()));
					scaleGroup.add(menuItem);
					scaleMenu.add(menuItem);
					return new Pair<>(realScaleFactor, menuItem);
				})
				.collect(Collectors.toMap(x -> x.a, x -> x.b));

		JRadioButtonMenuItem currentScaleButton = scaleButtons.get(UiConfig.getScaleFactor());
		if (currentScaleButton != null) {
			currentScaleButton.setSelected(true);
		}

		ScaleUtil.addListener((newScale, _oldScale) -> {
			JRadioButtonMenuItem mi = scaleButtons.get(newScale);
			if (mi != null) {
				mi.setSelected(true);
			} else {
				scaleGroup.clearSelection();
			}
		});
	}

}
