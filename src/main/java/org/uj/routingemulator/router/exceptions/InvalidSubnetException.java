package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when a provided subnet notation or network address is invalid.
 */
public class InvalidSubnetException extends RouterException {

	private final String rawInput;

	/**
	 * Creates a new invalid subnet exception with the specified message.
	 *
	 * @param message detail message
	 */
	public InvalidSubnetException(String message) {
		super(message);
		this.rawInput = extractPrefix(message);
	}

	/**
	 * Creates a new invalid subnet exception with the specified message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public InvalidSubnetException(String message, Throwable cause) {
		super(message, cause);
		this.rawInput = extractPrefix(message);
	}

	/**
	 * Creates a new invalid subnet exception with structured raw input value.
	 *
	 * @param message  detail message
	 * @param rawInput unparsed raw input value
	 */
	public InvalidSubnetException(String message, String rawInput) {
		super(message);
		this.rawInput = rawInput;
	}

	/**
	 * Creates a new invalid subnet exception with structured raw input value and cause.
	 *
	 * @param message  detail message
	 * @param rawInput unparsed raw input value
	 * @param cause    underlying cause
	 */
	public InvalidSubnetException(String message, String rawInput, Throwable cause) {
		super(message, cause);
		this.rawInput = rawInput;
	}

	private static String extractPrefix(String message) {
		if (message != null && message.endsWith(" is not a valid IPv4 prefix")) {
			return message.replace(" is not a valid IPv4 prefix", "");
		}
		return message;
	}

	public String getRawInput() {
		return rawInput;
	}
}