package cuchaz.enigma.command.checks;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.translation.representation.entry.DefEntry;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.Collection;

public class CheckNamedSyntheticEntry implements MappingCheck {
	@Override
	public void findErrors(EnigmaProject project, Collection<CheckFailureException> errors) {
		EntryIndex entryIndex = project.getJarIndex().getEntryIndex();

		check(entryIndex.getClasses(), project, errors);
		check(entryIndex.getMethods(), project, errors);
		check(entryIndex.getFields(), project, errors);
	}

	private void check(Collection<? extends Entry<?>> entries, EnigmaProject project, Collection<CheckFailureException> errors) {
		for (Entry<?> entry : entries) {
			DefEntry<?> defEntry = (DefEntry<?>) entry;
			if (defEntry.getAccess().isSynthetic() && project.getMapper().hasDeobfMapping(entry)) {
				errors.add(new CheckFailureException(String.format("Synthetic entry (%s) has a debof name (%s)", entry, project.getMapper().getDeobfMapping(entry).getTargetName())));
			}
		}
	}
}
