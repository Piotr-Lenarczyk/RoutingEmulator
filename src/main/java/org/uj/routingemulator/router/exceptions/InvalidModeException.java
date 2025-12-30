package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to execute a command in the wrong router mode.
 * <p>
 * For example, trying to execute configuration commands in operational mode,
 * or trying to execute show commands in configuration mode.
 */
public class InvalidModeException extends RouterException {

	/**
	 * Creates a new invalid mode exception with the specified message.
	 *
	 * @param message the error message
	 */
	public InvalidModeException(String message) {
		super(message);
	}
}

