package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to assign an IP address to an interface that already has that exact configuration.
 */
public class InterfaceAddressAlreadyConfiguredException extends RouterException {

	/**
	 * Creates a new exception with the specified detail message.
	 *
	 * @param message detail message
	 */
	public InterfaceAddressAlreadyConfiguredException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the specified detail message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public InterfaceAddressAlreadyConfiguredException(String message, Throwable cause) {
		super(message, cause);
	}
}