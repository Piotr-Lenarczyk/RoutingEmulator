package org.uj.routingemulator.router.exceptions;

/**
 * Base exception for all router-related errors.
 * <p>
 * All router exceptions extend this class to provide a common hierarchy
 * for exception handling.
 */
public class RouterException extends RuntimeException {

	/**
	 * Creates a new router exception with the specified message.
	 *
	 * @param message the error message
	 */
	public RouterException(String message) {
		super(message);
	}

	/**
	 * Creates a new router exception with the specified message and cause.
	 *
	 * @param message the error message
	 * @param cause the cause of the exception
	 */
	public RouterException(String message, Throwable cause) {
		super(message, cause);
	}
}

