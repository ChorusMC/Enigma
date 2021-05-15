package cuchaz.enigma.source.cfr;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.utils.AsmUtil;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.mapping.MappingFactory;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.relationship.MemberNameResolver;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollectingDumper;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CfrDecompiler implements Decompiler {
    private final DCCommonState state;
    // cfr doesn't add final on params so final setting is ignored
    private SourceSettings settings;

    public CfrDecompiler(ClassProvider classProvider, SourceSettings sourceSettings) {
        Map<String, String> options = new HashMap<>();

        state = new DCCommonState(OptionsImpl.getFactory().create(options), new ClassFileSource2() {
            @Override
            public JarContent addJarContent(String s, AnalysisType analysisType) {
                return null;
            }

            @Override
            public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {

            }

            @Override
            public Collection<String> addJar(String jarPath) {
                return null;
            }

            @Override
            public String getPossiblyRenamedPath(String path) {
                return path;
            }

            @Override
            public Pair<byte[], String> getClassFileContent(String path) {
                ClassNode node = classProvider.get(path.substring(0, path.lastIndexOf('.')));

                if (node == null) {
                    return null;
                }

                return new Pair<>(AsmUtil.nodeToBytes(node), path);
            }
        });

        this.settings = sourceSettings;
    }

    @Override
    public Source getSource(String className, @Nullable EntryRemapper mapper) {
        DCCommonState state = this.state;
        Options options = state.getOptions();

        ObfuscationMapping mapping = MappingFactory.get(options, state);
        state = new DCCommonState(state, mapping);
        ClassFile tree = state.getClassFileMaybePath(className);

        state.configureWith(tree);

        // To make sure we're analysing the cached version
        try {
            tree = state.getClassFile(tree.getClassType());
        } catch (CannotLoadClassException ignored) {
        }

        if (options.getOption(OptionsImpl.DECOMPILE_INNER_CLASSES)) {
            tree.loadInnerClasses(state);
        }

        if (options.getOption(OptionsImpl.RENAME_DUP_MEMBERS)) {
            MemberNameResolver.resolveNames(state, ListFactory.newList(state.getClassCache().getLoadedTypes()));
        }

        TypeUsageCollectingDumper typeUsageCollector = new TypeUsageCollectingDumper(options, tree);
        tree.analyseTop(state, typeUsageCollector);
        return new CfrSource(settings, tree, state, typeUsageCollector.getRealTypeUsageInformation(), options, mapper);
    }

    @Override
    public void setSourceSettings(SourceSettings settings) {
        this.settings = settings;
    }
}
