package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when attempting to configure an invalid IP address on an interface.
 * <p>
 * This includes:
 * <ul>
 *   <li>Network addresses (e.g., 192.168.1.0/24)</li>
 *   <li>Broadcast addresses (e.g., 192.168.1.255/24)</li>
 *   <li>Malformed IP addresses</li>
 *   <li>IP addresses outside the valid range</li>
 * </ul>
 */
public class InvalidAddressException extends RouterException {

	/**
	 * Creates a new invalid address exception with the specified message.
	 *
	 * @param message the error message
	 */
	public InvalidAddressException(String message) {
		super(message);
	}
}

