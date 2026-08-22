package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to add configuration that already exists.
 *
 * @deprecated Replaced by specific exceptions like {@link RouteAlreadyExistsException},
 * {@link InterfaceAddressAlreadyConfiguredException}, {@link RouteAlreadyDisabledException}, or {@link InterfaceAlreadyDisabledException}.
 */
@Deprecated
public class DuplicateConfigurationException extends RouterException {

	/**
	 * Creates a new duplicate configuration exception with the specified message.
	 *
	 * @param message the error message
	 */
	public DuplicateConfigurationException(String message) {
		super(message);
	}

	/**
	 * Creates a new duplicate configuration exception with the specified message and cause.
	 *
	 * @param message the error message
	 * @param cause   underlying cause
	 */
	public DuplicateConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}