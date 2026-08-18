package org.uj.routingemulator.router.cli;

import lombok.Getter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.cli.ethernet.DeleteInterfaceEthernetCommand;
import org.uj.routingemulator.router.cli.ethernet.DisableInterfaceEthernetCommand;
import org.uj.routingemulator.router.cli.ethernet.SetInterfaceEthernetCommand;
import org.uj.routingemulator.router.cli.route.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class RouterCLIParser {
	private static final Logger logger = Logger.getLogger(RouterCLIParser.class.getName());
	@Getter
	private final List<RouterCommand> commands;
	@Getter
	private Terminal terminal;
	private LineReader reader;
	@Getter
	private PrintWriter writer;

	public RouterCLIParser() {
		this.commands = new ArrayList<>();
		try {
			this.terminal = TerminalBuilder.builder().system(true).build();
			this.writer = terminal.writer();
		} catch (IOException e) {
			// If terminal creation fails (e.g., in GUI), writer will be null
			// Commands should use CLIContext.getWriter() which has a fallback
			logger.warning("Could not create system terminal: %s".formatted(e.getMessage()));
			this.terminal = null;
			this.writer = null;
		}
		registerCommands();
	}

	/**
	 * Initializes the LineReader with completer for the given router.
	 * Must be called before using the reader.
	 *
	 * @param router Router instance for context-aware completions
	 */
	public void initializeReader(Router router) {
		// Each router has a separate history but might not necessarily have a unique name
		// Therefore, object hash code needs to be included
		String historyFile = System.getProperty("user.home") + "/.vyos_history"
				+ router.getName() + "_" + System.identityHashCode(router);

		// Mark history file for deletion on JVM exit
		new File(historyFile).deleteOnExit();

		this.reader = LineReaderBuilder.builder()
				.terminal(terminal)
				.completer(new RouterCommandCompleter(router))
				.option(org.jline.reader.LineReader.Option.CASE_INSENSITIVE, false)
				.option(org.jline.reader.LineReader.Option.AUTO_LIST, true)
				.option(org.jline.reader.LineReader.Option.AUTO_MENU, true)
				.variable(org.jline.reader.LineReader.HISTORY_FILE, historyFile)
				.build();
	}

	/**
	 * Reads a line of input with JLine features (history, completion, etc.).
	 *
	 * @param prompt Prompt to display
	 * @return User input string
	 */
	public String readLine(String prompt) {
		if (reader == null) {
			throw new IllegalStateException("LineReader not initialized. Call initializeReader() first.");
		}
		return reader.readLine(prompt);
	}


	/**
	 * Registers all available CLI commands.
	 * Order is important: more specific patterns must be registered before general ones.
	 */
	private void registerCommands() {
		// Show commands (should be early to avoid conflicts)
		commands.add(new ShowIpRouteCommand());
		commands.add(new ShowConfigurationCommand());
		commands.add(new ShowInterfacesCommand());
		// Ping (operational)
		commands.add(new PingCommand());
		// Configuration mode commands
		commands.add(new ConfigureCommand());
		commands.add(new CommitCommand());
		commands.add(new ExitCommand());
		commands.add(new ForceExitCommand());
		// Register route commands - order matters: more specific patterns first
		// Delete commands (with distance first, then without)
		commands.add(new DeleteRouteNextHopDistanceCommand());
		commands.add(new DeleteRouteInterfaceDistanceCommand());
		commands.add(new DeleteRouteNextHopCommand());
		commands.add(new DeleteRouteInterfaceCommand());
		// Disable commands (with distance first, then without)
		commands.add(new DisableRouteNextHopDistanceCommand());
		commands.add(new DisableRouteInterfaceDistanceCommand());
		commands.add(new DisableRouteNextHopCommand());
		commands.add(new DisableRouteInterfaceCommand());
		// Set commands (with distance first, then without)
		commands.add(new SetRouteNextHopDistanceCommand());
		commands.add(new SetRouteInterfaceDistanceCommand());
		commands.add(new SetRouteNextHopCommand());
		commands.add(new SetRouteInterfaceCommand());
		// Interface commands
		commands.add(new DeleteInterfaceEthernetCommand());
		commands.add(new DisableInterfaceEthernetCommand());
		commands.add(new SetInterfaceEthernetCommand());
	}

	public void executeCommand(String input, Router router) {
		logger.info("%s: Executing command: %s".formatted(router.getName(), input));
		PrintWriter out = CLIContext.getWriter();

		for (RouterCommand command : commands) {
			if (command.matches(input)) {
				try {
					logger.info("%s: Command match found: %s for input string: %s".formatted(router.getName(), command.getCommandPattern(), input));
					command.execute(router);
					out.flush();
				} catch (RuntimeException e) {
					out.println(e.getMessage());
					out.flush();
				}
				return;
			}
		}

		RouterCommand prefixMatch = CommandMatcher.findUniquePrefixMatch(input, commands);
		if (prefixMatch != null) {
			try {
				prefixMatch.execute(router);
				out.flush();
			} catch (RuntimeException e) {
				out.println(e.getMessage());
				out.flush();
			}
			return;
		}

		out.println("Command not recognized or not supported");
		out.flush();
	}

	/**
	 * Prints help information for all registered commands.
	 */
	public void printHelp() {
		PrintWriter out = (writer != null) ? writer : CLIContext.getWriter();
		for (RouterCommand command : commands) {
			out.printf(" - %s: %s%n", command.getCommandPattern(), command.getDescription());
		}
		out.flush();
	}
}
