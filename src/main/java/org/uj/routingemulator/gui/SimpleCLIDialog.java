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
import java.util.List;

/**
 * Dialog window that provides CLI access to a router using SimpleTerminalTextArea.
 * This is a fallback version if RichTextFX has compatibility issues
 */
public class SimpleCLIDialog extends Dialog<Void> {

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
        terminal.setPrefColumnCount(95);

        // Restore previous terminal buffer and history
        boolean hasExistingBuffer = !router.getTerminalBuffer().isEmpty();
        if (hasExistingBuffer) {
            terminal.restoreFromBuffer(router.getTerminalBuffer().toString());
        }
        terminal.loadCommandHistory(router.getCommandHistory());

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

        // Save terminal state when dialog is closed
        setOnCloseRequest(event -> saveTerminalState());
    }

    private void processCommand(String command) {
        if (command.trim().isEmpty()) {
            showPrompt();
            saveTerminalState();
            return;
        }

        String output = captureOutput(() -> parser.executeCommand(command, router));

        if (output != null && !output.isEmpty()) {
            terminal.appendColoredText(output);
        }

        showPrompt();
        saveTerminalState();
    }

    private void handleTabCompletion(String input, java.util.function.Consumer<List<Candidate>> callback) {
        ParsedLine parsedLine = new SimpleParsedLine(input);
        List<Candidate> candidates = new ArrayList<>();
        completer.complete(null, parsedLine, candidates);

        // Pass all candidates directly to the terminal, allowing the terminal
        // to render VyOS style hints appropriately.
        callback.accept(candidates);
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

    private void saveTerminalState() {
        router.setTerminalBuffer(new StringBuilder(terminal.getText()));
        router.setCommandHistory(terminal.getCommandHistory());
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