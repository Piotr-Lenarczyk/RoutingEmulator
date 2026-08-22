package org.uj.routingemulator.router.exceptions;

import org.uj.routingemulator.common.exceptions.NetworkDomainException;

/**
 * Base exception for all router-related errors.
 * <p>
 * All router exceptions extend this class to provide a common hierarchy
 * for exception handling.
 */
public class RouterException extends NetworkDomainException {

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
	 * @param cause   underlying cause
	 */
	public RouterException(String message, Throwable cause) {
		super(message, cause);
	}
}