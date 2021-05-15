package cuchaz.enigma.source;

import cuchaz.enigma.translation.mapping.EntryRemapper;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Decompiler {
	@Deprecated // use remapper specific one for easy doc inclusion
	default Source getSource(String className) {
		return getSource(className, null);
	}

	Source getSource(String className, @Nullable EntryRemapper remapper);

	void setSourceSettings(SourceSettings settings);
}
