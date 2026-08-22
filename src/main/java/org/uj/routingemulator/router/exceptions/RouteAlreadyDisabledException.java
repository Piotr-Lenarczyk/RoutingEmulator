package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to disable a static route that is already disabled.
 */
public class RouteAlreadyDisabledException extends RouterException {

	/**
	 * Creates a new exception with the specified detail message.
	 *
	 * @param message detail message
	 */
	public RouteAlreadyDisabledException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the specified detail message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public RouteAlreadyDisabledException(String message, Throwable cause) {
		super(message, cause);
	}
}