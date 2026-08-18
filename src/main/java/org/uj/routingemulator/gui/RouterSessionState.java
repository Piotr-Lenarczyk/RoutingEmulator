package org.uj.routingemulator.gui;

/**
 * Maintains session state for a JavaFX terminal tied to a specific router.
 * Used exclusively by the GUI layer to preserve terminal buffers.
 */
public class RouterSessionState {
	private final StringBuilder terminalBuffer;

	public RouterSessionState() {
		this.terminalBuffer = new StringBuilder();
	}

	public StringBuilder getTerminalBuffer() {
		return terminalBuffer;
	}

	public void updateBuffer(String content) {
		terminalBuffer.setLength(0);
		terminalBuffer.append(content);
	}
}