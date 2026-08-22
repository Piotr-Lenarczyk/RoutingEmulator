package org.uj.routingemulator.common.exceptions;

import org.uj.routingemulator.common.topology.exceptions.TopologyException;

/**
 * Exception thrown when a requested interface is unavailable or not operational.
 */
public class InterfaceUnavailableException extends TopologyException {

	/**
	 * Creates a new interface unavailable exception with default detail message.
	 */
	public InterfaceUnavailableException() {
		super("Interface is unavailable");
	}

	/**
	 * Creates a new interface unavailable exception with the specified message.
	 *
	 * @param message detail message
	 */
	public InterfaceUnavailableException(String message) {
		super(message);
	}

	/**
	 * Creates a new interface unavailable exception with the specified message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public InterfaceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}