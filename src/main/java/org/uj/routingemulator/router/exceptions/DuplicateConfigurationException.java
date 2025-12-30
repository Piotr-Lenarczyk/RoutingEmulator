package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to add configuration that already exists.
 * <p>
 * This includes:
 * <ul>
 *   <li>Adding a route that already exists</li>
 *   <li>Configuring an interface with an already configured address</li>
 *   <li>Disabling an already disabled route or interface</li>
 * </ul>
 */
public class DuplicateConfigurationException extends RouterException {

	/**
	 * Creates a new duplicate configuration exception with the specified message.
	 *
	 * @param message the error message
	 */
	public DuplicateConfigurationException(String message) {
		super(message);
	}
}

