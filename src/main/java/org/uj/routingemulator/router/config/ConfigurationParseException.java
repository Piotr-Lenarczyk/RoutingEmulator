package org.uj.routingemulator.router.config;

/**
 * Exception thrown when parsing of router configuration fails.
 * Contains detailed error information including the problematic token's location.
 */
public class ConfigurationParseException extends RuntimeException {
	/**
	 * Creates a new configuration parse exception with the specified message.
	 *
	 * @param message the error message
	 */
	public ConfigurationParseException(String message) {
		super(message);
	}

	/**
	 * Creates a new configuration parse exception with a message and the problematic token.
	 * The token's position information is automatically included in the error message.
	 *
	 * @param message the error message
	 * @param token the token that caused the parsing error
	 */
	public ConfigurationParseException(String message, Token token) {
		super(String.format("%s\nInvalid token: %s", message, token));
	}
}
