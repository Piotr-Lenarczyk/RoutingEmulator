package org.uj.routingemulator.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;
import org.jline.reader.Candidate;
import org.jline.reader.ParsedLine;
import org.uj.routingemulator.common.NetworkTopology;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.RouterCLIParser;
import org.uj.routingemulator.router.cli.RouterCommandCompleter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog window that provides CLI access to a router using SimpleTerminalTextArea.
 */
public class SimpleCLIDialog extends Dialog<Void> {

	// Manage GUI session state outside of the domain model
	private static final Map<Router, RouterSessionState> sessionStates = new HashMap<>();

    private final Router router;
    private final RouterCLIParser parser;
    private final RouterCommandCompleter completer;
    private final SimpleTerminalTextArea terminal;
    private final NetworkTopology topology;

    public SimpleCLIDialog(Router router, NetworkTopology topology) {
        this.router = router;
        this.topology = topology;
        this.parser = new RouterCLIParser();
        this.completer = new RouterCommandCompleter(router);

        setTitle("Router CLI - " + router.getName());
        setHeaderText("VyOS Command Line Interface");

        // Create terminal widget
        terminal = new SimpleTerminalTextArea();
        terminal.setPrefRowCount(24);
        terminal.setPrefColumnCount(80);

	    // Retrieve or generate session state
	    RouterSessionState sessionState = sessionStates.computeIfAbsent(router, r -> new RouterSessionState());
	    boolean hasExistingBuffer = !sessionState.getTerminalBuffer().isEmpty();

        if (hasExistingBuffer) {
	        terminal.restoreFromBuffer(sessionState.getTerminalBuffer().toString());
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
            if (!hasExistingBuffer || !bufferEndsWithPrompt()) {
                showPrompt();
            }
            terminal.requestFocus();
        });

        // Save terminal buffer when dialog is closed
        setOnCloseRequest(event -> saveTerminalBuffer());
    }

    private void processCommand(String command) {
        if (command.trim().isEmpty()) {
            showPrompt();
            saveTerminalBuffer();
            return;
        }

        String output = captureOutput(() -> parser.executeCommand(command, router));
        if (output != null && !output.isEmpty()) {
            terminal.appendColoredText(output);
        }

        showPrompt();
        saveTerminalBuffer();
    }

    private void handleTabCompletion(String input, java.util.function.Consumer<List<String>> callback) {
        ParsedLine parsedLine = new SimpleParsedLine(input);
        List<org.jline.reader.Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine, candidates);

        List<String> completions = candidates.stream()
                .map(Candidate::value)
                .toList();

        callback.accept(completions);
    }

    private String captureOutput(Runnable command) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        try {
            CLIContext.setWriter(printWriter);
            CLIContext.setNetworkTopology(this.topology);
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

    private void saveTerminalBuffer() {
	    sessionStates.get(router).updateBuffer(terminal.getText());
    }

    private boolean bufferEndsWithPrompt() {
        String text = terminal.getText();
        return text.endsWith("vyos@vyos$ ") ||
                text.endsWith("vyos@vyos# ") ||
                text.endsWith("> ");
    }

    // Helper class for ParsedLine
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