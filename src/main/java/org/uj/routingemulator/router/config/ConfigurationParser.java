package org.uj.routingemulator.router.config;

import org.uj.routingemulator.router.Router;

/**
 * Interface for parsing router configuration in various formats.
 * <p>
 * Implementations can parse different VyOS configuration formats:
 * <ul>
 *   <li>Command-based format (set commands)</li>
 *   <li>Hierarchical format (show configuration output)</li>
 * </ul>
 * <p>
 * If parsing fails, all changes are automatically rolled back and the router
 * is restored to its original state.
 */
public interface ConfigurationParser {
	/**
	 * Loads and applies configuration from a string to the specified router.
	 *
	 * @param router the router to configure
	 * @param config the configuration text
	 * @throws ConfigurationParseException if the configuration is invalid
	 */
	void loadConfiguration(Router router, String config);
}

