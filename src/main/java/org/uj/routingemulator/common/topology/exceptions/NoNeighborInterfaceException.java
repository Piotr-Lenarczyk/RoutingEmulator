package org.uj.routingemulator.common.topology.exceptions;

/**
 * Exception thrown when a neighbor interface cannot be resolved for a connection.
 */
public class NoNeighborInterfaceException extends TopologyException {

	/**
	 * Creates a new no neighbor interface exception with the specified message.
	 *
	 * @param message detail message
	 */
	public NoNeighborInterfaceException(String message) {
		super(message);
	}

	/**
	 * Creates a new no neighbor interface exception with the specified message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public NoNeighborInterfaceException(String message, Throwable cause) {
		super(message, cause);
	}
}