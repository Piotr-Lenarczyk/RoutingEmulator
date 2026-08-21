package org.uj.routingemulator.router.cli;

public record CommandSuccess(String output) implements CommandResult {
	@Override
	public boolean isSuccess() {
		return true;
	}

	@Override
	public String getOutput() {
		return output;
	}
}