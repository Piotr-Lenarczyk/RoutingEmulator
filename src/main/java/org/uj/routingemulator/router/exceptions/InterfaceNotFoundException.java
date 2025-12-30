package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to configure a non-existent interface.
 * <p>
 * This occurs when the user tries to configure an interface that doesn't
 * exist on the router (e.g., configuring eth5 when the router only has eth0-eth2).
 */
public class InterfaceNotFoundException extends RouterException {

	/**
	 * Creates a new interface not found exception with the specified message.
	 *
	 * @param message the error message
	 */
	public InterfaceNotFoundException(String message) {
		super(message);
	}
}

