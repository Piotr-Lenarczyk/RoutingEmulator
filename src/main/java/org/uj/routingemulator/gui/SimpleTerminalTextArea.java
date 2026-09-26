package org.uj.routingemulator.gui;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.Setter;
import org.jline.reader.Candidate;

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
 * <p>
 * Note: ANSI color codes are stripped since TextArea doesn't support rich text.
 * For color support, a more complex solution with TextFlow would be needed.
 */
public class SimpleTerminalTextArea extends TextArea {

	private final List<String> commandHistory = new ArrayList<>();
	private int historyIndex = -1;
	private int promptStartPosition = 0;
	private String currentPrompt = "";

	@Setter
	private Consumer<String> onCommandSubmit;

	@Setter
	private BiConsumer<String, Consumer<List<Candidate>>> onTabComplete;

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

	public void restoreFromBuffer(String content) {
		setText(content);
		promptStartPosition = getLength();
		positionCaret(getLength());
	}

	public List<String> getCommandHistory() {
		return new ArrayList<>(commandHistory);
	}

	public void loadCommandHistory(List<String> history) {
		this.commandHistory.clear();
		if (history != null) {
			this.commandHistory.addAll(history);
		}
		this.historyIndex = this.commandHistory.size();
	}

	public void showPrompt(String prompt) {
		this.currentPrompt = prompt;
		appendText(prompt);
		promptStartPosition = getLength();
		positionCaret(getLength());
	}

	public void appendColoredText(String text) {
		appendText(text);
	}

	@SuppressWarnings("java:S6916")
	private void handleKeyPress(KeyEvent event) {
		switch (event.getCode()) {
			case KeyCode.ENTER -> {
				handleEnter();
				event.consume();
			}
			case KeyCode.UP -> {
				navigateHistory(-1);
				event.consume();
			}
			case KeyCode.DOWN -> {
				navigateHistory(1);
				event.consume();
			}
			case KeyCode.TAB -> {
				handleTab();
				event.consume();
			}
			case KeyCode.U -> {
				if (event.isControlDown()) {
					replaceText(promptStartPosition, getLength(), "");
					event.consume();
				}
			}
			case KeyCode.BACK_SPACE -> {
				if (getCaretPosition() <= promptStartPosition) {
					event.consume();
				}
			}
			case KeyCode.DELETE -> {
				if (getCaretPosition() < promptStartPosition) {
					event.consume();
				}
			}
			case KeyCode.LEFT -> {
				if (getCaretPosition() <= promptStartPosition) {
					event.consume();
				}
			}
			case KeyCode.HOME -> {
				positionCaret(promptStartPosition);
				event.consume();
			}
			default -> {
				Platform.runLater(() -> {
					if (getCaretPosition() < promptStartPosition) {
						positionCaret(promptStartPosition);
					}
				});
			}
		}
	}

	private void handleEnter() {
		String fullText = getText();
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
			if (onCommandSubmit != null) {
				onCommandSubmit.accept("");
			}
		}
	}

	private void handleTab() {
		String fullText = getText();
		final String currentInput = fullText.length() > promptStartPosition ?
				fullText.substring(promptStartPosition) : "";

		if (onTabComplete != null) {
			onTabComplete.accept(currentInput, completions -> performTabCompletion(completions, currentInput));
		}
	}

	private void performTabCompletion(List<Candidate> candidates, String currentInput) {
		if (candidates != null && !candidates.isEmpty()) {

			// Distinguish between actual input completions vs generic hint rules
			List<Candidate> realCompletions = candidates.stream()
					.filter(Candidate::complete)
					.toList();

			if (candidates.size() == 1 && realCompletions.size() == 1) {
				// Autocomplete immediately only if there's exactly 1 candidate and it's selectable
				handleSingleCompletion(realCompletions.getFirst().value(), currentInput);
			} else {
				// Otherwise print the VyOS styled menu
				appendText("\nPossible completions:\n");
				for (Candidate c : candidates) {
					String val = c.displ() != null ? c.displ() : c.value();
					String desc = c.descr() != null ? c.descr() : "";

					if (desc.isEmpty()) {
						appendText(String.format(" > %-14s%n", val));
					} else {
						appendText(String.format(" > %-14s %s%n", val, desc));
					}
				}
				appendText("\n");
				showPrompt(currentPrompt);
				appendText(currentInput);
			}
		}
	}

	private void handleSingleCompletion(String completion, String currentInput) {
		boolean endsWithSpace = currentInput.endsWith(" ");
		String trimmedInput = currentInput.trim();

		int wordStartPos;
		if (endsWithSpace || trimmedInput.isEmpty()) {
			wordStartPos = promptStartPosition + currentInput.length();
		} else {
			int lastSpacePos = currentInput.lastIndexOf(' ');
			wordStartPos = promptStartPosition + (lastSpacePos >= 0 ? lastSpacePos + 1 : 0);
		}

		replaceText(wordStartPos, getLength(), completion + " ");
		positionCaret(getLength());
	}

	private void navigateHistory(int direction) {
		if (commandHistory.isEmpty()) {
			return;
		}

		historyIndex += direction;
		historyIndex = Math.clamp(historyIndex, 0, commandHistory.size());

		if (historyIndex < commandHistory.size()) {
			replaceText(promptStartPosition, getLength(), commandHistory.get(historyIndex));
		} else {
			replaceText(promptStartPosition, getLength(), "");
		}
		positionCaret(getLength());
	}
}