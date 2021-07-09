package cuchaz.enigma.translation.mapping.serde.rgs;

import com.google.common.collect.Lists;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.VoidEntryResolver;
import cuchaz.enigma.translation.mapping.serde.LfPrintWriter;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public enum RGSWriter implements MappingsWriter {
    INSTANCE;

    private Collection<Entry<?>> sorted(Iterable<? extends Entry<?>> iterable) {
        ArrayList<Entry<?>> sorted = Lists.newArrayList(iterable);
        sorted.sort(Comparator.comparing(Entry::getName));
        return sorted;
    }

    @Override
    public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
        try {
            Files.deleteIfExists(path);
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> classLines = new ArrayList<>();
        List<String> fieldLines = new ArrayList<>();
        List<String> methodLines = new ArrayList<>();

        List<? extends Entry<?>> rootEntries = Lists.newArrayList(mappings).stream()
                .map(EntryTreeNode::getEntry)
                .toList();
        progress.init(rootEntries.size(), I18n.translate("progress.mappings.rgs_file.generating"));

        int steps = 0;
        for (Entry<?> entry : sorted(rootEntries)) {
            progress.step(steps++, entry.getName());
            writeEntry(classLines, fieldLines, methodLines, mappings, entry);
        }

        progress.init(3, I18n.translate("progress.mappings.rgs_file.writing"));
        try (PrintWriter writer = new LfPrintWriter(Files.newBufferedWriter(path))) {
            // Preprocessor arguments, Alpha 1.1.2_01 RGS parser freaks if these do not exist
            writer.write(".option Application\n");
            writer.write(".class paulscode/sound/** protected\n");
            writer.write(".class com/jcraft/** protected\n");
            writer.write(".class *\n");
            writer.write(".class net/minecraft/**\n");

            progress.step(0, I18n.translate("type.classes"));
            classLines.forEach(writer::println);
            progress.step(1, I18n.translate("type.fields"));
            fieldLines.forEach(writer::println);
            progress.step(2, I18n.translate("type.methods"));
            methodLines.forEach(writer::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void writeEntry(List<String> classes, List<String> fields, List<String> methods, EntryTree<EntryMapping> mappings, Entry<?> entry) {
        EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
        if (node == null) {
            return;
        }

        Translator translator = new MappingTranslator(mappings, VoidEntryResolver.INSTANCE);
        if (entry instanceof ClassEntry classEntry) {
            // RGS parser freaks if any of these classes are in the output.
            if (!classEntry.getFullName().contains("paulscode") && !classEntry.getName().equals("net/minecraft/client/MinecraftApplet") && !classEntry.getName().equals("net/minecraft/isom/IsomPreviewApplet") && !classEntry.getName().equals("net/minecraft/client/Minecraft")) {
                classes.add(generateClassLine(classEntry, translator));
            }
        } else if (entry instanceof FieldEntry fieldEntry) {
            fields.add(generateFieldLine(fieldEntry, translator));
        } else if (entry instanceof MethodEntry methodEntry) {
            // RGS parser doesn't like constructors being mapped like methods as <init>
            if (!entry.getName().contains("<init>")) {
                methods.add(generateMethodLine(methodEntry, translator));
            }
        }

        for (Entry<?> child : sorted(node.getChildren())) {
            writeEntry(classes, fields, methods, mappings, child);
        }
    }

    private String generateClassLine(ClassEntry entry, Translator translator) {
        ClassEntry targetEntry = translator.translate(entry);
        return ".class_map " + entry.getFullName().replaceAll("\\.", "/") + " " + targetEntry.getName();
    }

    private String generateFieldLine(FieldEntry entry, Translator translator) {
        FieldEntry targetEntry = translator.translate(entry);
        return ".field_map " + entry.getFullName().replaceAll("\\.", "/") + " " + targetEntry.getName();
    }

    private String generateMethodLine(MethodEntry entry, Translator translator) {
        MethodEntry targetEntry = translator.translate(entry);
        return ".method_map " + entry.getFullName().replaceAll("\\.", "/") + " " + entry.getDesc() + " " + targetEntry.getName();
    }
}
