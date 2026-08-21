package org.uj.routingemulator.router.cli;

public record CommandFailure(String output) implements CommandResult {
	@Override
	public boolean isSuccess() {
		return false;
	}

	@Override
	public String getOutput() {
		return output;
	}
}