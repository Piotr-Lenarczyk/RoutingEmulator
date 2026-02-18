package org.uj.routingemulator.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.RouterCLIParser;
import org.uj.routingemulator.router.cli.RouterCommandCompleter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog window that provides CLI access to a router using SimpleTerminalTextArea.
 * This is a fallback version if RichTextFX has compatibility issues.
 */
public class SimpleCLIDialog extends Dialog<Void> {

	private final Router router;
	private final RouterCLIParser parser;
	private final RouterCommandCompleter completer;
	private final SimpleTerminalTextArea terminal;

	public SimpleCLIDialog(Router router) {
		this.router = router;
		this.parser = new RouterCLIParser();
		this.completer = new RouterCommandCompleter(router, parser.getCommands());

		setTitle("Router CLI - " + router.getName());
		setHeaderText("VyOS Command Line Interface");

		// Create terminal widget
		terminal = new SimpleTerminalTextArea();
		terminal.setPrefRowCount(24);
		terminal.setPrefColumnCount(80);

		// Restore previous terminal buffer
		boolean hasExistingBuffer = router.getTerminalBuffer().length() > 0;
		if (hasExistingBuffer) {
			terminal.restoreFromBuffer(router.getTerminalBuffer().toString());
		}

		// Setup handlers
		terminal.setOnCommandSubmit(this::processCommand);
		terminal.setOnTabComplete(this::handleTabCompletion);

		VBox content = new VBox(10);
		content.setPadding(new Insets(10));
		content.getChildren().add(terminal);

		getDialogPane().setContent(content);
		getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

		// Show initial prompt and focus terminal
		Platform.runLater(() -> {
			// Only show prompt if we didn't restore a buffer (fresh session)
			// or if buffer doesn't end with a prompt already
			if (!hasExistingBuffer || !bufferEndsWithPrompt()) {
				showPrompt();
			}
			terminal.requestFocus();
		});

		// Save terminal buffer when dialog is closed
		setOnCloseRequest(event -> saveTerminalBuffer());
	}

	private void processCommand(String command) {
		// Early return for empty commands - just show prompt
		if (command.trim().isEmpty()) {
			showPrompt();
			saveTerminalBuffer();
			return;
		}

		// Execute command and capture output
		String output = captureOutput(() -> parser.executeCommand(command, router));
		if (output != null && !output.isEmpty()) {
			terminal.appendColoredText(output);
		}

		showPrompt();
		saveTerminalBuffer();
	}

	private void handleTabCompletion(String input, java.util.function.Consumer<List<String>> callback) {
		// Use completer to get suggestions
		org.jline.reader.ParsedLine parsedLine = new SimpleParsedLine(input);
		List<org.jline.reader.Candidate> candidates = new ArrayList<>();

		completer.complete(null, parsedLine, candidates);

		List<String> completions = candidates.stream()
				.map(org.jline.reader.Candidate::value)
				.collect(Collectors.toList());

		callback.accept(completions);
	}

	private String captureOutput(Runnable command) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);

		try {
			CLIContext.setWriter(printWriter);
			command.run();
			printWriter.flush();
			return stringWriter.toString();
		} finally {
			CLIContext.clear();
		}
	}

	private void showPrompt() {
		String prompt;
		if (router.getMode() == RouterMode.OPERATIONAL) {
			prompt = "vyos@vyos$ ";
		} else if (router.getMode() == RouterMode.CONFIGURATION) {
			prompt = "vyos@vyos# ";
		} else {
			prompt = "> ";
		}
		terminal.showPrompt(prompt);
	}

	/**
	 * Saves the current terminal content to the router's terminal buffer.
	 * This allows the terminal history to persist across dialog sessions.
	 */
	private void saveTerminalBuffer() {
		router.setTerminalBuffer(new StringBuilder(terminal.getText()));
	}

	/**
	 * Checks if the terminal text ends with a prompt.
	 * Used to avoid showing duplicate prompts when reopening the dialog.
	 */
	private boolean bufferEndsWithPrompt() {
		String text = terminal.getText();
		return text.endsWith("vyos@vyos$ ") ||
		       text.endsWith("vyos@vyos# ") ||
		       text.endsWith("> ");
	}

	// Helper class for ParsedLine
	private static class SimpleParsedLine implements org.jline.reader.ParsedLine {
		private final String line;

		SimpleParsedLine(String line) {
			this.line = line;
		}

		@Override
		public String word() {
			// If line ends with whitespace, we're starting a new word (empty)
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
			// If line ends with space, we're at a new word position
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
		public String line() {
			return line;
		}

		@Override
		public int cursor() {
			return line.length();
		}
	}
}

