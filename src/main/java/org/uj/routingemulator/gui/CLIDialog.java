package org.uj.routingemulator.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.cli.RouterCLIParser;

/**
 * Dialog window that provides CLI access to a router.
 */
public class CLIDialog extends Dialog<Void> {

	private Router router;
	private RouterCLIParser parser;
	private TextArea outputArea;
	private TextField inputField;

	/**
	 * Creates a new CLI dialog for the specified router.
	 *
	 * @param router the router to interact with
	 */
	public CLIDialog(Router router) {
		this.router = router;
		this.parser = new RouterCLIParser();

		setTitle("Router CLI - " + router.getName());
		setHeaderText("VyOS Command Line Interface");

		// Create UI components
		outputArea = new TextArea();
		outputArea.setEditable(false);
		outputArea.setPrefRowCount(20);
		outputArea.setPrefColumnCount(80);
		outputArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

		inputField = new TextField();
		inputField.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
		inputField.setPromptText("Enter command...");

		VBox content = new VBox(10);
		content.setPadding(new Insets(10));
		content.getChildren().addAll(outputArea, inputField);

		getDialogPane().setContent(content);
		getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

		// Setup event handlers
		inputField.setOnAction(e -> processCommand());

		// Initial prompt
		showPrompt();

		// Focus on input field
		Platform.runLater(() -> inputField.requestFocus());
	}

	/**
	 * Processes the command entered by the user.
	 */
	private void processCommand() {
		String command = inputField.getText().trim();
		inputField.clear();

		if (command.isEmpty()) {
			showPrompt();
			return;
		}

		// Display the command
		appendOutput(command + "\n");

		// Execute the command and capture output
		try {
			String output = captureOutput(() -> parser.executeCommand(command, router));
			if (output != null && !output.isEmpty()) {
				appendOutput(output);
			}
		} catch (Exception ex) {
			appendOutput("Error: " + ex.getMessage() + "\n");
		}

		showPrompt();
	}

	/**
	 * Captures output from a command execution.
	 *
	 * @param command the command to execute
	 * @return the captured output
	 */
	private String captureOutput(Runnable command) {
		// Redirect System.out temporarily
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		java.io.PrintStream ps = new java.io.PrintStream(baos);
		java.io.PrintStream oldOut = System.out;
		java.io.PrintStream oldErr = System.err;

		try {
			System.setOut(ps);
			System.setErr(ps);
			command.run();
			System.out.flush();
			System.err.flush();
			return baos.toString();
		} finally {
			System.setOut(oldOut);
			System.setErr(oldErr);
		}
	}

	/**
	 * Shows the command prompt based on router mode.
	 */
	private void showPrompt() {
		String prompt;
		if (router.getMode() == RouterMode.OPERATIONAL) {
			prompt = "vyos@vyos$ ";
		} else if (router.getMode() == RouterMode.CONFIGURATION) {
			prompt = "vyos@vyos# ";
		} else {
			prompt = "> ";
		}
		appendOutput(prompt);
	}

	/**
	 * Appends text to the output area.
	 *
	 * @param text the text to append
	 */
	private void appendOutput(String text) {
		outputArea.appendText(text);
		outputArea.setScrollTop(Double.MAX_VALUE);
	}
}

