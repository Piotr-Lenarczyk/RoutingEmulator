package org.uj.routingemulator.common.exceptions;

/**
 * Base exception for all domain-level exceptions within the network emulator system.
 */
public class NetworkDomainException extends RuntimeException {

	/**
	 * Creates a new exception with the specified message.
	 *
	 * @param message detail message
	 */
	public NetworkDomainException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the specified message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public NetworkDomainException(String message, Throwable cause) {
		super(message, cause);
	}
}