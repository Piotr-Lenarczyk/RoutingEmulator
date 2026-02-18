package org.uj.routingemulator.gui;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Terminal-like widget using a single TextArea for unified display.
 * Supports:
 * - Command history (arrow up/down)
 * - Tab completion
 * - Protected prompt area
 *
 * Note: ANSI color codes are stripped since TextArea doesn't support rich text.
 * For color support, a more complex solution with TextFlow would be needed.
 */
public class SimpleTerminalTextArea extends TextArea {

	private final List<String> commandHistory = new ArrayList<>();
	private int historyIndex = -1;
	private int promptStartPosition = 0;
	private String currentPrompt = "";

	private Consumer<String> onCommandSubmit;
	private BiConsumer<String, Consumer<List<String>>> onTabComplete;

	public SimpleTerminalTextArea() {
		super();
		setWrapText(true);
		setEditable(true);
		setStyle("-fx-control-inner-background: #1e1e1e; " +
				"-fx-text-fill: #d4d4d4; " +
				"-fx-font-family: 'Courier New'; " +
				"-fx-font-size: 12px;");

		// Setup key event handlers
		addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

		// Prevent editing before the prompt
		addEventFilter(KeyEvent.KEY_TYPED, event -> {
			if (getCaretPosition() < promptStartPosition) {
				event.consume();
			}
		});
	}

	/**
	 * Sets the command submit handler.
	 */
	public void setOnCommandSubmit(Consumer<String> handler) {
		this.onCommandSubmit = handler;
	}

	/**
	 * Sets the tab completion handler.
	 */
	public void setOnTabComplete(BiConsumer<String, Consumer<List<String>>> handler) {
		this.onTabComplete = handler;
	}

	/**
	 * Restores terminal content from a buffer and updates the prompt position.
	 * This should be used instead of setText() when restoring session history.
	 *
	 * @param content The terminal content to restore
	 */
	public void restoreFromBuffer(String content) {
		setText(content);
		// Update prompt start position to be at the end of the restored content
		// This ensures that the protection mechanism works correctly
		promptStartPosition = getLength();
		positionCaret(getLength());
	}

	/**
	 * Displays a prompt and waits for input.
	 */
	public void showPrompt(String prompt) {
		this.currentPrompt = prompt;
		appendText(prompt);
		promptStartPosition = getLength();
		positionCaret(getLength());
	}

	/**
	 * Appends text to the terminal.
	 * Method name kept for compatibility with existing code.
	 */
	public void appendColoredText(String text) {
		appendText(text);
	}

	/**
	 * Handles key press events for history and shortcuts.
	 */
	private void handleKeyPress(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			handleEnter();
			event.consume();
		} else if (event.getCode() == KeyCode.UP) {
			navigateHistory(-1);
			event.consume();
		} else if (event.getCode() == KeyCode.DOWN) {
			navigateHistory(1);
			event.consume();
		} else if (event.getCode() == KeyCode.TAB) {
			handleTab();
			event.consume();
		} else if (event.getCode() == KeyCode.U && event.isControlDown()) {
			// Ctrl+U: Clear the command (but keep prompt)
			replaceText(promptStartPosition, getLength(), "");
			event.consume();
		} else if (event.getCode() == KeyCode.BACK_SPACE) {
			// Prevent backspace from deleting the prompt
			if (getCaretPosition() <= promptStartPosition) {
				event.consume();
			}
		} else if (event.getCode() == KeyCode.DELETE) {
			// Allow delete only after the prompt
			if (getCaretPosition() < promptStartPosition) {
				event.consume();
			}
		} else if (event.getCode() == KeyCode.LEFT) {
			// Prevent moving cursor before the prompt
			if (getCaretPosition() <= promptStartPosition) {
				event.consume();
			}
		} else if (event.getCode() == KeyCode.HOME) {
			// Home key should move to start of command, not start of line
			positionCaret(promptStartPosition);
			event.consume();
		}

		// Additional check: if user somehow manages to position cursor before prompt, move it back
		Platform.runLater(() -> {
			if (getCaretPosition() < promptStartPosition) {
				positionCaret(promptStartPosition);
			}
		});
	}

	/**
	 * Handles Enter key - submits command.
	 */
	private void handleEnter() {
		String fullText = getText();

		// Extract command (everything after the prompt)
		String command = "";
		if (fullText.length() > promptStartPosition) {
			command = fullText.substring(promptStartPosition).trim();
		}

		appendText("\n");

		if (!command.isEmpty()) {
			commandHistory.add(command);
			historyIndex = commandHistory.size();

			if (onCommandSubmit != null) {
				onCommandSubmit.accept(command);
			}
		} else {
			// Empty command
			if (onCommandSubmit != null) {
				onCommandSubmit.accept("");
			}
		}
	}

	/**
	 * Handles Tab key - triggers completion.
	 */
	private void handleTab() {
		String fullText = getText();

		// Extract command (without prompt)
		final String currentInput = fullText.length() > promptStartPosition
			? fullText.substring(promptStartPosition)
			: "";

		if (onTabComplete != null) {
			onTabComplete.accept(currentInput, completions -> {
				if (completions != null && !completions.isEmpty()) {
					if (completions.size() == 1) {
						// Single completion - replace only the last word
						String completion = completions.get(0);

						// Find the position where the last word starts
						// If input ends with space, we're completing a new empty word
						boolean endsWithSpace = currentInput.endsWith(" ");
						String trimmedInput = currentInput.trim();

						int wordStartPos;
						if (endsWithSpace || trimmedInput.isEmpty()) {
							// Completing after a space or empty input
							wordStartPos = promptStartPosition + currentInput.length();
						} else {
							// Completing a partial word
							int lastSpacePos = currentInput.lastIndexOf(' ');
							wordStartPos = promptStartPosition + (lastSpacePos >= 0 ? lastSpacePos + 1 : 0);
						}

						// Replace from word start to end with the completion, and add a space
						replaceText(wordStartPos, getLength(), completion + " ");
						positionCaret(getLength());
					} else {
						// Multiple completions - show them
						appendText("\n");
						for (String completion : completions) {
							appendText(completion + "  ");
						}
						appendText("\n");
						showPrompt(currentPrompt);
						appendText(currentInput);
					}
				}
			});
		}
	}

	/**
	 * Navigates command history.
	 */
	private void navigateHistory(int direction) {
		if (commandHistory.isEmpty()) {
			return;
		}

		historyIndex += direction;
		historyIndex = Math.max(0, Math.min(historyIndex, commandHistory.size()));

		if (historyIndex < commandHistory.size()) {
			replaceText(promptStartPosition, getLength(), commandHistory.get(historyIndex));
		} else {
			replaceText(promptStartPosition, getLength(), "");
		}
		positionCaret(getLength());
	}
}

