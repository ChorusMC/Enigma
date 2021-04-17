package cuchaz.enigma.command.checks;

import cuchaz.enigma.EnigmaProject;

import java.util.Collection;

public interface MappingCheck {
	void findErrors(EnigmaProject project, Collection<CheckFailureException> errors);

	default boolean failOnError() {
		return true;
	}
}
