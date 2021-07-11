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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.io.MoreFiles;
import joptsimple.*;

import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.dialog.CrashDialog;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.utils.I18n;

public class Main {

	public static void main(String[] args) throws IOException {
		OptionParser parser = new OptionParser();

		OptionSpec<Path> jar = parser.accepts("jar", "Jar file to open at startup")
				.withRequiredArg()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Path> mappings = parser.accepts("mappings", "Mappings file to open at startup")
				.withRequiredArg()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Path> profile = parser.accepts("profile", "Profile json to apply at startup")
				.withRequiredArg()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		parser.acceptsAll(List.of("edit-all", "e"), "Enable editing everything");
		parser.acceptsAll(List.of("no-edit-all", "E"), "Disable editing everything");
		parser.acceptsAll(List.of("edit-classes", "c"), "Enable editing class names");
		parser.acceptsAll(List.of("no-edit-classes", "C"), "Disable editing class names");
		parser.acceptsAll(List.of("edit-methods", "m"), "Enable editing method names");
		parser.acceptsAll(List.of("no-edit-methods", "M"), "Disable editing method names");
		parser.acceptsAll(List.of("edit-fields", "f"), "Enable editing field names");
		parser.acceptsAll(List.of("no-edit-fields", "F"), "Disable editing field names");
		parser.acceptsAll(List.of("edit-parameters", "p"), "Enable editing parameter names");
		parser.acceptsAll(List.of("no-edit-parameters", "P"), "Disable editing parameter names");
		parser.acceptsAll(List.of("edit-locals"), "Enable editing local variable names");
		parser.acceptsAll(List.of("no-edit-locals"), "Disable editing local variable names");
		parser.acceptsAll(List.of("edit-javadocs", "d"), "Enable editing Javadocs");
		parser.acceptsAll(List.of("no-edit-javadocs", "D"), "Disable editing Javadocs");

		parser.accepts("single-class-tree", "Unify the deobfuscated and obfuscated class panels");

		parser.accepts("help", "Displays help information");

		try {
			OptionSet options = parser.parse(args);

			if (options.has("help")) {
				parser.printHelpOn(System.out);
				return;
			}

			Set<EditableType> editables = EnumSet.allOf(EditableType.class);

			for (OptionSpec<?> spec : options.specs()) {
				for (String s : spec.options()) {
					switch (s) {
						case "edit-all" -> editables.addAll(List.of(EditableType.values()));
						case "no-edit-all" -> editables.clear();
						case "edit-classes" -> editables.add(EditableType.CLASS);
						case "no-edit-classes" -> editables.remove(EditableType.CLASS);
						case "edit-methods" -> editables.add(EditableType.METHOD);
						case "no-edit-methods" -> editables.remove(EditableType.METHOD);
						case "edit-fields" -> editables.add(EditableType.FIELD);
						case "no-edit-fields" -> editables.remove(EditableType.FIELD);
						case "edit-parameters" -> editables.add(EditableType.PARAMETER);
						case "no-edit-parameters" -> editables.remove(EditableType.PARAMETER);
						case "edit-locals" -> {
							editables.add(EditableType.LOCAL_VARIABLE);
							System.err.println("warning: --edit-locals has no effect as local variables are currently not editable");
						}
						case "no-edit-locals" -> {
							editables.remove(EditableType.LOCAL_VARIABLE);
							System.err.println("warning: --no-edit-locals has no effect as local variables are currently not editable");
						}
						case "edit-javadocs" -> editables.add(EditableType.JAVADOC);
						case "no-edit-javadocs" -> editables.remove(EditableType.JAVADOC);
					}
				}
			}

			EnigmaProfile parsedProfile = EnigmaProfile.read(options.valueOf(profile));

			I18n.setLanguage(UiConfig.getLanguage());
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			Themes.setupTheme();

			Gui gui = new Gui(parsedProfile, editables);
			GuiController controller = gui.getController();
			
			if (options.has("single-class-tree")) {
				gui.setSingleClassTree(true);
			}

			if (Boolean.parseBoolean(System.getProperty("enigma.catchExceptions", "true"))) {
				// install a global exception handler to the event thread
				CrashDialog.init(gui.getFrame());
				Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
					t.printStackTrace(System.err);
					if (!ExceptionIgnorer.shouldIgnore(t)) {
						CrashDialog.show(t);
					}
				});
			}

			if (options.has(jar)) {
				Path jarPath = options.valueOf(jar);
				controller.openJar(jarPath)
						.whenComplete((v, t) -> {
							if (options.has(mappings)) {
								Path mappingsPath = options.valueOf(mappings);
								if (Files.isDirectory(mappingsPath)) {
									controller.openMappings(MappingFormat.ENIGMA_DIRECTORY, mappingsPath);
								} else if ("zip".equalsIgnoreCase(MoreFiles.getFileExtension(mappingsPath))) {
									controller.openMappings(MappingFormat.ENIGMA_ZIP, mappingsPath);
								} else {
									controller.openMappings(MappingFormat.ENIGMA_FILE, mappingsPath);
								}
							}
						});
			}
		} catch (OptionException e) {
			System.out.println("Invalid arguments: " + e.getMessage());
			System.out.println();
			parser.printHelpOn(System.out);
		}
	}

	public static class PathConverter implements ValueConverter<Path> {
		public static final ValueConverter<Path> INSTANCE = new PathConverter();

		PathConverter() {
		}

		@Override
		public Path convert(String path) {
			// expand ~ to the home dir
			if (path.startsWith("~")) {
				// get the home dir
				Path dirHome = Paths.get(System.getProperty("user.home"));

				// is the path just ~/ or is it ~user/ ?
				if (path.startsWith("~/")) {
					return dirHome.resolve(path.substring(2));
				} else {
					return dirHome.getParent().resolve(path.substring(1));
				}
			}

			return Paths.get(path);
		}

		@Override
		public Class<? extends Path> valueType() {
			return Path.class;
		}

		@Override
		public String valuePattern() {
			return "path";
		}
	}
}
