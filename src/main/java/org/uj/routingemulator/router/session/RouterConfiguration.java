package org.uj.routingemulator.router.session;

import org.uj.routingemulator.router.model.RouterInterface;
import org.uj.routingemulator.router.model.RoutingTable;

import java.util.List;

public record RouterConfiguration(List<RouterInterface> interfaces, RoutingTable routingTable) {
	public RouterConfiguration {
		interfaces = List.copyOf(interfaces);
	}
}