package org.uj.routingemulator.router.exceptions;

import org.uj.routingemulator.common.addressing.InterfaceAddress;

/**
 * Exception thrown when attempting to configure an invalid IP address on an interface.
 */
public class InvalidAddressException extends RouterException {

	private final Reason reason;
	private final InterfaceAddress interfaceAddress;
	/**
	 * Creates a new invalid address exception with the specified message.
	 *
	 * @param message the error message
	 */
	public InvalidAddressException(String message) {
		super(message);
		this.reason = Reason.INVALID_FORMAT;
		this.interfaceAddress = null;
	}

	/**
	 * Creates a new invalid address exception with the specified message and cause.
	 *
	 * @param message the error message
	 * @param cause   underlying cause
	 */
	public InvalidAddressException(String message, Throwable cause) {
		super(message, cause);
		this.reason = Reason.INVALID_FORMAT;
		this.interfaceAddress = null;
	}

	/**
	 * Creates a new invalid address exception with structured reason details.
	 *
	 * @param message          the error message
	 * @param reason           failure category
	 * @param interfaceAddress target interface address
	 */
	public InvalidAddressException(String message, Reason reason, InterfaceAddress interfaceAddress) {
		super(message);
		this.reason = reason;
		this.interfaceAddress = interfaceAddress;
	}

	/**
	 * Creates a new invalid address exception with structured reason details and cause.
	 *
	 * @param message          the error message
	 * @param reason           failure category
	 * @param interfaceAddress target interface address
	 * @param cause            underlying cause
	 */
	public InvalidAddressException(String message, Reason reason, InterfaceAddress interfaceAddress, Throwable cause) {
		super(message, cause);
		this.reason = reason;
		this.interfaceAddress = interfaceAddress;
	}

	public Reason getReason() {
		return reason;
	}

	public InterfaceAddress getInterfaceAddress() {
		return interfaceAddress;
	}

	public enum Reason {
		INVALID_FORMAT,
		NETWORK_ADDRESS,
		BROADCAST_ADDRESS,
		INVALID_HOST
	}
}