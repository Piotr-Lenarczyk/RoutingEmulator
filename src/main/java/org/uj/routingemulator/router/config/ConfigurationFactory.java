package org.uj.routingemulator.router.config;

/**
 * Factory for creating appropriate configuration parser and generator based on format detection.
 * <p>
 * Automatically detects whether configuration is in command format ('set' commands)
 * or hierarchical format (curly braces).
 */
public class ConfigurationFactory {

	/**
	 * Detects configuration format and returns appropriate parser.
	 * <p>
	 * Detection logic:
	 * <ul>
	 *   <li>If config starts with "set" → Command format</li>
	 *   <li>If config contains "{" → Hierarchical format</li>
	 *   <li>Otherwise → Command format (default)</li>
	 * </ul>
	 *
	 * @param config configuration text to analyze
	 * @return appropriate parser for the detected format
	 */
	public static ConfigurationParser getParser(String config) {
		String trimmed = config.trim();

		// Check for hierarchical format (contains curly braces)
		if (trimmed.contains("{")) {
			return new HierarchicalConfigurationParser();
		}

		// Default to command format
		return new CommandConfigurationParser();
	}

	/**
	 * Returns command-based configuration generator (default).
	 *
	 * @return command configuration generator
	 */
	public static ConfigurationGenerator getCommandGenerator() {
		return new CommandConfigurationGenerator();
	}

	/**
	 * Returns hierarchical configuration generator.
	 *
	 * @return hierarchical configuration generator
	 */
	public static ConfigurationGenerator getHierarchicalGenerator() {
		return new HierarchicalConfigurationGenerator();
	}
}

