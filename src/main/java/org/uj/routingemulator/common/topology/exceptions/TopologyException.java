package org.uj.routingemulator.common.topology.exceptions;

import org.uj.routingemulator.common.exceptions.NetworkDomainException;

/**
 * Exception representing failures related to network topology structure, device linkages, or connectivity.
 */
public class TopologyException extends NetworkDomainException {

	/**
	 * Creates a new topology exception with the specified message.
	 *
	 * @param message detail message
	 */
	public TopologyException(String message) {
		super(message);
	}

	/**
	 * Creates a new topology exception with the specified message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public TopologyException(String message, Throwable cause) {
		super(message, cause);
	}
}