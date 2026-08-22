package org.uj.routingemulator.common.topology.exceptions;

/**
 * Exception thrown when attempting to establish a connection between two interfaces that are already connected.
 */
public class DuplicateConnectionException extends TopologyException {

	/**
	 * Creates a new duplicate connection exception with the specified message.
	 *
	 * @param message detail message
	 */
	public DuplicateConnectionException(String message) {
		super(message);
	}

	/**
	 * Creates a new duplicate connection exception with the specified message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public DuplicateConnectionException(String message, Throwable cause) {
		super(message, cause);
	}
}