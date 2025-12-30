package org.uj.routingemulator.router.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a token in the configuration file with its position information.
 * Used by the configuration parser to identify and report errors with precise location.
 */
@Getter
@AllArgsConstructor
public class Token {
	/** The string value of this token */
	private final String value;

	/** Line number where this token appears (1-based) */
	private final int line;

	/** Column number where this token starts (1-based) */
	private final int column;

	@Override
	public String toString() {
		return String.format("'%s' at line %d, column %d", value, line, column);
	}
}
