package org.uj.routingemulator;

import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.RouterCLIParser;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class E2ETest {
	@Test
	void testNextHopSubnetNotANetworkAddress() {
		Router router = new Router("R1");
		router.setMode(RouterMode.CONFIGURATION);

		// Prepare CLI parser and capture output
		RouterCLIParser parser = new RouterCLIParser();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		CLIContext.setWriter(pw);

		// Execute CLI command that contains mask in next-hop
		parser.executeCommand("set protocols static route 1.1.1.1/8 next-hop 2.2.2.2", router);

		String out = sw.toString();
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(1, 1, 1, 1), new SubnetMask(8)), new IPAddress(2, 2, 2, 2));
		assertThat(out).contains("Error: 1.1.1.1/8 is not a valid IPv4 prefix");
		assertThat(out).contains("Invalid value");
		assertThat(out).contains("Value validation failed");
		assertThat(out).contains("Set failed");
		assertThat(out).contains("[edit]");
		assertFalse(router.getRoutingTable().contains(entry));
	}

	@Test
	void testNextHopAddressContainsMask() {
		Router router = new Router("R1");
		router.setMode(RouterMode.CONFIGURATION);

		// Prepare CLI parser and capture output
		RouterCLIParser parser = new RouterCLIParser();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		CLIContext.setWriter(pw);

		// Execute CLI command that contains mask in next-hop
		parser.executeCommand("set protocols static route 1.1.1.0/8 next-hop 2.2.2.2/8", router);

		String out = sw.toString();
		assertThat(out).contains("Error: 2.2.2.2/8 is not a valid IPv4 prefix");
		assertThat(out).contains("Invalid value");
		assertThat(out).contains("Value validation failed");
		assertThat(out).contains("Set failed");
		assertThat(out).contains("[edit]");
		CLIContext.clear();
	}

	@Test
	void testTripleRouterSetup() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("H1", new HostInterface("Ethernet0", new Subnet(new IPAddress(10, 0, 0, 2), new SubnetMask(8)), new IPAddress(10, 0, 0, 1)));
		Host h2 = new Host("H2", new HostInterface("Ethernet0", new Subnet(new IPAddress(20, 0, 0, 2), new SubnetMask(8)), new IPAddress(20, 0, 0, 1)));
		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r3 = new Router("R3", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

		topology.addHost(h1);
		topology.addHost(h2);
		topology.addRouter(r1);
		topology.addRouter(r2);
		topology.addRouter(r3);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r2.getInterfaces().get(1), r3.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r3.getInterfaces().get(1), h2.getHostInterface()));

		// Configure interfaces
		r1.setMode(RouterMode.CONFIGURATION);
		r1.configureInterface("eth0", InterfaceAddress.fromString("10.0.0.1/8"));
		r1.configureInterface("eth1", InterfaceAddress.fromString("192.168.0.1/25"));
		r1.commitChanges();
		r2.setMode(RouterMode.CONFIGURATION);
		r2.configureInterface("eth0", InterfaceAddress.fromString("192.168.0.2/25"));
		r2.configureInterface("eth1", InterfaceAddress.fromString("192.168.0.129/26"));
		r2.commitChanges();
		r3.setMode(RouterMode.CONFIGURATION);
		r3.configureInterface("eth0", InterfaceAddress.fromString("192.168.0.130/26"));
		r3.configureInterface("eth1", InterfaceAddress.fromString("20.0.0.1/8"));
		r3.commitChanges();

		// Test connectivity between directly connected routers
		PingStatistics stats = r1.ping("192.168.0.2", topology);
		assertEquals(4, stats.getSent());
		assertEquals(4, stats.getReceived(), "Should receive a reply from a directly connected router");

		PingStatistics stats1 = r2.ping("192.168.0.130", topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should receive a reply from a directly connected router");

		PingStatistics stats2 = r1.ping("192.168.0.130", topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Should not receive a reply from an indirectly connected router");

		// Configure static route only on R1
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 128), new SubnetMask(26)), r1.findFromName("eth1")));
		r1.commitChanges();

		PingStatistics stats3 = r1.ping("192.168.0.130", topology);
		assertEquals(4, stats3.getSent());
		assertEquals(0, stats3.getReceived(), "Should not receive a reply due to packet drop at R3 (no route back to R1)");

		// Configure return route on R3
		r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 0), new SubnetMask(25)), r3.findFromName("eth0")));
		r3.commitChanges();

		PingStatistics stats4 = r1.ping("192.168.0.130", topology);
		assertEquals(4, stats4.getSent());
		assertEquals(4, stats4.getReceived(), "Should succeed due to correct return route");
	}

	@Test
	void testTripleRouterSetupEndToEnd() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("H1", new HostInterface("Ethernet0", new Subnet(new IPAddress(10, 0, 0, 2), new SubnetMask(8)), new IPAddress(10, 0, 0, 1)));
		Host h2 = new Host("H2", new HostInterface("Ethernet0", new Subnet(new IPAddress(20, 0, 0, 2), new SubnetMask(8)), new IPAddress(20, 0, 0, 1)));
		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r3 = new Router("R3", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

		topology.addHost(h1);
		topology.addHost(h2);
		topology.addRouter(r1);
		topology.addRouter(r2);
		topology.addRouter(r3);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r2.getInterfaces().get(1), r3.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r3.getInterfaces().get(1), h2.getHostInterface()));

		// Configure interfaces
		r1.setMode(RouterMode.CONFIGURATION);
		r1.configureInterface("eth0", InterfaceAddress.fromString("10.0.0.1/8"));
		r1.configureInterface("eth1", InterfaceAddress.fromString("192.168.0.1/25"));
		r1.commitChanges();
		r2.setMode(RouterMode.CONFIGURATION);
		r2.configureInterface("eth0", InterfaceAddress.fromString("192.168.0.2/25"));
		r2.configureInterface("eth1", InterfaceAddress.fromString("192.168.0.129/26"));
		r2.commitChanges();
		r3.setMode(RouterMode.CONFIGURATION);
		r3.configureInterface("eth0", InterfaceAddress.fromString("192.168.0.130/26"));
		r3.configureInterface("eth1", InterfaceAddress.fromString("20.0.0.1/8"));
		r3.commitChanges();

		// Test connectivity between directly connected routers
		PingStatistics stats = r1.ping("192.168.0.2", topology);
		assertEquals(4, stats.getSent());
		assertEquals(4, stats.getReceived(), "Should receive a reply from a directly connected router");

		PingStatistics stats1 = r2.ping("192.168.0.130", topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should receive a reply from a directly connected router");

		PingStatistics stats2 = r1.ping("192.168.0.130", topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Should not receive a reply from an indirectly connected router");

		// Configure static route only on R1
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 128), new SubnetMask(26)), r1.findFromName("eth1")));
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r1.findFromName("eth1")));
		r1.commitChanges();

		PingStatistics stats3 = r1.ping("192.168.0.130", topology);
		assertEquals(4, stats3.getSent());
		assertEquals(0, stats3.getReceived(), "Should not receive a reply due to packet drop at R3 (no route back to R1)");

		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth0")));
		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth1")));
		r2.commitChanges();

		// Configure return route on R3
		r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 0), new SubnetMask(25)), r3.findFromName("eth0")));
		r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r3.findFromName("eth0")));
		r3.commitChanges();

		PingStatistics stats4 = r1.ping("192.168.0.130", topology);
		assertEquals(4, stats4.getSent());
		assertEquals(4, stats4.getReceived(), "Should succeed due to correct return route");

		PingStatistics e2estats = h1.ping("20.0.0.2", topology);
		assertEquals(4, e2estats.getSent());
		assertEquals(4, e2estats.getReceived(), "End-to-end ping should succeed with full static routing path");
	}
}
