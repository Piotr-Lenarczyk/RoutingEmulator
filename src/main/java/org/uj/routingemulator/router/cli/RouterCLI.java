package org.uj.routingemulator.router.cli;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.uj.routingemulator.common.NetworkTopology;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class RouterCLI {
	private static final Logger logger = Logger.getLogger(RouterCLI.class.getName());
	private final Router router;
	private final NetworkTopology topology;
	private final CommandExecutor executor;
	private Terminal terminal;
	private LineReader reader;
	private PrintWriter writer;

	public RouterCLI(Router router, NetworkTopology topology) {
		this.router = router;
		this.topology = topology;
		this.executor = new DefaultCommandExecutor(new RouterCLIParser());
		try {
			this.terminal = TerminalBuilder.builder().system(true).build();
			this.writer = terminal.writer();
			initializeReader();
		} catch (IOException e) {
			logger.warning("Could not create system terminal: %s".formatted(e.getMessage()));
		}
	}

	private void initializeReader() {
		String historyFile = System.getProperty("user.home") + "/.vyos_history" + router.getName() + "_" + System.identityHashCode(router);
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

	public void start() {
		if (reader == null || writer == null) return;

		CommandOutput output = new PrintWriterCommandOutput(writer);
		CommandExecutionContext context = new CommandExecutionContext(router, topology, output);
		CliSession session = new CliSession(executor, context);

		boolean running = true;
		while (running) {
			try {
				String prompt = getPrompt();
				String line = reader.readLine(prompt);
				if (line != null && !line.trim().isEmpty()) {
					if (line.trim().equalsIgnoreCase("exit") && router.getMode() == RouterMode.OPERATIONAL) {
						running = false;
					} else {
						session.execute(line);
					}
				}
			} catch (UserInterruptException | EndOfFileException e) {
				writer.println();
				running = false;
			}
		}
	}

	private String getPrompt() {
		if (router.getMode() == RouterMode.OPERATIONAL) {
			return router.getName() + "vyos@vyos$ ";
		} else {
			return router.getName() + "vyos@vyos# ";
		}
	}
}