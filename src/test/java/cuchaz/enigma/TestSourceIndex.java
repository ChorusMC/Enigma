/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma;

import com.google.common.collect.Sets;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import org.junit.Test;

import java.io.File;
import java.util.Set;
import java.util.jar.JarFile;

public class TestSourceIndex {
	@Test
	public void indexEverything()
		throws Exception {
		// Figure out where Minecraft is...
		final String mcDir = System.getProperty("enigma.test.minecraftdir");
		File mcJar = null;
		if (mcDir == null) {
			String osname = System.getProperty("os.name").toLowerCase();
			if (osname.contains("nix") || osname.contains("nux") || osname.contains("solaris")) {
				mcJar = new File(System.getProperty("user.home"), ".minecraft/versions/1.8.3/1.8.3.jar");
			} else if (osname.contains("mac") || osname.contains("darwin")) {
				mcJar = new File(System.getProperty("user.home"), "Library/Application Support/minecraft/versions/1.8.3/1.8.3.jar");
			} else if (osname.contains("win")) {
				mcJar = new File(System.getenv("AppData"), ".minecraft/versions/1.8.3/1.8.3.jar");
			}
		} else {
			mcJar = new File(mcDir, "versions/1.8.3/1.8.3.jar");
		}

		Deobfuscator deobfuscator = new Deobfuscator(new JarFile(mcJar));

		// get all classes that aren't inner classes
		Set<ClassEntry> classEntries = Sets.newHashSet();
		for (ClassEntry obfClassEntry : deobfuscator.getJarIndex().getEntryIndex().getClasses()) {
			if (!obfClassEntry.isInnerClass()) {
				classEntries.add(obfClassEntry);
			}
		}

		SourceProvider sourceProvider = deobfuscator.getObfSourceProvider();
		for (ClassEntry obfClassEntry : classEntries) {
			try {
				CompilationUnit tree = sourceProvider.getSources(obfClassEntry.getName());
				String source = sourceProvider.writeSourceToString(tree);

				SourceIndex.buildIndex(source, tree, true);
			} catch (Throwable t) {
				throw new Error("Unable to index " + obfClassEntry, t);
			}
		}
	}
}
