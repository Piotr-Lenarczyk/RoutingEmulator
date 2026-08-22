package org.uj.routingemulator.gui.dialogs;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;
import org.uj.routingemulator.gui.services.RouterCLIService;
import org.uj.routingemulator.gui.viewmodel.RouterSessionState;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.RouterMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleCLIDialog extends Dialog<Void> {
    private static final Map<Router, RouterSessionState> sessionStates = new HashMap<>();

    private final Router router;
    private final RouterCLIService cliService;
    private final SimpleTerminalTextArea terminal;

    public SimpleCLIDialog(Router router, RouterCLIService cliService) {
        this.router = router;
        this.cliService = cliService;

        setTitle("Router CLI - " + router.getName());
        setHeaderText("VyOS Command Line Interface");

        terminal = new SimpleTerminalTextArea();
        terminal.setPrefRowCount(24);
        terminal.setPrefColumnCount(80);

        RouterSessionState sessionState = sessionStates.computeIfAbsent(router, r -> new RouterSessionState());
        boolean hasExistingBuffer = !sessionState.getTerminalBuffer().isEmpty();

        if (hasExistingBuffer) {
            terminal.restoreFromBuffer(sessionState.getTerminalBuffer().toString());
        }

        terminal.setOnCommandSubmit(this::processCommand);
        terminal.setOnTabComplete(this::handleTabCompletion);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().add(terminal);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Platform.runLater(() -> {
            if (!hasExistingBuffer || !bufferEndsWithPrompt()) {
                showPrompt();
            }
            terminal.requestFocus();
        });

        setOnCloseRequest(event -> saveTerminalBuffer());
    }

    private void processCommand(String command) {
        if (command.trim().isEmpty()) {
            showPrompt();
            saveTerminalBuffer();
            return;
        }

        String output = cliService.executeCommand(command, router);
        if (output != null && !output.isEmpty()) {
            terminal.appendColoredText(output);
        }

        showPrompt();
        saveTerminalBuffer();
    }

    private void handleTabCompletion(String input, java.util.function.Consumer<List<String>> callback) {
        List<String> completions = cliService.getCompletions(input, router);
        callback.accept(completions);
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
}