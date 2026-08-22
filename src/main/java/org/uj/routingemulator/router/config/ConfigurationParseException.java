package org.uj.routingemulator.router.config;

import org.uj.routingemulator.common.exceptions.ConfigurationException;

/**
 * Exception thrown when parsing of router configuration fails.
 * Contains detailed error information including the problematic token's location.
 */
public class ConfigurationParseException extends ConfigurationException {

	private final Token token;

	/**
	 * Creates a new configuration parse exception with the specified message.
	 *
	 * @param message the error message
	 */
	public ConfigurationParseException(String message) {
		super(message);
		this.token = null;
	}

	/**
	 * Creates a new configuration parse exception with the specified message and cause.
	 *
	 * @param message the error message
	 * @param cause   underlying cause
	 */
	public ConfigurationParseException(String message, Throwable cause) {
		super(message, cause);
		this.token = null;
	}

	/**
	 * Creates a new configuration parse exception with a message and the problematic token.
	 * The token's position information is automatically included in the error message.
	 *
	 * @param message the error message
	 * @param token the token that caused the parsing error
	 */
	public ConfigurationParseException(String message, Token token) {
		super(String.format("%s%nInvalid token: %s", message, token));
		this.token = token;
	}

	/**
	 * Creates a new configuration parse exception with a message, problematic token, and cause.
	 *
	 * @param message the error message
	 * @param token   the token that caused the parsing error
	 * @param cause   underlying cause
	 */
	public ConfigurationParseException(String message, Token token, Throwable cause) {
		super(String.format("%s%nInvalid token: %s", message, token), cause);
		this.token = token;
	}

	public Token getToken() {
		return token;
	}
}