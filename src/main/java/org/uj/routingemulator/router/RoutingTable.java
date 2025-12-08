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

	/**
	 * Copy constructor that creates a deep copy of the routing table.
	 * Creates new StaticRoutingEntry objects to avoid sharing mutable state.
	 *
	 * @param other RoutingTable to copy
	 */
	public RoutingTable(RoutingTable other) {
		this.routingEntries = new ArrayList<>();
		for (StaticRoutingEntry entry : other.getRoutingEntries()) {
			this.routingEntries.add(new StaticRoutingEntry(entry));
		}
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
