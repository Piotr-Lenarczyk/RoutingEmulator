package org.uj.routingemulator.router;

import java.util.List;

public record RouterConfiguration(List<RouterInterface> interfaces, RoutingTable routingTable) {
	public RouterConfiguration {
		interfaces = List.copyOf(interfaces);
	}
}