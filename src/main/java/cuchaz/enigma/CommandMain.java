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

package cuchaz.enigma;

import cuchaz.enigma.Deobfuscator.ProgressListener;
import cuchaz.enigma.translation.mapping.BidirectionalMapper;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.tree.MappingTree;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.jar.JarFile;

public class CommandMain {

	public static void main(String[] args) throws Exception {
		try {
			// process the command
			String command = getArg(args, 0, "command", true);
			if (command.equalsIgnoreCase("deobfuscate")) {
				deobfuscate(args);
			} else if (command.equalsIgnoreCase("decompile")) {
				decompile(args);
			} else if (command.equalsIgnoreCase("convertmappings")) {
				convertMappings(args);
			} else {
				throw new IllegalArgumentException("Command not recognized: " + command);
			}
		} catch (IllegalArgumentException ex) {
			System.out.println(ex.getMessage());
			printHelp();
		}
	}

	private static void printHelp() {
		System.out.println(String.format("%s - %s", Constants.NAME, Constants.VERSION));
		System.out.println("Usage:");
		System.out.println("\tjava -cp enigma.jar cuchaz.enigma.CommandMain <command>");
		System.out.println("\twhere <command> is one of:");
		System.out.println("\t\tdeobfuscate <in jar> <out jar> [<mappings file>]");
		System.out.println("\t\tdecompile <in jar> <out folder> [<mappings file>]");
		System.out.println("\t\tconvertmappings <enigma mappings> <converted mappings> <ENIGMA_FILE|ENIGMA_DIRECTORY|SRG_FILE>");
	}

	private static void decompile(String[] args) throws Exception {
		File fileJarIn = getReadableFile(getArg(args, 1, "in jar", true));
		File fileJarOut = getWritableFolder(getArg(args, 2, "out folder", true));
		Path fileMappings = getReadablePath(getArg(args, 3, "mappings file", false));
		Deobfuscator deobfuscator = getDeobfuscator(fileMappings, new JarFile(fileJarIn));
		deobfuscator.writeSources(fileJarOut, new ConsoleProgressListener());
	}

	private static void deobfuscate(String[] args) throws Exception {
		File fileJarIn = getReadableFile(getArg(args, 1, "in jar", true));
		File fileJarOut = getWritableFile(getArg(args, 2, "out jar", true));
		Path fileMappings = getReadablePath(getArg(args, 3, "mappings file", false));
		Deobfuscator deobfuscator = getDeobfuscator(fileMappings, new JarFile(fileJarIn));
		deobfuscator.writeJar(fileJarOut, new ConsoleProgressListener());
	}

	private static Deobfuscator getDeobfuscator(Path fileMappings, JarFile jar) throws Exception {
		System.out.println("Reading jar...");
		Deobfuscator deobfuscator = new Deobfuscator(jar);
		if (fileMappings != null) {
			System.out.println("Reading mappings...");
			MappingTree<EntryMapping> mappings = chooseEnigmaFormat(fileMappings).read(fileMappings);
			deobfuscator.setMapper(new BidirectionalMapper(deobfuscator.getJarIndex(), mappings));
		}
		return deobfuscator;
	}

	private static void convertMappings(String[] args) throws Exception {
		Path fileMappings = getReadablePath(getArg(args, 1, "enigma mapping", true));
		File result = getWritableFile(getArg(args, 2, "enigma mapping", true));
		String name = getArg(args, 3, "format desc", true);
		MappingFormat saveFormat;
		try {
			saveFormat = MappingFormat.valueOf(name.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(name + "is not a valid mapping format!");
		}

		System.out.println("Reading mappings...");

		MappingFormat readFormat = chooseEnigmaFormat(fileMappings);
		MappingTree<EntryMapping> mappings = readFormat.read(fileMappings);
		System.out.println("Saving new mappings...");

		saveFormat.write(mappings, result.toPath());
	}

	private static MappingFormat chooseEnigmaFormat(Path path) {
		if (Files.isDirectory(path)) {
			return MappingFormat.ENIGMA_DIRECTORY;
		} else {
			return MappingFormat.ENIGMA_FILE;
		}
	}

	private static String getArg(String[] args, int i, String name, boolean required) {
		if (i >= args.length) {
			if (required) {
				throw new IllegalArgumentException(name + " is required");
			} else {
				return null;
			}
		}
		return args[i];
	}

	private static File getWritableFile(String path) {
		if (path == null) {
			return null;
		}
		File file = new File(path).getAbsoluteFile();
		File dir = file.getParentFile();
		if (dir == null) {
			throw new IllegalArgumentException("Cannot write file: " + path);
		}
		// quick fix to avoid stupid stuff in Gradle code
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		return file;
	}

	private static File getWritableFolder(String path) {
		if (path == null) {
			return null;
		}
		File dir = new File(path).getAbsoluteFile();
		if (!dir.exists()) {
			throw new IllegalArgumentException("Cannot write to folder: " + dir);
		}
		return dir;
	}

	private static File getReadableFile(String path) {
		if (path == null) {
			return null;
		}
		File file = new File(path).getAbsoluteFile();
		if (!file.exists()) {
			throw new IllegalArgumentException("Cannot find file: " + file.getAbsolutePath());
		}
		return file;
	}

	private static Path getReadablePath(String path) {
		if (path == null) {
			return null;
		}
		Path file = Paths.get(path).toAbsolutePath();
		if (!Files.exists(file)) {
			throw new IllegalArgumentException("Cannot find file: " + file.toString());
		}
		return file;
	}

	public static class ConsoleProgressListener implements ProgressListener {

		private static final int ReportTime = 5000; // 5s

		private int totalWork;
		private long startTime;
		private long lastReportTime;

		@Override
		public void init(int totalWork, String title) {
			this.totalWork = totalWork;
			this.startTime = System.currentTimeMillis();
			this.lastReportTime = this.startTime;
			System.out.println(title);
		}

		@Override
		public void onProgress(int numDone, String message) {
			long now = System.currentTimeMillis();
			boolean isLastUpdate = numDone == this.totalWork;
			boolean shouldReport = isLastUpdate || now - this.lastReportTime > ReportTime;

			if (shouldReport) {
				int percent = numDone * 100 / this.totalWork;
				System.out.println(String.format("\tProgress: %3d%%", percent));
				this.lastReportTime = now;
			}
			if (isLastUpdate) {
				double elapsedSeconds = (now - this.startTime) / 1000.0;
				System.out.println(String.format("Finished in %.1f seconds", elapsedSeconds));
			}
		}
	}
}
