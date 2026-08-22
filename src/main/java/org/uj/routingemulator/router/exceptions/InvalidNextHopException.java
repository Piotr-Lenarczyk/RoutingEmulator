package org.uj.routingemulator.router.exceptions;

/**
 * Exception thrown when a provided next-hop address format or value is invalid.
 */
public class InvalidNextHopException extends RouterException {

	private final String rawInput;

	/**
	 * Creates a new invalid next-hop exception with the specified message.
	 *
	 * @param message detail message
	 */
	public InvalidNextHopException(String message) {
		super(message);
		this.rawInput = extractPrefix(message);
	}

	/**
	 * Creates a new invalid next-hop exception with the specified message and cause.
	 *
	 * @param message detail message
	 * @param cause   underlying cause
	 */
	public InvalidNextHopException(String message, Throwable cause) {
		super(message, cause);
		this.rawInput = extractPrefix(message);
	}

	/**
	 * Creates a new invalid next-hop exception with structured raw input value.
	 *
	 * @param message  detail message
	 * @param rawInput unparsed raw input value
	 */
	public InvalidNextHopException(String message, String rawInput) {
		super(message);
		this.rawInput = rawInput;
	}

	/**
	 * Creates a new invalid next-hop exception with structured raw input value and cause.
	 *
	 * @param message  detail message
	 * @param rawInput unparsed raw input value
	 * @param cause    underlying cause
	 */
	public InvalidNextHopException(String message, String rawInput, Throwable cause) {
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