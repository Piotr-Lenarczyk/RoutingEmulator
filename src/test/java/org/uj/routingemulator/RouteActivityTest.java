package org.uj.routingemulator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.ShowIpRouteCommand;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteActivityTest {
	private static Router routerWithInterfaces(String name, String... interfaceNames) {
		List<RouterInterface> interfaces = new ArrayList<>();
		for (String interfaceName : interfaceNames) {
			interfaces.add(new RouterInterface(interfaceName));
		}
		return new Router(name, interfaces);
	}

	private static void configureInterface(Router router, String name, String address) {
		if (router.getMode() != RouterMode.CONFIGURATION) {
			router.setMode(RouterMode.CONFIGURATION);
		}
		router.configureInterface(name, InterfaceAddress.fromString(address));
	}

	private static void commit(Router router) {
		router.commitChanges();
		router.setMode(RouterMode.OPERATIONAL);
	}

	private static Host host(String name, String address, String gateway) {
		return new Host(name, new HostInterface("eth0", Subnet.fromString(address),
				org.uj.routingemulator.common.IPAddress.fromString(gateway)));
	}

	private static String showRoutes(Router router, NetworkTopology topology) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		CLIContext.setWriter(new PrintWriter(output, true));
		CLIContext.setNetworkTopology(topology);
		new ShowIpRouteCommand().execute(router);
		return output.toString();
	}

	private static boolean hasFibMarkedPath(String output, String nextHop) {
		return output.lines().anyMatch(line -> line.contains(nextHop)
				&& (line.startsWith("S>*") || line.startsWith("  *")));
	}

	@AfterEach
	void clearCliContext() {
		CLIContext.clear();
	}

	@Test
	void showIpRouteDoesNotMarkRoutesOnDisconnectedInterfaceAsActive() {
		Router router = routerWithInterfaces("R1", "eth0", "eth1");
		configureInterface(router, "eth0", "192.0.2.1/24");
		configureInterface(router, "eth1", "203.0.113.1/24");
		router.addRoute(new StaticRoutingEntry(Subnet.fromString("198.51.100.0/24"),
				router.findFromName("eth1")));
		commit(router);

		NetworkTopology topology = new NetworkTopology();
		Host sourcePeer = host("source-peer", "192.0.2.2/24", "192.0.2.1");
		Host transitPeer = host("transit-peer", "203.0.113.2/24", "203.0.113.1");
		topology.addRouter(router);
		topology.addHost(sourcePeer);
		topology.addHost(transitPeer);
		topology.addConnection(new Connection(router.findFromName("eth0"), sourcePeer.getHostInterface()));
		Connection transitConnection = new Connection(router.findFromName("eth1"), transitPeer.getHostInterface());
		topology.addConnection(transitConnection);
		topology.removeConnection(transitConnection);

		String output = showRoutes(router, topology);

		assertTrue(output.contains("C>* 192.0.2.0/24"), "The active connected route should remain installed");
		assertFalse(output.contains("C>* 203.0.113.0/24"), "A disconnected connected route must not be active");
		assertFalse(output.contains("S>* 198.51.100.0/24"), "A route through a disconnected interface must not be active");
	}

	@Test
	void showIpRouteMarksOnlyReachableEqualCostNextHopAsInstalled() {
		Router router = routerWithInterfaces("R1", "eth0");
		configureInterface(router, "eth0", "192.0.2.1/24");
		Subnet destination = Subnet.fromString("198.51.100.0/24");
		router.addRoute(new StaticRoutingEntry(destination,
				IPAddress.fromString("1.2.3.5"), 10));
		router.addRoute(new StaticRoutingEntry(destination,
				IPAddress.fromString("192.0.2.2"), 10));
		commit(router);

		Router neighbor = routerWithInterfaces("R2", "eth0");
		configureInterface(neighbor, "eth0", "192.0.2.2/24");
		commit(neighbor);
		NetworkTopology topology = new NetworkTopology();
		topology.addRouter(router);
		topology.addRouter(neighbor);
		topology.addConnection(new Connection(router.findFromName("eth0"), neighbor.findFromName("eth0")));

		String output = showRoutes(router, topology);

		assertTrue(hasFibMarkedPath(output, "192.0.2.2"), "The reachable next-hop should be installed in the FIB");
		assertFalse(hasFibMarkedPath(output, "1.2.3.5"), "The unreachable next-hop must not be marked as installed");
	}

	@Test
	void pingSkipsUnreachableNextHopWhenAnEqualCostPathIsReachable() {
		Router first = routerWithInterfaces("R1", "eth0", "eth1");
		configureInterface(first, "eth0", "192.0.2.1/24");
		configureInterface(first, "eth1", "203.0.113.1/24");
		Subnet destinationSubnet = Subnet.fromString("198.51.100.0/24");
		first.addRoute(new StaticRoutingEntry(destinationSubnet,
				IPAddress.fromString("1.2.3.5"), 10));
		first.addRoute(new StaticRoutingEntry(destinationSubnet,
				IPAddress.fromString("203.0.113.2"), 10));
		commit(first);

		Router second = routerWithInterfaces("R2", "eth0", "eth1");
		configureInterface(second, "eth0", "203.0.113.2/24");
		configureInterface(second, "eth1", "198.51.100.1/24");
		second.addRoute(new StaticRoutingEntry(Subnet.fromString("192.0.2.0/24"),
				second.findFromName("eth0")));
		commit(second);

		Host source = host("source", "192.0.2.10/24", "192.0.2.1");
		Host destination = host("destination", "198.51.100.2/24", "198.51.100.1");
		NetworkTopology topology = new NetworkTopology();
		topology.addHost(source);
		topology.addHost(destination);
		topology.addRouter(first);
		topology.addRouter(second);
		topology.addConnection(new Connection(source.getHostInterface(), first.findFromName("eth0")));
		topology.addConnection(new Connection(first.findFromName("eth1"), second.findFromName("eth0")));
		topology.addConnection(new Connection(second.findFromName("eth1"), destination.getHostInterface()));

		assertEquals(4, source.ping("198.51.100.2", topology).getReceived(),
				"An unreachable equal-cost next-hop must not mask a reachable path");
	}
}