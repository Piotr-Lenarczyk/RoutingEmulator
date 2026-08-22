package org.uj.routingemulator.common.exceptions;

/**
 * Exception representing failures encountered during system or device configuration processing.
 */
public class ConfigurationException extends NetworkDomainException {

	/**
	 * Creates a new configuration exception with the specified message.
	 *
	 * @param message detail message
	 */
	public ConfigurationException(String message) {
		super(message);
	}

	/**
	 * Creates a new configuration exception with the specified message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}