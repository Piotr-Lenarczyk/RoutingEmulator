package org.uj.routingemulator.router;

import lombok.Getter;

/**
 * Represents the administrative state of a router interface.
 * <p>
 * Administrative state is controlled by the network administrator through configuration.
 * It determines whether the interface is allowed to be operational.
 */
@Getter
public enum AdminState {
	/** Interface is administratively enabled */
	UP('u'),
	/** Interface is administratively disabled (shutdown) */
	ADMIN_DOWN('A');

	private final char code;

	/**
	 * Creates an AdminState with the specified character code.
	 *
	 * @param code single character representation used in status display
	 */
	AdminState(char code) {
		this.code = code;
	}

	/**
	 * Parses AdminState from a character code.
	 *
	 * @param c Character code ('u' for UP, 'A' for ADMIN_DOWN)
	 * @return Corresponding AdminState
	 * @throws IllegalArgumentException if the code is invalid
	 */
	public static AdminState fromCode(char c) {
		return switch (c) {
			case 'u' -> UP;
			case 'A' -> ADMIN_DOWN;
			default -> throw new IllegalArgumentException("Invalid state: " + c);
		};
	}
}
