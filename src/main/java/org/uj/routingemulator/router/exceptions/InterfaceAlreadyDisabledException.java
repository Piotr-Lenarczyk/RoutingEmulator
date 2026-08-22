package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to administratively disable an interface that is already disabled.
 */
public class InterfaceAlreadyDisabledException extends RouterException {

	/**
	 * Creates a new exception with the specified detail message.
	 *
	 * @param message detail message
	 */
	public InterfaceAlreadyDisabledException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the specified detail message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public InterfaceAlreadyDisabledException(String message, Throwable cause) {
		super(message, cause);
	}
}