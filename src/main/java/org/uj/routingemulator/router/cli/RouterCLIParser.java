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

public class RouterCLIParser {
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
			System.err.println("Warning: Could not create system terminal: " + e.getMessage());
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
				.completer(new RouterCommandCompleter(router, commands))
				.option(LineReader.Option.CASE_INSENSITIVE, false)
				.option(LineReader.Option.AUTO_LIST, true)
				.option(LineReader.Option.AUTO_MENU, true)
				.variable(LineReader.HISTORY_FILE, historyFile)
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
		// CLIContext should be set by the caller before calling this method
		// In terminal mode, it's set in RouterCLI.start()
		// In GUI mode, it's set by captureOutput() wrapper

		PrintWriter out = CLIContext.getWriter(); // This has a fallback to System.out

		// First, try exact match
		for (RouterCommand command : commands) {
			if (command.matches(input)) {
				try {
					command.execute(router);
					out.flush();
				} catch (RuntimeException e) {
					out.println(e.getMessage());
					out.flush();
				}
				return;
			}
		}

		// No exact match found - try prefix matching
		// This allows "con" to match "configure" if it's unambiguous
		RouterCommand prefixMatch = findUniquePrefixMatch(input);
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
	 * Finds a command that uniquely matches the input as a prefix.
	 * Returns the command if exactly one command's pattern starts with the input.
	 * Returns null if no match or multiple matches (ambiguous).
	 *
	 * @param input User input
	 * @return Matching command or null
	 */
	private RouterCommand findUniquePrefixMatch(String input) {
		String inputLower = input.trim().toLowerCase();
		if (inputLower.isEmpty()) {
			return null;
		}

		// Split input to get the first word (the command keyword)
		String[] inputWords = inputLower.split("\\s+");
		String firstWord = inputWords[0];

		List<RouterCommand> matches = new ArrayList<>();

		for (RouterCommand command : commands) {
			String pattern = command.getCommandPattern().toLowerCase();
			// Get the first word of the pattern
			String[] patternWords = pattern.split("\\s+");
			if (patternWords.length > 0 && patternWords[0].startsWith(firstWord)) {
				// Check if the rest of the input also matches (for multi-word commands)
				if (inputWords.length == 1) {
					// Only checking the first word
					matches.add(command);
				} else {
					// For multi-word commands, check if the full input matches the pattern prefix
					String patternPrefix = extractPatternPrefix(pattern, inputWords.length);
					if (input.trim().equalsIgnoreCase(patternPrefix)) {
						matches.add(command);
					}
				}
			}
		}

		// Return the command only if there's exactly one match
		return matches.size() == 1 ? matches.get(0) : null;
	}

	/**
	 * Extracts the first N words from a pattern, stopping at placeholders.
	 *
	 * @param pattern   Command pattern
	 * @param wordCount Number of words to extract
	 * @return Pattern prefix or null if it contains placeholders
	 */
	private String extractPatternPrefix(String pattern, int wordCount) {
		String[] words = pattern.split("\\s+");
		if (words.length < wordCount) {
			return null;
		}

		StringBuilder prefix = new StringBuilder();
		for (int i = 0; i < wordCount; i++) {
			// Stop if we encounter a placeholder (words with < or >)
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
