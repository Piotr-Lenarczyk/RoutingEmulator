package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to delete an IP address configuration from an interface that has no address assigned.
 */
public class InterfaceAddressNotFoundException extends RouterException {

	/**
	 * Creates a new exception with the specified detail message.
	 *
	 * @param message detail message
	 */
	public InterfaceAddressNotFoundException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the specified detail message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public InterfaceAddressNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}