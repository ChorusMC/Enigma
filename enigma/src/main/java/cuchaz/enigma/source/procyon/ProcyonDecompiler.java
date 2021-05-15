package cuchaz.enigma.source.procyon;

import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.java.BraceStyle;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.InsertParenthesesVisitor;
import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.source.procyon.transformers.*;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.utils.AsmUtil;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.tree.ClassNode;

public class ProcyonDecompiler implements Decompiler {
	private SourceSettings settings;
	private final DecompilerSettings decompilerSettings;
	private final MetadataSystem metadataSystem;

	public ProcyonDecompiler(ClassProvider classProvider, SourceSettings settings) {
		ITypeLoader typeLoader = (name, buffer) -> {
			ClassNode node = classProvider.get(name);

			if (node == null) {
				return false;
			}

			byte[] data = AsmUtil.nodeToBytes(node);
			buffer.reset(data.length);
			System.arraycopy(data, 0, buffer.array(), buffer.position(), data.length);
			buffer.position(0);
			return true;
		};

		metadataSystem = new MetadataSystem(typeLoader);
		metadataSystem.setEagerMethodLoadingEnabled(true);

		decompilerSettings = DecompilerSettings.javaDefaults();
		decompilerSettings.setMergeVariables(getSystemPropertyAsBoolean("enigma.mergeVariables", true));
		decompilerSettings.setForceExplicitImports(getSystemPropertyAsBoolean("enigma.forceExplicitImports", true));
		decompilerSettings.setForceExplicitTypeArguments(getSystemPropertyAsBoolean("enigma.forceExplicitTypeArguments", true));
		decompilerSettings.setShowDebugLineNumbers(getSystemPropertyAsBoolean("enigma.showDebugLineNumbers", false));
		decompilerSettings.setShowSyntheticMembers(getSystemPropertyAsBoolean("enigma.showSyntheticMembers", false));
		decompilerSettings.setTypeLoader(typeLoader);

		JavaFormattingOptions formattingOptions = decompilerSettings.getJavaFormattingOptions();
		formattingOptions.ClassBraceStyle = BraceStyle.EndOfLine;
		formattingOptions.InterfaceBraceStyle = BraceStyle.EndOfLine;
		formattingOptions.EnumBraceStyle = BraceStyle.EndOfLine;

		this.settings = settings;
	}

	@Override
	public Source getSource(String className, @Nullable EntryRemapper remapper) {
		TypeReference type = metadataSystem.lookupType(className);
		if (type == null) {
			throw new Error(String.format("Unable to find desc: %s", className));
		}

		TypeDefinition resolvedType = type.resolve();

		DecompilerContext context = new DecompilerContext();
		context.setCurrentType(resolvedType);
		context.setSettings(decompilerSettings);

		AstBuilder builder = new AstBuilder(context);
		builder.addType(resolvedType);
		builder.runTransformations(null);
		CompilationUnit source = builder.getCompilationUnit();

		new ObfuscatedEnumSwitchRewriterTransform(context).run(source);
		new VarargsFixer(context).run(source);
		new RemoveObjectCasts(context).run(source);
		new Java8Generics().run(source);
		new InvalidIdentifierFix().run(source);
		if (settings.removeImports) DropImportAstTransform.INSTANCE.run(source);
		if (settings.removeVariableFinal) DropVarModifiersAstTransform.INSTANCE.run(source);
		source.acceptVisitor(new InsertParenthesesVisitor(), null);

		if (remapper != null) {
			new AddJavadocsAstTransform(remapper).run(source);
		}

		return new ProcyonSource(source, decompilerSettings);
	}

	@Override
	public void setSourceSettings(SourceSettings settings) {
		this.settings = settings;
	}

	private static boolean getSystemPropertyAsBoolean(String property, boolean defValue) {
		String value = System.getProperty(property);
		return value == null ? defValue : Boolean.parseBoolean(value);
	}
}
