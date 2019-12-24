package cuchaz.enigma.translation.mapping.serde;

import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryMapping;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

final class RawEntryMapping {
	private final @Nullable String targetName;
	private final AccessModifier access;
	private List<String> javadocs = new ArrayList<>();

	RawEntryMapping(@Nullable String targetName) {
		this(targetName, null);
	}

	RawEntryMapping(@Nullable String targetName, AccessModifier access) {
		this.access = access;
		this.targetName = targetName;
	}

	void addJavadocLine(String line) {
		javadocs.add(line);
	}

	EntryMapping bake() {
		return new EntryMapping(targetName, access, javadocs.isEmpty() ? null : String.join("\n", javadocs));
	}
}
