package org.uj.routingemulator.router.cli;

import java.io.PrintWriter;

public class PrintWriterCommandOutput implements CommandOutput {
	private final PrintWriter writer;

	public PrintWriterCommandOutput(PrintWriter writer) {
		this.writer = writer;
	}

	@Override
	public void print(String text) {
		writer.print(text);
		writer.flush();
	}

	@Override
	public void println(String text) {
		writer.println(text);
		writer.flush();
	}
}