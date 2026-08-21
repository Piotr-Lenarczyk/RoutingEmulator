package org.uj.routingemulator.router.cli;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CommandSyntax {
	private final String pattern;
	private final List<String> elements;

	public CommandSyntax(String pattern) {
		this.pattern = pattern;
		this.elements = List.of(pattern.split("\\s+"));
	}

	public String getPattern() {
		return pattern;
	}

	public Optional<Map<String, String>> parseFully(String input) {
		if (input == null || input.trim().isEmpty()) {
			return Optional.empty();
		}

		String[] inputTokens = input.trim().split("\\s+");

		if (elements.stream().anyMatch(e -> e.startsWith("["))) {
			if (inputTokens[0].equals(elements.get(0))) {
				return Optional.of(Map.of("rawInput", input.trim()));
			}
			return Optional.empty();
		}

		if (inputTokens.length != elements.size()) {
			return Optional.empty();
		}

		Map<String, String> args = new HashMap<>();
		for (int i = 0; i < elements.size(); i++) {
			String expected = elements.get(i);
			String actual = inputTokens[i];

			if (expected.startsWith("<") && expected.endsWith(">")) {
				args.put(expected.substring(1, expected.length() - 1), actual);
			} else {
				if (!expected.equals(actual)) {
					return Optional.empty();
				}
			}
		}
		return Optional.of(args);
	}

	public boolean matchesPrefix(String input) {
		if (input == null || input.trim().isEmpty()) {
			return false;
		}
		String[] inputTokens = input.trim().split("\\s+");

		if (elements.stream().anyMatch(e -> e.startsWith("["))) {
			return elements.get(0).startsWith(inputTokens[0]);
		}

		if (inputTokens.length > elements.size()) {
			return false;
		}

		for (int i = 0; i < inputTokens.length; i++) {
			String expected = elements.get(i);
			String actual = inputTokens[i];

			if (expected.startsWith("<") && expected.endsWith(">")) {
				continue;
			}

			if (i == inputTokens.length - 1) {
				if (!expected.startsWith(actual)) {
					return false;
				}
			} else {
				if (!expected.equals(actual)) {
					return false;
				}
			}
		}
		return true;
	}
}