package org.uj.routingemulator.router;

/**
 * Represents the current operational mode of the router CLI.
 * OPERATIONAL - Normal operation mode, read-only commands.
 * CONFIGURATION - Configuration mode, allows modifications.
 */
public enum RouterMode {
	OPERATIONAL,
	CONFIGURATION
}
