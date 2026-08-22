package org.uj.routingemulator.router.session;

import org.uj.routingemulator.router.exceptions.InvalidModeException;
import org.uj.routingemulator.router.exceptions.NoChangesToCommitException;
import org.uj.routingemulator.router.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class ConfigurationSession {
	private static final Logger logger = Logger.getLogger(ConfigurationSession.class.getName());

	private final Router router;
	private RoutingTable stagedRoutingTable;
	private List<RouterInterface> stagedInterfaces;
	private boolean hasUncommittedChanges;

	public ConfigurationSession(Router router) {
		this.router = router;
		this.stagedRoutingTable = new RoutingTable();
		this.stagedInterfaces = new ArrayList<>();
		this.hasUncommittedChanges = false;
	}

	public boolean hasUncommittedChanges() {
		return hasUncommittedChanges;
	}

	public void setHasUncommittedChanges(boolean hasUncommittedChanges) {
		this.hasUncommittedChanges = hasUncommittedChanges;
	}

	public List<RouterInterface> getStagedInterfaces() {
		return Collections.unmodifiableList(stagedInterfaces);
	}

	public void setStagedInterfaces(List<RouterInterface> stagedInterfaces) {
		this.stagedInterfaces = stagedInterfaces;
	}

	public RoutingTable getStagedRoutingTable() {
		return stagedRoutingTable;
	}

	public void setStagedRoutingTable(RoutingTable stagedRoutingTable) {
		this.stagedRoutingTable = stagedRoutingTable;
	}

	private void requireConfigMode(String cmd) {
		if (router.getMode() != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to " + cmd + " while in " + router.getMode() + " mode");
			throw new InvalidModeException("Invalid command: " + cmd);
		}
	}

	public void commit() {
		requireConfigMode("[commit]");
		if (!hasUncommittedChanges) {
			throw new NoChangesToCommitException("No configuration changes to commit");
		}
		List<RouterInterface> newInterfaces = RoutingTableCopier.deepCopyInterfaces(stagedInterfaces);
		RoutingTable newTable = RoutingTableCopier.copyRoutingTableWithUpdatedInterfaces(stagedRoutingTable, newInterfaces);
		RouterConfiguration newConfig = new RouterConfiguration(newInterfaces, newTable);
		router.applyConfiguration(newConfig);
		this.hasUncommittedChanges = false;
		logger.info("%s: Commit complete".formatted(router.getName()));
	}

	public void discard() {
		this.stagedInterfaces = RoutingTableCopier.deepCopyInterfaces(router.getInterfaces());
		this.stagedRoutingTable = RoutingTableCopier.copyRoutingTableWithUpdatedInterfaces(router.getRoutingTable(), this.stagedInterfaces);
		this.hasUncommittedChanges = false;
	}

	public void resetCandidateConfiguration() {
		requireConfigMode("clear configuration");
		for (RouterInterface iface : stagedInterfaces) {
			iface.setInterfaceAddress(null);
			if (iface.getStatus().admin() == AdminState.ADMIN_DOWN) {
				iface.enable();
			}
		}
		this.stagedRoutingTable = new RoutingTable();
		this.hasUncommittedChanges = true;
	}
}