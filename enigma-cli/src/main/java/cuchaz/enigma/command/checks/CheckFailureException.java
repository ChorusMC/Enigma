package cuchaz.enigma.command.checks;

public class CheckFailureException extends RuntimeException {
	public CheckFailureException(String message) {
		super(message);
	}
}
