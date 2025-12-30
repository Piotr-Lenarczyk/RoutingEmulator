package org.uj.routingemulator.router.config;

import org.uj.routingemulator.router.Router;

/**
 * Interface for generating router configuration in various formats.
 * Implementations can produce configuration in different styles (e.g., VyOS commands, JSON, XML).
 */
public interface ConfigurationGenerator {
	/**
	 * Generates configuration text for the specified router.
	 *
	 * @param router the router to generate configuration for
	 * @return configuration text in the format specific to the implementation
	 */
	String generateConfiguration(Router router);
}
