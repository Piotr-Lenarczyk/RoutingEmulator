package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to modify or delete configuration that doesn't exist.
 * <p>
 * This includes:
 * <ul>
 *   <li>Deleting a route that doesn't exist</li>
 *   <li>Deleting an interface address that isn't configured</li>
 *   <li>Disabling a route that doesn't exist</li>
 *   <li>Enabling an interface that is already enabled</li>
 * </ul>
 */
public class ConfigurationNotFoundException extends RouterException {

	/**
	 * Creates a new configuration not found exception with the specified message.
	 *
	 * @param message the error message
	 */
	public ConfigurationNotFoundException(String message) {
		super(message);
	}
}

