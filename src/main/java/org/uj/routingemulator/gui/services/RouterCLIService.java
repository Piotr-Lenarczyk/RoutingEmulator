package org.uj.routingemulator.gui.services;

import org.jline.reader.Candidate;
import org.jline.reader.ParsedLine;
import org.uj.routingemulator.common.topology.NetworkTopology;
import org.uj.routingemulator.router.cli.*;
import org.uj.routingemulator.router.model.Router;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class RouterCLIService {
	private final NetworkTopology topology;
	private final CommandExecutor executor;

	public RouterCLIService(NetworkTopology topology) {
		this.topology = topology;
		this.executor = new DefaultCommandExecutor(new RouterCLIParser(CommandRegistry.defaultRegistry()));
	}

	public String executeCommand(String command, Router router) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter, true);
		CommandOutput output = new PrintWriterCommandOutput(printWriter);
		CommandExecutionContext context = new CommandExecutionContext(router, topology, output);
		CliSession session = new CliSession(executor, context);

		session.execute(command);

		return stringWriter.toString();
	}

	public List<String> getCompletions(String input, Router router) {
		RouterCommandCompleter completer = new RouterCommandCompleter(router);
		List<Candidate> candidates = new ArrayList<>();
		completer.complete(null, new SimpleParsedLine(input), candidates);
		return candidates.stream().map(Candidate::value).toList();
	}

	private record SimpleParsedLine(String line) implements ParsedLine {
		@Override
		public String word() {
			if (line.endsWith(" ") || line.endsWith("\t")) {
				return "";
			}
			String[] words = line.split("\\s+");
			return words.length > 0 ? words[words.length - 1] : "";
		}

		@Override
		public int wordCursor() {
			return word().length();
		}

		@Override
		public int wordIndex() {
			if (line.endsWith(" ") || line.endsWith("\t")) {
				return line.split("\\s+").length;
			}
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