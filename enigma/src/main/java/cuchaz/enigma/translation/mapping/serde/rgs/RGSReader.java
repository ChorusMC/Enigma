package cuchaz.enigma.translation.mapping.serde.rgs;

import com.google.common.base.Charsets;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingPair;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingsReader;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum RGSReader implements MappingsReader {
    INSTANCE;

    // Key: obfuscated name, Value: Remapped name
    private final Map<String, String> remappedFields = new HashMap<>();
    private boolean isGeneratedIntermediaries = false;

    @Override
    public EntryTree<EntryMapping> read(Path path, ProgressListener progress, MappingSaveParameters saveParameters) throws MappingParseException, IOException {
        return read(path, Files.readAllLines(path, Charsets.UTF_8), progress);
    }

    private EntryTree<EntryMapping> read(Path path, List<String> lines, ProgressListener progress) throws MappingParseException {
        EntryTree<EntryMapping> mappings = new HashEntryTree<>();

        for (int i = 0; i < lines.size(); i++) {
            progress.step(i, "");
            // This mainly removes empty lines which cause our reader to freak
            String line = lines.get(i).trim().replaceAll("\n\n+", "");

            // This is needed to avoid preprocessor arguments, maybe there is a way to fix this?
            if (line.equals("") || line.startsWith("#") || line.startsWith(".option") || line.startsWith(".class ")) continue;

            try {
                MappingPair<?, EntryMapping> mapping = parseLine(line);
                mappings.insert(mapping.getEntry(), mapping.getMapping());
            } catch (Throwable t) {
                t.printStackTrace();
                throw new MappingParseException(path::toString, i, t.toString());
            }
        }

        return mappings;
    }

    private MappingPair<?, EntryMapping> parseLine(String line) {
        String[] tokens = line.split(" ");
        String key = tokens[0];

        switch (key) {
            case ".class_map" -> { return parseClass(tokens); }
            case ".field_map" -> { return parseField(tokens); }
            case ".method_map" -> { return parseMethod(tokens); }
            // This is for JADs/RetroguardSrc version of intermediary (I.E. func_), our mappings will be a direct translation
            // so everything else will have to be obfuscated, I could possibly "generate" intermediaries, but that's a
            // feature as for now.
            case "### GENERATED MAPPINGS:" -> {
                this.isGeneratedIntermediaries = true;
                return null;
            }
            default -> throw new RuntimeException("Unknown token '" + key + "'!");
        }
    }

    private MappingPair<ClassEntry, EntryMapping> parseClass(String[] tokens) {
        // .class_map ClassA RemappedClassA
        ClassEntry obfuscatedEntry = new ClassEntry(tokens[1]);
        String mapping = tokens[2];

        // TODO: Do inner classes if those even exist in RGS
        return new MappingPair<>(obfuscatedEntry, new EntryMapping(mapping));
    }

    private MappingPair<FieldEntry, EntryMapping> parseField(String[] tokens) {
        // .field_map ClassA/FieldA RemappedFieldA
        String obfuscatedClassAndFieldName = tokens[1];

        int lastIndex = obfuscatedClassAndFieldName.lastIndexOf('/');
        String obfuscatedClass = obfuscatedClassAndFieldName.substring(0, lastIndex);
        String obfuscatedField = obfuscatedClassAndFieldName.substring(lastIndex + 1);

        ClassEntry ownerClass = new ClassEntry(obfuscatedClass);
        FieldEntry obfuscatedEntry = new FieldEntry(ownerClass, obfuscatedField, new TypeDescriptor("LStepSound;"));
        String mapping = tokens[2];

        if (isGeneratedIntermediaries) {
            return new MappingPair<>(
                    new FieldEntry(ownerClass, obfuscatedField, new TypeDescriptor("LStepSound;")),
                    new EntryMapping(this.remappedFields.get(obfuscatedClass))
            );
        }

        this.remappedFields.put(obfuscatedClass, obfuscatedField);
        return new MappingPair<>(obfuscatedEntry, new EntryMapping(mapping));
    }

    private MappingPair<MethodEntry, EntryMapping> parseMethod(String[] tokens) {
        // .method_map ClassA/MethodA ()Z RemappedMethodA
        String obfuscatedClassAndMethodName = tokens[1];

        int lastIndex = obfuscatedClassAndMethodName.lastIndexOf('/');
        String obfuscatedClass = obfuscatedClassAndMethodName.substring(0, lastIndex);
        String obfuscatedMethod = obfuscatedClassAndMethodName.substring(lastIndex + 1);

        ClassEntry ownerClass = new ClassEntry(obfuscatedClass);
        MethodDescriptor ownerDescriptor = new MethodDescriptor(tokens[2]);
        MethodEntry ownerMethod = new MethodEntry(ownerClass, obfuscatedMethod, ownerDescriptor);
        String mapping = tokens[3];
        return new MappingPair<>(ownerMethod, new EntryMapping(mapping));
    }
}
