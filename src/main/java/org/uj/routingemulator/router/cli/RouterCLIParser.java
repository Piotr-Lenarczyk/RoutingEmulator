package org.uj.routingemulator.router.cli;

import lombok.Getter;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.cli.dummy.DeleteInterfaceDummyCommand;
import org.uj.routingemulator.router.cli.dummy.DisableInterfaceDummyCommand;
import org.uj.routingemulator.router.cli.dummy.SetInterfaceDummyCommand;
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
		String historyFile = System.getProperty("user.home") + "/.vyos_history" + router.getName() + "_" + System.identityHashCode(router);
		new File(historyFile).deleteOnExit();

		this.reader = LineReaderBuilder.builder()
				.terminal(terminal)
				.completer(new RouterCommandCompleter(router))
				.option(LineReader.Option.CASE_INSENSITIVE, false)
				.option(LineReader.Option.AUTO_LIST, true)
				.option(LineReader.Option.AUTO_MENU, true)
				.variable(LineReader.HISTORY_FILE, historyFile)
				.build();
	}

	public String readLine(String prompt) {
		if (reader == null) {
			throw new IllegalStateException("LineReader not initialized. Call initializeReader() first.");
		}
		return reader.readLine(prompt);
	}

	private void registerCommands() {
		// Show commands
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

		// Register route commands
		commands.add(new DeleteRouteNextHopDistanceCommand());
		commands.add(new DeleteRouteInterfaceDistanceCommand());
		commands.add(new DeleteRouteNextHopCommand());
		commands.add(new DeleteRouteInterfaceCommand());

		commands.add(new DisableRouteNextHopDistanceCommand());
		commands.add(new DisableRouteInterfaceDistanceCommand());
		commands.add(new DisableRouteNextHopCommand());
		commands.add(new DisableRouteInterfaceCommand());

		commands.add(new SetRouteNextHopDistanceCommand());
		commands.add(new SetRouteInterfaceDistanceCommand());
		commands.add(new SetRouteNextHopCommand());
		commands.add(new SetRouteInterfaceCommand());

		// Interface commands (Ethernet)
		commands.add(new DeleteInterfaceEthernetCommand());
		commands.add(new DisableInterfaceEthernetCommand());
		commands.add(new SetInterfaceEthernetCommand());

		// Interface commands (Dummy)
		commands.add(new DeleteInterfaceDummyCommand());
		commands.add(new DisableInterfaceDummyCommand());
		commands.add(new SetInterfaceDummyCommand());
	}

	public void executeCommand(String input, Router router) {
		logger.info("%s: Executing command: %s".formatted(router.getName(), input));
		PrintWriter out = CLIContext.getWriter();

		String expandedInput;
		try {
			// Pre-process the input to expand abbreviations (e.g. "show int" -> "show interfaces")
			expandedInput = expandCommand(input, router);
		} catch (RuntimeException e) {
			out.println(e.getMessage());
			out.flush();
			return;
		}

		// Try exact match using the fully expanded command string
		for (RouterCommand command : commands) {
			if (command.matches(expandedInput)) {
				try {
					logger.info("%s: Command match found: %s for input string: %s".formatted(router.getName(), command.getCommandPattern(), expandedInput));
					command.execute(router);
					out.flush();
				} catch (RuntimeException e) {
					out.println(e.getMessage());
					out.flush();
				}
				return;
			}
		}

		out.println("Command not recognized or not supported");
		out.flush();
	}

	/**
	 * Expands abbreviated commands word-by-word using the RouterCommandCompleter logic.
	 * E.g., "set int eth eth0 dis" expands to "set interfaces ethernet eth0 disable".
	 *
	 * @param input  User input
	 * @param router Router context
	 * @return Fully expanded command string
	 */
	private String expandCommand(String input, Router router) {
		String[] words = input.trim().split("\\s+");
		if (words.length == 0 || words[0].isEmpty()) {
			return "";
		}

		StringBuilder expanded = new StringBuilder();
		RouterCommandCompleter completer = new RouterCommandCompleter(router);

		for (int i = 0; i < words.length; i++) {
			String currentWord = words[i];

			// Build the line context up to the current word
			String lineSoFar = expanded.toString();
			if (i > 0 && !lineSoFar.endsWith(" ")) {
				lineSoFar += " ";
			}

			ParsedLine parsedLine = new SimpleParsedLine(lineSoFar);
			List<Candidate> candidates = new ArrayList<>();
			completer.complete(null, parsedLine, candidates);

			// Filter out placeholder hints (e.g., <x.x.x.x>)
			List<String> validCompletions = candidates.stream()
					.map(Candidate::value)
					.filter(val -> !val.startsWith("<"))
					.toList();

			List<String> matches = new ArrayList<>();
			String exactMatch = null;

			for (String val : validCompletions) {
				if (val.equalsIgnoreCase(currentWord)) {
					exactMatch = val;
				}
				if (val.toLowerCase().startsWith(currentWord.toLowerCase())) {
					matches.add(val);
				}
			}

			// Determine how to append the current word
			if (exactMatch != null) {
				expanded.append(exactMatch);
			} else if (matches.size() == 1) {
				expanded.append(matches.get(0));
			} else if (matches.size() > 1) {
				throw new RuntimeException("Ambiguous command: " + currentWord);
			} else {
				// If 0 matches, it's likely a dynamic user argument (like an IP address). Pass it through verbatim.
				expanded.append(currentWord);
			}

			// Append space if it's not the last word
			if (i < words.length - 1) {
				expanded.append(" ");
			}
		}
		return expanded.toString();
	}

	public void printHelp() {
		PrintWriter out = (writer != null) ? writer : CLIContext.getWriter();
		for (RouterCommand command : commands) {
			out.printf(" - %s: %s%n", command.getCommandPattern(), command.getDescription());
		}
		out.flush();
	}

	/**
	 * Helper record to feed the current command state into the JLine Completer
	 */
	private record SimpleParsedLine(String line) implements ParsedLine {
		@Override
		public String word() {
			if (line.endsWith(" ") || line.endsWith("\t")) return "";
			String[] words = line.split("\\s+");
			return words.length > 0 ? words[words.length - 1] : "";
		}

		@Override
		public int wordCursor() {
			return word().length();
		}

		@Override
		public int wordIndex() {
			if (line.endsWith(" ") || line.endsWith("\t")) return line.split("\\s+").length;
			return line.split("\\s+").length - 1;
		}

		@Override
		public List<String> words() {
			return List.of(line.split("\\s+"));
		}

		@Override
		public int cursor() {
			return line.length();
		}
	}
}