package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.common.topology.NetworkTopology;
import org.uj.routingemulator.router.model.Router;

public record CommandExecutionContext(Router router, NetworkTopology topology, CommandOutput output) {
}