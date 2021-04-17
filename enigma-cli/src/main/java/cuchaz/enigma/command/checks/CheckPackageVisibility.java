package cuchaz.enigma.command.checks;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class CheckPackageVisibility implements MappingCheck {
	@Override
	public void findErrors(EnigmaProject project, Collection<CheckFailureException> errors) {
		JarIndex idx = project.getJarIndex();

		for (Set<ClassEntry> partition : idx.getPackageVisibilityIndex().getPartitions()) {
			long packages = partition.stream()
					.map(project.getMapper()::deobfuscate)
					.map(ClassEntry::getPackageName)
					.distinct()
					.count();

			if (packages > 1) {
				errors.add(new CheckFailureException("ERROR: Must be in one package:\n" + partition.stream()
						.map(project.getMapper()::deobfuscate)
						.map(ClassEntry::toString)
						.sorted()
						.collect(Collectors.joining("\n"))
				));
			}
		}
	}

	@Override
	public boolean failOnError() {
		return false;
	}
}
