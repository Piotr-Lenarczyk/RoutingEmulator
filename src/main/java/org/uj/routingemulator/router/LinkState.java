package org.uj.routingemulator.router;

public enum LinkState {
	UP('u'),
	DOWN('D');

	private final char code;

	LinkState(char code) {
		this.code = code;
	}

	public char getCode() {
		return code;
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
