package org.uj.routingemulator.router.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizes router configuration text into individual tokens with position tracking.
 * Handles VyOS-style configuration format, skipping comments and empty lines.
 */
public class ConfigurationTokenizer {
	/**
	 * Tokenizes the input configuration string into a list of tokens.
	 * <p>
	 * The tokenizer:
	 * <ul>
	 *   <li>Splits input by whitespace</li>
	 *   <li>Skips empty lines</li>
	 *   <li>Skips comment lines (starting with #)</li>
	 *   <li>Tracks line and column numbers for each token</li>
	 * </ul>
	 *
	 * @param input the configuration text to tokenize
	 * @return list of tokens with position information
	 */
	public List<Token> tokenize(String input) {
		List<Token> tokens = new ArrayList<>();
		String[] lines = input.split("\n");

		for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
			String line = lines[lineNumber].trim();
			int column = 0;

			if (line.isEmpty() || line.startsWith("#")) {
				continue; // Skip empty lines and comments
			}

			// Tokenize the line by whitespace
			String[] parts = line.split("\\s+");
			for (String part: parts) {
				if (!part.isEmpty()) {
					tokens.add(new Token(part, lineNumber + 1, column + 1));
					column += part.length() + 1; // +1 for the space

				}
			}
		}
		return tokens;
	}
}
