package org.uj.routingemulator.router.config;

import java.util.logging.Logger;

/**
 * Factory for creating appropriate configuration parser and generator based on format detection.
 * <p>
 * Automatically detects whether configuration is in command format ('set' commands)
 * or hierarchical format (curly braces).
 */
public class ConfigurationFactory {
	private static final Logger logger = Logger.getLogger(ConfigurationFactory.class.getName());

	private ConfigurationFactory() {
		// Private constructor to prevent instantiation
	}

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
			logger.info("Detected hierarchical configuration format");
			return new HierarchicalConfigurationParser();
		}

		// Default to command format
		logger.info("Detected command-based configuration format");
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

