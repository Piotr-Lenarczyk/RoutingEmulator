package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to commit configuration with no pending changes.
 * <p>
 * This occurs when the commit command is issued but there are no staged
 * configuration changes to apply.
 */
public class NoChangesToCommitException extends RouterException {

	/**
	 * Creates a new no changes to commit exception with the specified message.
	 *
	 * @param message the error message
	 */
	public NoChangesToCommitException(String message) {
		super(message);
	}
}

