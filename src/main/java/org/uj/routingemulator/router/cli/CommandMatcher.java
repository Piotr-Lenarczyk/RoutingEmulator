package org.uj.routingemulator.router.cli;

import java.util.ArrayList;
import java.util.List;

public class CommandMatcher {
	private CommandMatcher() {
	}

	public static RouterCommand findUniquePrefixMatch(String input, List<RouterCommand> commands) {
		String inputTrim = input.trim();
		if (inputTrim.isEmpty()) {
			return null;
		}

		String[] inputWords = inputTrim.split("\\s+");
		String firstWord = inputWords[0];
		List<RouterCommand> matches = new ArrayList<>();

		for (RouterCommand command : commands) {
			String pattern = command.getCommandPattern();
			String[] patternWords = pattern.split("\\s+");

			if (patternWords.length > 0 && patternWords[0].startsWith(firstWord)) {
				if (inputWords.length == 1) {
					matches.add(command);
				} else {
					String patternPrefix = extractPatternPrefix(pattern, inputWords.length);
					if (inputTrim.equals(patternPrefix)) {
						matches.add(command);
					}
				}
			}
		}
		return matches.size() == 1 ? matches.getFirst() : null;
	}

	private static String extractPatternPrefix(String pattern, int wordCount) {
		String[] words = pattern.split("\\s+");
		if (words.length < wordCount) {
			return null;
		}

		StringBuilder prefix = new StringBuilder();
		for (int i = 0; i < wordCount; i++) {
			if (words[i].contains("<") || words[i].contains(">")) {
				return null;
			}
			if (i > 0) {
				prefix.append(" ");
			}
			prefix.append(words[i]);
		}
		return prefix.toString();
	}
}