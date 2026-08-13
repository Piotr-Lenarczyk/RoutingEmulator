package org.uj.routingemulator.router.config;

/**
 * Represents a token in the configuration file with its position information.
 * Used by the configuration parser to identify and report errors with precise location.
 *
 * @param value  The string value of this token
 * @param line   Line number where this token appears (1-based)
 * @param column Column number where this token starts (1-based)
 */
public record Token(String value, int line, int column) {
	@Override
	public String toString() {
		return String.format("'%s' at line %d, column %d", value, line, column);
	}
}
