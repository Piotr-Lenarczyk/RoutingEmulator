package org.uj.routingemulator.router;

import org.uj.routingemulator.router.exceptions.NoChangesToCommitException;

import java.util.List;

/**
 * Manages the staged state for a router undergoing configuration changes.
 */
public class RouterConfigurationSession {
	private RoutingTable stagedRoutingTable;
	private List<RouterInterface> stagedInterfaces;
	private boolean hasUncommittedChanges;

	public RouterConfigurationSession(Router router) {
		discardChanges(router);
	}

	public RoutingTable getStagedRoutingTable() {
		return stagedRoutingTable;
	}

	public List<RouterInterface> getStagedInterfaces() {
		return stagedInterfaces;
	}

	public boolean hasUncommittedChanges() {
		return hasUncommittedChanges;
	}

	public void setHasUncommittedChanges(boolean hasUncommittedChanges) {
		this.hasUncommittedChanges = hasUncommittedChanges;
	}

	public void commitChanges(Router router) {
		if (!hasUncommittedChanges) {
			throw new NoChangesToCommitException("No configuration changes to commit");
		}

		for (RouterInterface stagedIf : stagedInterfaces) {
			RouterInterface existing = router.getInterfaces().stream()
					.filter(i -> i.getInterfaceName().equals(stagedIf.getInterfaceName()))
					.findFirst()
					.orElse(null);

			if (existing != null) {
				existing.setInterfaceAddress(stagedIf.getInterfaceAddress());
				existing.setMacAddress(stagedIf.getMacAddress());
				existing.setDescription(stagedIf.getDescription());
				existing.setVrf(stagedIf.getVrf());
				existing.setMtu(stagedIf.getMtu());
				existing.setStatus(stagedIf.getStatus());
			} else {
				router.getInterfaces().add(new RouterInterface(stagedIf));
			}
		}

		router.setRoutingTable(RoutingTableCopier.copyRoutingTableWithUpdatedInterfaces(stagedRoutingTable, router.getInterfaces()));
		this.hasUncommittedChanges = false;
	}

	public void discardChanges(Router router) {
		this.stagedInterfaces = RoutingTableCopier.deepCopyInterfaces(router.getInterfaces());
		this.stagedRoutingTable = RoutingTableCopier.copyRoutingTableWithUpdatedInterfaces(router.getRoutingTable(), this.stagedInterfaces);
		this.hasUncommittedChanges = false;
	}

	public void clearStagedConfiguration() {
		for (RouterInterface iface : stagedInterfaces) {
			iface.setInterfaceAddress(null);
			if (iface.getStatus().getAdmin() == AdminState.ADMIN_DOWN) {
				iface.enable();
			}
		}
		this.stagedRoutingTable = new RoutingTable();
		this.hasUncommittedChanges = true;
	}
}