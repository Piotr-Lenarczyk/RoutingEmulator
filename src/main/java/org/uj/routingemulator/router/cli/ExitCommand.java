package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.exceptions.UncommittedChangesException;

import java.io.PrintWriter;

/**
 * Command to exit configuration mode and return to operational mode.
 * Only works when router is in configuration mode.
 * If there are uncommitted changes, this command will fail.
 */
public class ExitCommand implements RouterCommand {
	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		if (router.getMode() != RouterMode.CONFIGURATION) {
			out.println("\n\tInvalid command: [exit]\n");
			out.flush();
		} else {
			try {
				router.setMode(RouterMode.OPERATIONAL);
				out.println("exit");
				out.flush();
			} catch (UncommittedChangesException e) {
				out.println(e.getMessage());
				out.flush();
			}
		}
	}

	@Override
	public boolean matches(String command) {
		return command.trim().equals("exit");
	}

	@Override
	public String getCommandPattern() {
		return "exit";
	}

	@Override
	public String getDescription() {
		return "Exit configuration mode";
	}
}
