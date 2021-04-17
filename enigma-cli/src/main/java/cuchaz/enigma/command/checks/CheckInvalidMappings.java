package cuchaz.enigma.command.checks;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.Collection;

public class CheckInvalidMappings implements MappingCheck {
	@Override
	public void findErrors(EnigmaProject project, Collection<CheckFailureException> errors) {
		Collection<Entry<?>> invalidEntries = project.dropMappings(ProgressListener.none());

		for (Entry<?> invalidEntry : invalidEntries) {
			errors.add(new CheckFailureException("Found invalid mapping entry: " + invalidEntry.toString()));
		}
	}
}
