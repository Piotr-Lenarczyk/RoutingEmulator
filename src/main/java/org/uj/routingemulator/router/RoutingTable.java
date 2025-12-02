package org.uj.routingemulator.router;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode
public class RoutingTable {
	private List<StaticRoutingEntry> routingEntries;

	public RoutingTable() {
		this.routingEntries = new ArrayList<>();
	}

	public RoutingTable(RoutingTable stagedRoutingTable) {
		this.routingEntries = new ArrayList<>(stagedRoutingTable.getRoutingEntries());
	}

	public void addRoute(StaticRoutingEntry entry) {
		this.routingEntries.add(entry);
	}

	public boolean contains(StaticRoutingEntry entry) {
		return this.routingEntries.contains(entry);
	}

	/**
	 * Removes a routing entry from the table.
	 *
	 * @param entry Routing entry to remove
	 */
	public void removeRoute(StaticRoutingEntry entry) {
		this.routingEntries.remove(entry);
	}

	public void disableRoute(StaticRoutingEntry entry) {
		if (this.routingEntries.contains(entry)) {
			StaticRoutingEntry route = this.routingEntries.get(this.routingEntries.indexOf(entry));
			route.disable();
		}
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("RoutingTable{" + "routingEntries=\n");
		for (StaticRoutingEntry entry : routingEntries) {
			stringBuilder.append("\t").append(entry.toString()).append("\n");
		}
		stringBuilder.append("}");
		return stringBuilder.toString();
	}
}
