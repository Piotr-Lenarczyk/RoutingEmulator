package org.uj.routingemulator.router.cli;

public interface CommandResult {
	boolean isSuccess();

	String getOutput();
}