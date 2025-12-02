package org.example.thesisuj.router.cli;

import org.example.thesisuj.router.Router;

public interface RouterCommand {
	/**
	 * Executes the command on the given router.
	 *
	 * @param router Router instance to execute the command on
	 */
	void execute(Router router);

	/**
	 * Checks if the given input matches this command's pattern.
	 *
	 * @param command Input string to match
	 * @return true if the command matches, false otherwise
	 */
	boolean matches(String command);

	/**
	 * Returns the command pattern for help display.
	 *
	 * @return Command pattern string (e.g., "set protocols static route <destination> next-hop <next-hop>")
	 */
	String getCommandPattern();

	/**
	 * Returns a brief description of what the command does.
	 *
	 * @return Command description
	 */
	String getDescription();
}
