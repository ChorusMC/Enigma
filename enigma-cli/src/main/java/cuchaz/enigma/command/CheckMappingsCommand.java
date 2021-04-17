package cuchaz.enigma.command;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.command.checks.CheckFailureException;
import cuchaz.enigma.command.checks.CheckInvalidMappings;
import cuchaz.enigma.command.checks.CheckNamedSyntheticEntry;
import cuchaz.enigma.command.checks.MappingCheck;
import cuchaz.enigma.command.checks.CheckPackageVisibility;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class CheckMappingsCommand extends Command {

	public CheckMappingsCommand() {
		super("checkmappings");
	}

	@Override
	public String getUsage() {
		return "<in jar> <mappings file>";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 2;
	}

	@Override
	public void run(String... args) throws Exception {
		Path fileJarIn = getReadableFile(getArg(args, 0, "in jar", true)).toPath();
		Path fileMappings = getReadablePath(getArg(args, 1, "mappings file", true));

		Enigma enigma = Enigma.create();

		System.out.println("Reading JAR...");

		EnigmaProject project = enigma.openJar(fileJarIn, new ClasspathClassProvider(), ProgressListener.none());

		System.out.println("Reading mappings...");

		MappingFormat format = chooseEnigmaFormat(fileMappings);
		MappingSaveParameters saveParameters = enigma.getProfile().getMappingSaveParameters();

		EntryTree<EntryMapping> mappings = format.read(fileMappings, ProgressListener.none(), saveParameters);
		project.setMappings(mappings);

		Set<MappingCheck> checks = new HashSet<>();
		checks.add(new CheckPackageVisibility());
		checks.add(new CheckInvalidMappings());
		checks.add(new CheckNamedSyntheticEntry());

		LinkedList<CheckFailureException> errors = new LinkedList<>();
		LinkedList<CheckFailureException> warnings = new LinkedList<>();

		for (MappingCheck check : checks) {
			check.findErrors(project, check.failOnError() ? errors : warnings);
		}

		System.out.printf("%d warnings:\n", warnings.size());
		for (CheckFailureException warning : warnings) {
			System.out.println(warning.getMessage());
		}

		System.out.printf("%d errors:\n", errors.size());
		if (!errors.isEmpty()) {
			for (CheckFailureException error : errors) {
				System.err.println(error.getMessage());
			}

			throw new IllegalStateException("Mappings check failed, see above output");
		}
	}
}
