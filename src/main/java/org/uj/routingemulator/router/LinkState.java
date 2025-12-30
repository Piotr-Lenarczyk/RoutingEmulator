package org.uj.routingemulator.router;

import lombok.Getter;

/**
 * Represents the physical link state of a router interface.
 * <p>
 * Link state indicates whether the physical connection is established.
 * It is typically determined by the presence of a physical signal on the cable.
 */
@Getter
public enum LinkState {
	/** Physical link is up (cable connected and signal detected) */
	UP('u'),
	/** Physical link is down (no cable or no signal) */
	DOWN('D');

	private final char code;

	/**
	 * Creates a LinkState with the specified character code.
	 *
	 * @param code single character representation used in status display
	 */
	LinkState(char code) {
		this.code = code;
	}

	/**
	 * Parses LinkState from a character code.
	 *
	 * @param c Character code ('u' for UP, 'D' for DOWN)
	 * @return Corresponding LinkState
	 * @throws IllegalArgumentException if the code is invalid
	 */
	public static LinkState fromCode(char c) {
		return switch (c) {
			case 'u' -> UP;
			case 'D' -> DOWN;
			default -> throw new IllegalArgumentException("Invalid link state: " + c);
		};
	}
}
