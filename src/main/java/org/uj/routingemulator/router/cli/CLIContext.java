package org.uj.routingemulator.router.cli;

import java.io.PrintWriter;

/**
 * Thread-local context for CLI output.
 * Allows commands to access the current terminal writer without passing it explicitly.
 * Falls back to System.out if no writer is set for testing purposes.
 */
public class CLIContext {
	private static final ThreadLocal<PrintWriter> writer = new ThreadLocal<>();

	/**
	 * Get the current terminal writer for this thread.
	 * If no writer is set, returns a PrintWriter wrapping System.out.
	 *
	 * @return PrintWriter for terminal output
	 */
	public static PrintWriter getWriter() {
		PrintWriter w = writer.get();
		if (w == null) {
			w = new PrintWriter(System.out, true);
		}
		return w;
	}

	/**
	 * Sets the current terminal writer for this thread.
	 *
	 * @param w PrintWriter to use for output
	 */
	public static void setWriter(PrintWriter w) {
		writer.set(w);
	}

	/**
	 * Clears the terminal writer for this thread.
	 * Should be called whtn CLI session ends to avoid memory leaks.
	 */
	public static void clear() {
		writer.remove();
	}
}
