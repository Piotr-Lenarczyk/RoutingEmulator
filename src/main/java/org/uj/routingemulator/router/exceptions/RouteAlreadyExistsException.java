package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to add a static route that already exists in candidate or active configuration.
 */
public class RouteAlreadyExistsException extends RouterException {

	/**
	 * Creates a new exception with the specified detail message.
	 *
	 * @param message detail message
	 */
	public RouteAlreadyExistsException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the specified detail message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public RouteAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}
}