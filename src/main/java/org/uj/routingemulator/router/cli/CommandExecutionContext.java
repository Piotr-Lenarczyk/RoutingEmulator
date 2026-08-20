package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.common.NetworkTopology;
import org.uj.routingemulator.router.Router;

public record CommandExecutionContext(Router router, NetworkTopology topology, CommandOutput output) {
}