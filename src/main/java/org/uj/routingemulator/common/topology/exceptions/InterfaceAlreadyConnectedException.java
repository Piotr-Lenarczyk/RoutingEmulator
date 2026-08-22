package org.uj.routingemulator.common.topology.exceptions;

/**
 * Exception thrown when an interface is already bound to another connection in the topology.
 */
public class InterfaceAlreadyConnectedException extends TopologyException {

	/**
	 * Creates a new interface already connected exception with the specified message.
	 *
	 * @param message detail message
	 */
	public InterfaceAlreadyConnectedException(String message) {
		super(message);
	}

	/**
	 * Creates a new interface already connected exception with the specified message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public InterfaceAlreadyConnectedException(String message, Throwable cause) {
		super(message, cause);
	}
}