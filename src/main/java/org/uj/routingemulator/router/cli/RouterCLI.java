package org.uj.routingemulator.router.cli;

import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;

/**
 * Interactive CLI session for the router.
 * Handles the read-eval-print loop with JLine integration
 * for command history, line editing, and auto-completion.
 */
public class RouterCLI {
	private final RouterCLIParser parser;
	private final Router router;

	/**
	 * Constructs a new CLI session for the specified router.
	 *
	 * @param router the router to interact with
	 */
	public RouterCLI(Router router) {
		this.router = router;
		this.parser = new RouterCLIParser();
		this.parser.initializeReader(router);
	}

	/**
	 * Starts the interactive CLI loop.
	 * Continues until user exits the session (via 'exit' in operational mode
	 * or Ctrl+C/Ctrl+D).
	 */
	public void start() {
		// Set the CLIContext writer for terminal mode
		if (parser.getWriter() != null) {
			CLIContext.setWriter(parser.getWriter());
		}

		boolean running = true;

		while (running) {
			try {
				String prompt = getPrompt();
				String line = parser.readLine(prompt);

				if (line != null && !line.trim().isEmpty()) {
					if (line.trim().equalsIgnoreCase("exit")
							&& router.getMode() == RouterMode.OPERATIONAL) {
						running = false;
					} else {
						parser.executeCommand(line, router);
					}
				}
			} catch (UserInterruptException | EndOfFileException e) {
				parser.getWriter().println();
				running = false;
			}
		}

		// Clear CLIContext when done
		CLIContext.clear();
	}

	/**
	 * Gets the CLI prompt based on the router's current mode.
	 *
	 * @return Prompt string
	 */
	private String getPrompt() {
		if (router.getMode() == RouterMode.OPERATIONAL) {
			return router.getName() + "vyos@vyos$ ";
		} else {
			return router.getName() + "vyos@vyos# ";
		}
	}
}
