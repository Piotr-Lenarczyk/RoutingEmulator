package org.uj.routingemulator.common.exceptions;

import org.uj.routingemulator.common.topology.exceptions.TopologyException;

/**
 * Exception thrown when an action is attempted on an interface that is administratively disabled.
 */
public class InterfaceAdministrativelyDownException extends TopologyException {

	/**
	 * Creates a new interface administratively down exception with the specified message.
	 *
	 * @param message detail message
	 */
	public InterfaceAdministrativelyDownException(String message) {
		super(message);
	}

	/**
	 * Creates a new interface administratively down exception with the specified message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public InterfaceAdministrativelyDownException(String message, Throwable cause) {
		super(message, cause);
	}
}