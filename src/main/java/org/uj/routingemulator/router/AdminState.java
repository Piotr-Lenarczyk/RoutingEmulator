package org.uj.routingemulator.router;

public enum AdminState {
	UP('u'),
	ADMIN_DOWN('A');

	private final char code;

	AdminState(char code) {
		this.code = code;
	}

	public char getCode() {
		return code;
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
