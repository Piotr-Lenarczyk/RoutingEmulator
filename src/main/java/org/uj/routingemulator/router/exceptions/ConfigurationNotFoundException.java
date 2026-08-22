package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to modify or delete configuration that doesn't exist.
 *
 * @deprecated Replaced by specific exceptions like {@link RouteNotFoundException},
 * {@link InterfaceAddressNotFoundException}, or {@link InterfaceAlreadyEnabledException}.
 */
@Deprecated
public class ConfigurationNotFoundException extends RouterException {

	/**
	 * Creates a new configuration not found exception with the specified message.
	 *
	 * @param message the error message
	 */
	public ConfigurationNotFoundException(String message) {
		super(message);
	}

	/**
	 * Creates a new configuration not found exception with the specified message and cause.
	 *
	 * @param message the error message
	 * @param cause   underlying cause
	 */
	public ConfigurationNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}