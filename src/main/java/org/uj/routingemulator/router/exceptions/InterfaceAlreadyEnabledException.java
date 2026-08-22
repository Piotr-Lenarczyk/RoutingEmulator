package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to administratively enable an interface that is already enabled.
 */
public class InterfaceAlreadyEnabledException extends RouterException {

	/**
	 * Creates a new exception with the specified detail message.
	 *
	 * @param message detail message
	 */
	public InterfaceAlreadyEnabledException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the specified detail message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public InterfaceAlreadyEnabledException(String message, Throwable cause) {
		super(message, cause);
	}
}