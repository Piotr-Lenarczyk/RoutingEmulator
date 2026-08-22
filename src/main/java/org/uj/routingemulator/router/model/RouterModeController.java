package org.uj.routingemulator.router.model;

import org.uj.routingemulator.router.exceptions.UncommittedChangesException;

public class RouterModeController {
	private RouterModeController() {
	}

	public static void setMode(Router router, RouterMode mode) {
		if (mode == RouterMode.OPERATIONAL && router.getMode() == RouterMode.CONFIGURATION && router.hasUncommittedChanges()) {
			throw new UncommittedChangesException("Cannot exit: configuration modified.\nUse 'exit discard' to discard the changes and exit.\n[edit]");
		}
		if (mode == RouterMode.CONFIGURATION && router.getMode() == RouterMode.OPERATIONAL) {
			router.getConfigSession().discard();
		}
		router.setMode(mode);
	}

	public static void setModeForced(Router router, RouterMode mode) {
		if (mode == RouterMode.OPERATIONAL && router.getMode() == RouterMode.CONFIGURATION && router.hasUncommittedChanges()) {
			router.getConfigSession().discard();
		}
		router.setMode(mode);
	}
}