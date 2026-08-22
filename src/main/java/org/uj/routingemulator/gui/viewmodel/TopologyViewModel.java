package org.uj.routingemulator.gui.viewmodel;

import org.uj.routingemulator.common.topology.Connection;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.switching.Switch;

import java.util.List;

public record TopologyViewModel(
		List<Router> routers,
		List<Switch> switches,
		List<Host> hosts,
		List<Connection> connections
) {
}