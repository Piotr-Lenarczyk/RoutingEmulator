package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to modify, delete, or disable a static route that does not exist.
 */
public class RouteNotFoundException extends RouterException {

	/**
	 * Creates a new exception with the specified detail message.
	 *
	 * @param message detail message
	 */
	public RouteNotFoundException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the specified detail message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public RouteNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}