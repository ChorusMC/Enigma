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

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.Lists;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.api.service.ObfuscationTestService;
import cuchaz.enigma.classhandle.ClassHandle;
import cuchaz.enigma.classhandle.ClassHandleProvider;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.gui.config.NetConfig;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.dialog.ProgressDialog;
import cuchaz.enigma.gui.stats.StatsGenerator;
import cuchaz.enigma.gui.stats.StatsMember;
import cuchaz.enigma.gui.util.History;
import cuchaz.enigma.network.*;
import cuchaz.enigma.network.packet.LoginC2SPacket;
import cuchaz.enigma.network.packet.Packet;
import cuchaz.enigma.source.DecompiledClassSource;
import cuchaz.enigma.source.DecompilerService;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.*;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Utils;
import cuchaz.enigma.utils.validation.ValidationContext;

public class GuiController implements ClientPacketHandler {
	private final Gui gui;
	public final Enigma enigma;

	public EnigmaProject project;
	private IndexTreeBuilder indexTreeBuilder;

	private Path loadedMappingPath;
	private MappingFormat loadedMappingFormat;

	private ClassHandleProvider chp;

	private ClassHandle tokenHandle;

	private EnigmaClient client;
	private EnigmaServer server;

	public GuiController(Gui gui, EnigmaProfile profile) {
		this.gui = gui;
		this.enigma = Enigma.builder()
				.setProfile(profile)
				.build();
	}

	public boolean isDirty() {
		return project != null && project.getMapper().isDirty();
	}

	public CompletableFuture<Void> openJar(final Path jarPath) {
		this.gui.onStartOpenJar();

		return ProgressDialog.runOffThread(gui.getFrame(), progress -> {
			project = enigma.openJar(jarPath, new ClasspathClassProvider(), progress);
			indexTreeBuilder = new IndexTreeBuilder(project.getJarIndex());
			chp = new ClassHandleProvider(project, UiConfig.getDecompiler().service);
			SwingUtilities.invokeLater(() -> {
				gui.onFinishOpenJar(jarPath.getFileName().toString());
				refreshClasses();
			});
		});
	}

	public void closeJar() {
		this.chp.destroy();
		this.chp = null;
		this.project = null;
		this.gui.onCloseJar();
	}

	public CompletableFuture<Void> openMappings(MappingFormat format, Path path) {
		if (project == null) return CompletableFuture.completedFuture(null);

		gui.setMappingsFile(path);

		return ProgressDialog.runOffThread(gui.getFrame(), progress -> {
			try {
				MappingSaveParameters saveParameters = enigma.getProfile().getMappingSaveParameters();

				EntryTree<EntryMapping> mappings = format.read(path, progress, saveParameters);
				project.setMappings(mappings);

				loadedMappingFormat = format;
				loadedMappingPath = path;

				refreshClasses();
				chp.invalidateJavadoc();
			} catch (MappingParseException e) {
				JOptionPane.showMessageDialog(gui.getFrame(), e.getMessage());
			}
		});
	}

	@Override
	public void openMappings(EntryTree<EntryMapping> mappings) {
		if (project == null) return;

		project.setMappings(mappings);
		refreshClasses();
		chp.invalidateJavadoc();
	}

	public CompletableFuture<Void> saveMappings(Path path) {
		return saveMappings(path, loadedMappingFormat);
	}

	public CompletableFuture<Void> saveMappings(Path path, MappingFormat format) {
		if (project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> {
			EntryRemapper mapper = project.getMapper();
			MappingSaveParameters saveParameters = enigma.getProfile().getMappingSaveParameters();

			MappingDelta<EntryMapping> delta = mapper.takeMappingDelta();
			boolean saveAll = !path.equals(loadedMappingPath);

			loadedMappingFormat = format;
			loadedMappingPath = path;

			if (saveAll) {
				format.write(mapper.getObfToDeobf(), path, progress, saveParameters);
			} else {
				format.write(mapper.getObfToDeobf(), delta, path, progress, saveParameters);
			}
		});
	}

	public void closeMappings() {
		if (project == null) return;

		project.setMappings(null);

		this.gui.setMappingsFile(null);
		refreshClasses();
		chp.invalidateJavadoc();
	}

	public void reloadAll() {
		Path jarPath = this.project.getJarPath();
		MappingFormat loadedMappingFormat = this.loadedMappingFormat;
		Path loadedMappingPath = this.loadedMappingPath;
		if (jarPath != null) {
			this.closeJar();
			CompletableFuture<Void> f = this.openJar(jarPath);
			if (loadedMappingFormat != null && loadedMappingPath != null) {
				f.whenComplete((v, t) -> this.openMappings(loadedMappingFormat, loadedMappingPath));
			}
		}
	}

	public void reloadMappings() {
		MappingFormat loadedMappingFormat = this.loadedMappingFormat;
		Path loadedMappingPath = this.loadedMappingPath;
		if (loadedMappingFormat != null && loadedMappingPath != null) {
			this.closeMappings();
			this.openMappings(loadedMappingFormat, loadedMappingPath);
		}
	}

	public CompletableFuture<Void> dropMappings() {
		if (project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> project.dropMappings(progress));
	}

	public CompletableFuture<Void> exportSource(final Path path) {
		if (project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> {
			EnigmaProject.JarExport jar = project.exportRemappedJar(progress);
			jar.decompileStream(progress, chp.getDecompilerService(), EnigmaProject.DecompileErrorStrategy.TRACE_AS_SOURCE)
					.forEach(source -> {
						try {
							source.writeTo(source.resolvePath(path));
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
		});
	}

	public CompletableFuture<Void> exportJar(final Path path) {
		if (project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> {
			EnigmaProject.JarExport jar = project.exportRemappedJar(progress);
			jar.write(path, progress);
		});
	}

	public void setTokenHandle(ClassHandle handle) {
		if (tokenHandle != null) {
			tokenHandle.close();
		}

		tokenHandle = handle;
	}

	public ClassHandle getTokenHandle() {
		return tokenHandle;
	}

	public ReadableToken getReadableToken(Token token) {
		if (tokenHandle == null) {
			return null;
		}

		try {
			return tokenHandle.getSource().get()
					.map(DecompiledClassSource::getIndex)
					.map(index -> new ReadableToken(
							index.getLineNumber(token.start),
							index.getColumnNumber(token.start),
							index.getColumnNumber(token.end)))
					.unwrapOr(null);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Navigates to the declaration with respect to navigation history
	 *
	 * @param entry the entry whose declaration will be navigated to
	 */
	public void openDeclaration(Entry<?> entry) {
		if (entry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}
		openReference(EntryReference.declaration(entry, entry.getName()));
	}

	/**
	 * Navigates to the reference with respect to navigation history
	 *
	 * @param reference the reference
	 */
	public void openReference(EntryReference<Entry<?>, Entry<?>> reference) {
		if (reference == null) {
			throw new IllegalArgumentException("Reference cannot be null!");
		}
		if (this.gui.referenceHistory == null) {
			this.gui.referenceHistory = new History<>(reference);
		} else {
			if (!reference.equals(this.gui.referenceHistory.getCurrent())) {
				this.gui.referenceHistory.push(reference);
			}
		}
		setReference(reference);
	}

	/**
	 * Navigates to the reference without modifying history. If the class is not currently loaded, it will be loaded.
	 *
	 * @param reference the reference
	 */
	private void setReference(EntryReference<Entry<?>, Entry<?>> reference) {
		gui.openClass(reference.getLocationClassEntry().getOutermostClass()).showReference(reference);
	}

	public Collection<Token> getTokensForReference(DecompiledClassSource source, EntryReference<Entry<?>, Entry<?>> reference) {
		EntryRemapper mapper = this.project.getMapper();

		SourceIndex index = source.getIndex();
		return mapper.getObfResolver().resolveReference(reference, ResolutionStrategy.RESOLVE_CLOSEST)
				.stream()
				.flatMap(r -> index.getReferenceTokens(r).stream())
				.collect(Collectors.toList());
	}

	public void openPreviousReference() {
		if (hasPreviousReference()) {
			setReference(gui.referenceHistory.goBack());
		}
	}

	public boolean hasPreviousReference() {
		return gui.referenceHistory != null && gui.referenceHistory.canGoBack();
	}

	public void openNextReference() {
		if (hasNextReference()) {
			setReference(gui.referenceHistory.goForward());
		}
	}

	public boolean hasNextReference() {
		return gui.referenceHistory != null && gui.referenceHistory.canGoForward();
	}

	public void navigateTo(Entry<?> entry) {
		if (!project.isRenamable(entry)) {
			// entry is not in the jar. Ignore it
			return;
		}
		openDeclaration(entry);
	}

	public void navigateTo(EntryReference<Entry<?>, Entry<?>> reference) {
		if (!project.isRenamable(reference.getLocationClassEntry())) {
			return;
		}
		openReference(reference);
	}

	private void refreshClasses() {
		List<ClassEntry> obfClasses = Lists.newArrayList();
		List<ClassEntry> deobfClasses = Lists.newArrayList();
		this.addSeparatedClasses(obfClasses, deobfClasses);
		this.gui.setObfClasses(obfClasses);
		this.gui.setDeobfClasses(deobfClasses);
	}

	public void addSeparatedClasses(List<ClassEntry> obfClasses, List<ClassEntry> deobfClasses) {
		EntryRemapper mapper = project.getMapper();

		Collection<ClassEntry> classes = project.getJarIndex().getEntryIndex().getClasses();
		Stream<ClassEntry> visibleClasses = classes.stream()
				.filter(entry -> !entry.isInnerClass());

		visibleClasses.forEach(entry -> {
			ClassEntry deobfEntry = mapper.deobfuscate(entry);

			List<ObfuscationTestService> obfService = enigma.getServices().get(ObfuscationTestService.TYPE);
			boolean obfuscated = deobfEntry.equals(entry);

			if (obfuscated && !obfService.isEmpty()) {
				if (obfService.stream().anyMatch(service -> service.testDeobfuscated(entry))) {
					obfuscated = false;
				}
			}

			if (obfuscated) {
				obfClasses.add(entry);
			} else {
				deobfClasses.add(entry);
			}
		});
	}

	public void onModifierChanged(ValidationContext vc, Entry<?> entry, AccessModifier modifier) {
		EntryRemapper mapper = project.getMapper();

		EntryMapping mapping = mapper.getDeobfMapping(entry);
		if (mapping != null) {
			mapper.mapFromObf(vc, entry, new EntryMapping(mapping.getTargetName(), modifier));
		} else {
			mapper.mapFromObf(vc, entry, new EntryMapping(entry.getName(), modifier));
		}

		chp.invalidateMapped();
	}

	public StructureTreeNode getClassStructure(ClassEntry entry, boolean hideDeobfuscated) {
		StructureTreeNode rootNode = new StructureTreeNode(this.project, entry, entry);
		rootNode.load(this.project, hideDeobfuscated);
		return rootNode;
	}

	public ClassInheritanceTreeNode getClassInheritance(ClassEntry entry) {
		Translator translator = project.getMapper().getDeobfuscator();
		ClassInheritanceTreeNode rootNode = indexTreeBuilder.buildClassInheritance(translator, entry);
		return ClassInheritanceTreeNode.findNode(rootNode, entry);
	}

	public ClassImplementationsTreeNode getClassImplementations(ClassEntry entry) {
		Translator translator = project.getMapper().getDeobfuscator();
		return this.indexTreeBuilder.buildClassImplementations(translator, entry);
	}

	public MethodInheritanceTreeNode getMethodInheritance(MethodEntry entry) {
		Translator translator = project.getMapper().getDeobfuscator();
		MethodInheritanceTreeNode rootNode = indexTreeBuilder.buildMethodInheritance(translator, entry);
		return MethodInheritanceTreeNode.findNode(rootNode, entry);
	}

	public MethodImplementationsTreeNode getMethodImplementations(MethodEntry entry) {
		Translator translator = project.getMapper().getDeobfuscator();
		List<MethodImplementationsTreeNode> rootNodes = indexTreeBuilder.buildMethodImplementations(translator, entry);
		if (rootNodes.isEmpty()) {
			return null;
		}
		if (rootNodes.size() > 1) {
			System.err.println("WARNING: Method " + entry + " implements multiple interfaces. Only showing first one.");
		}
		return MethodImplementationsTreeNode.findNode(rootNodes.get(0), entry);
	}

	public ClassReferenceTreeNode getClassReferences(ClassEntry entry) {
		Translator deobfuscator = project.getMapper().getDeobfuscator();
		ClassReferenceTreeNode rootNode = new ClassReferenceTreeNode(deobfuscator, entry);
		rootNode.load(project.getJarIndex(), true);
		return rootNode;
	}

	public FieldReferenceTreeNode getFieldReferences(FieldEntry entry) {
		Translator translator = project.getMapper().getDeobfuscator();
		FieldReferenceTreeNode rootNode = new FieldReferenceTreeNode(translator, entry);
		rootNode.load(project.getJarIndex(), true);
		return rootNode;
	}

	public MethodReferenceTreeNode getMethodReferences(MethodEntry entry, boolean recursive) {
		Translator translator = project.getMapper().getDeobfuscator();
		MethodReferenceTreeNode rootNode = new MethodReferenceTreeNode(translator, entry);
		rootNode.load(project.getJarIndex(), true, recursive);
		return rootNode;
	}

	@Override
	public void rename(ValidationContext vc, EntryReference<Entry<?>, Entry<?>> reference, String newName, boolean refreshClassTree) {
		rename(vc, reference, newName, refreshClassTree, false);
	}

	public void rename(ValidationContext vc, EntryReference<Entry<?>, Entry<?>> reference, String newName, boolean refreshClassTree, boolean validateOnly) {
		Entry<?> entry = reference.getNameableEntry();
		EntryMapping previous = project.getMapper().getDeobfMapping(entry);
		project.getMapper().mapFromObf(vc, entry, previous != null ? previous.withName(newName) : new EntryMapping(newName), true, validateOnly);
		gui.showStructure(gui.getActiveEditor());

		if (validateOnly || !vc.canProceed()) return;

		if (refreshClassTree && reference.entry instanceof ClassEntry && !((ClassEntry) reference.entry).isInnerClass())
			this.gui.moveClassTree(reference.entry, newName);

		chp.invalidateMapped();
	}

	@Override
	public void removeMapping(ValidationContext vc, EntryReference<Entry<?>, Entry<?>> reference) {
		project.getMapper().removeByObf(vc, reference.getNameableEntry());
		gui.showStructure(gui.getActiveEditor());

		if (!vc.canProceed()) return;

		if (reference.entry instanceof ClassEntry)
			this.gui.moveClassTree(reference.entry, false, true);

		chp.invalidateMapped();
	}

	@Override
	public void changeDocs(ValidationContext vc, EntryReference<Entry<?>, Entry<?>> reference, String updatedDocs) {
		changeDocs(vc, reference, updatedDocs, false);
	}

	public void changeDocs(ValidationContext vc, EntryReference<Entry<?>, Entry<?>> reference, String updatedDocs, boolean validateOnly) {
		changeDoc(vc, reference.entry, updatedDocs, validateOnly);

		if (validateOnly || !vc.canProceed()) return;

		chp.invalidateJavadoc(reference.getLocationClassEntry());
	}

	private void changeDoc(ValidationContext vc, Entry<?> obfEntry, String newDoc, boolean validateOnly) {
		EntryRemapper mapper = project.getMapper();

		EntryMapping deobfMapping = mapper.getDeobfMapping(obfEntry);
		if (deobfMapping == null) {
			deobfMapping = new EntryMapping(mapper.deobfuscate(obfEntry).getName());
		}

		mapper.mapFromObf(vc, obfEntry, deobfMapping.withDocs(newDoc), false, validateOnly);
	}

	@Override
	public void markAsDeobfuscated(ValidationContext vc, EntryReference<Entry<?>, Entry<?>> reference) {
		EntryRemapper mapper = project.getMapper();
		Entry<?> entry = reference.getNameableEntry();
		mapper.mapFromObf(vc, entry, new EntryMapping(mapper.deobfuscate(entry).getName()));
		gui.showStructure(gui.getActiveEditor());

		if (!vc.canProceed()) return;

		if (reference.entry instanceof ClassEntry && !((ClassEntry) reference.entry).isInnerClass())
			this.gui.moveClassTree(reference.entry, true, false);

		chp.invalidateMapped();
	}

	public void openStats(Set<StatsMember> includedMembers, String topLevelPackage, boolean includeSynthetic) {
		ProgressDialog.runOffThread(gui.getFrame(), progress -> {
			String data = new StatsGenerator(project).generate(progress, includedMembers, topLevelPackage, includeSynthetic).getTreeJson();

			try {
				File statsFile = File.createTempFile("stats", ".html");

				try (FileWriter w = new FileWriter(statsFile)) {
					w.write(
							Utils.readResourceToString("/stats.html")
									.replace("/*data*/", data)
					);
				}

				Desktop.getDesktop().open(statsFile);
			} catch (IOException e) {
				throw new Error(e);
			}
		});
	}

	public void setDecompiler(DecompilerService service) {
		if (chp != null) {
			chp.setDecompilerService(service);
			chp.setDecompilerRemoveImports(!UiConfig.shouldShowImports());
		}
	}

	public ClassHandleProvider getClassHandleProvider() {
		return chp;
	}

	public EnigmaClient getClient() {
		return client;
	}

	public EnigmaServer getServer() {
		return server;
	}

	public void createClient(String username, String ip, int port, char[] password) throws IOException {
		client = new EnigmaClient(this, ip, port);
		client.connect();
		client.sendPacket(new LoginC2SPacket(project.getJarChecksum(), password, username));
		gui.setConnectionState(ConnectionState.CONNECTED);
	}

	public void createServer(int port, char[] password) throws IOException {
		server = new IntegratedEnigmaServer(project.getJarChecksum(), password, EntryRemapper.mapped(project.getJarIndex(), new HashEntryTree<>(project.getMapper().getObfToDeobf())), port);
		server.start();
		client = new EnigmaClient(this, "127.0.0.1", port);
		client.connect();
		client.sendPacket(new LoginC2SPacket(project.getJarChecksum(), password, NetConfig.getUsername()));
		gui.setConnectionState(ConnectionState.HOSTING);
	}

	@Override
	public synchronized void disconnectIfConnected(String reason) {
		if (client == null && server == null) {
			return;
		}

		if (client != null) {
			client.disconnect();
		}
		if (server != null) {
			server.stop();
		}
		client = null;
		server = null;
		SwingUtilities.invokeLater(() -> {
			if (reason != null) {
				JOptionPane.showMessageDialog(gui.getFrame(), I18n.translate(reason), I18n.translate("disconnect.disconnected"), JOptionPane.INFORMATION_MESSAGE);
			}
			gui.setConnectionState(ConnectionState.NOT_CONNECTED);
		});
	}

	@Override
	public void sendPacket(Packet<ServerPacketHandler> packet) {
		if (client != null) {
			client.sendPacket(packet);
		}
	}

	@Override
	public void addMessage(Message message) {
		gui.addMessage(message);
	}

	@Override
	public void updateUserList(List<String> users) {
		gui.setUserList(users);
	}

}
