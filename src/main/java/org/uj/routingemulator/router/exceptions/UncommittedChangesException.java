package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when the router cannot exit configuration mode due to uncommitted changes.
 * <p>
 * This prevents accidental loss of configuration changes. The user must either
 * commit or discard the changes before exiting configuration mode.
 */
public class UncommittedChangesException extends RouterException {

	/**
	 * Creates a new uncommitted changes exception with the specified message.
	 *
	 * @param message the error message
	 */
	public UncommittedChangesException(String message) {
		super(message);
	}
}

