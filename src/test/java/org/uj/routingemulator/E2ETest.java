package org.uj.routingemulator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
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
		assertThat(out).contains("Error: 1.1.1.1/8 is not a valid IPv4 prefix").contains("Invalid value").contains("Value validation failed").contains("Set failed").contains("[edit]");
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
		assertThat(out).contains("Error: 2.2.2.2/8 is not a valid IPv4 prefix").contains("Invalid value").contains("Value validation failed").contains("Set failed").contains("[edit]");
		CLIContext.clear();
	}

	@Test
	void testTripleRouterSetup() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("H1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(10, 0, 0, 2), new SubnetMask(8)), new IPAddress(10, 0, 0, 1)));
		Host h2 = new Host("H2", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(20, 0, 0, 2), new SubnetMask(8)), new IPAddress(20, 0, 0, 1)));
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

		Host h1 = new Host("H1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(10, 0, 0, 2), new SubnetMask(8)), new IPAddress(10, 0, 0, 1)));
		Host h2 = new Host("H2", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(20, 0, 0, 2), new SubnetMask(8)), new IPAddress(20, 0, 0, 1)));
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

	@Test
	void testTripleRouterRoutingLoop() {
		// Assuming same setup as previous test
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("H1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(10, 0, 0, 2), new SubnetMask(8)), new IPAddress(10, 0, 0, 1)));
		Host h2 = new Host("H2", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(20, 0, 0, 2), new SubnetMask(8)), new IPAddress(20, 0, 0, 1)));
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

		// Configure static route only on R1
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 128), new SubnetMask(26)), r1.findFromName("eth1")));
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r1.findFromName("eth1")));
		r1.commitChanges();

		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth0")));
		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth1")));
		r2.commitChanges();

		// Configure return route on R3
		r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 0), new SubnetMask(25)), r3.findFromName("eth0")));
		r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r3.findFromName("eth0")));
		r3.commitChanges();

		// Attempt ping to a network that does not exist in the topology
		PingStatistics stats = h1.ping("30.0.0.2", topology);
		assertEquals(4, stats.getSent());
		assertEquals(0, stats.getReceived(), "Should not receive a reply from a non-existent destination");

		PingStatistics stats1 = r1.ping("30.0.0.2", topology);
		assertEquals(4, stats1.getSent());
		assertEquals(0, stats1.getReceived(), "Should not receive a reply from a non-existent destination");

		// Create a routing loop
		// R1 next-hop R2
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(30, 0, 0, 0), new SubnetMask(8)), r1.findFromName("eth1")));
		r1.commitChanges();
		// R2 next-hop R3
		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(30, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth1")));
		r2.commitChanges();
		// R2 next-hop R3
		r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(30, 0, 0, 0), new SubnetMask(8)), r3.findFromName("eth0")));
		r3.commitChanges();

		PingStatistics stats2 = h1.ping("30.0.0.2", topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Should not receive a reply due to a routing loop and TTL expiry");

		PingStatistics stats3 = r1.ping("30.0.0.2", topology);
		assertEquals(4, stats3.getSent());
		assertEquals(0, stats3.getReceived(), "Should not receive a reply due to a routing loop and TTL expiry");
	}

	@Test
	void testTriangleTopologyWithMetrics() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("H1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(10, 0, 0, 2), new SubnetMask(8)), new IPAddress(10, 0, 0, 1)));
		Host h2 = new Host("H2", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(20, 0, 0, 2), new SubnetMask(8)), new IPAddress(20, 0, 0, 1)));
		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r3 = new Router("R3", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));

		topology.addHost(h1);
		topology.addHost(h2);
		topology.addRouter(r1);
		topology.addRouter(r2);
		topology.addRouter(r3);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(2), r3.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r2.getInterfaces().get(1), r3.getInterfaces().get(1)));
		topology.addConnection(new Connection(r3.getInterfaces().get(2), h2.getHostInterface()));

		// Configure interfaces
		r1.setMode(RouterMode.CONFIGURATION);
		r1.configureInterface("eth0", InterfaceAddress.fromString("10.0.0.1/8"));
		r1.configureInterface("eth1", InterfaceAddress.fromString("192.168.0.1/25"));
		r1.configureInterface("eth2", InterfaceAddress.fromString("192.168.0.193/26"));
		r1.commitChanges();
		r2.setMode(RouterMode.CONFIGURATION);
		r2.configureInterface("eth0", InterfaceAddress.fromString("192.168.0.2/25"));
		r2.configureInterface("eth1", InterfaceAddress.fromString("192.168.0.129/26"));
		r2.commitChanges();
		r3.setMode(RouterMode.CONFIGURATION);
		r3.configureInterface("eth0", InterfaceAddress.fromString("192.168.0.194/26"));
		r3.configureInterface("eth1", InterfaceAddress.fromString("192.168.0.130/26"));
		r3.configureInterface("eth2", InterfaceAddress.fromString("20.0.0.1/8"));
		r3.commitChanges();

		// Configure static routes
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 128), new SubnetMask(26)), r1.findFromName("eth1")));
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 128), new SubnetMask(26)), r1.findFromName("eth2"))); // Should allow both routes since outbound interface is different

		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r1.findFromName("eth2"), 1)); // Lower metric, should be preferred
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r1.findFromName("eth1"), 2)); // Route via 2 routers, higher metric
		r1.commitChanges();

		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth0")));
		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth1"), 2));
		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth1")));
		// To 192.168.0.192/26 there are two equidistant routes via eth0 and eth1
		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 192), new SubnetMask(26)), r2.findFromName("eth0")));
		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 192), new SubnetMask(26)), r2.findFromName("eth1")));
		r2.commitChanges();

		r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 0), new SubnetMask(25)), r3.findFromName("eth0")));
		r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 0), new SubnetMask(25)), r3.findFromName("eth1")));
		r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r3.findFromName("eth0"))); // Route via 1 router, should be preferred
		r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r3.findFromName("eth1"), 2)); // Route via 2 routers, higher metric
		r3.commitChanges();

		// From X to XX-XXX
		PingStatistics stats = h1.ping("192.168.0.129", topology);
		assertEquals(4, stats.getSent());
		assertEquals(4, stats.getReceived(), "Should receive a reply from R2");

		PingStatistics stats1 = h1.ping("192.168.0.130", topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should receive a reply from R2");

		// From XX to X
		PingStatistics stats2 = r2.ping("10.0.0.1", topology);
		assertEquals(4, stats2.getSent());
		assertEquals(4, stats2.getReceived(), "Should receive a reply from H1");

		// Should prefer route with lower metric
		int initialHops = stats2.results().getFirst().hopCount();

		r2.disableRoute(new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth0")));
		r2.commitChanges();

		PingStatistics stats3 = r2.ping("10.0.0.1", topology);
		assertEquals(4, stats3.getSent());
		assertEquals(4, stats3.getReceived(), "Should receive a reply from H1");

		// Should now use secondary route
		for (PingResult pingResult : stats3.results()) {
			assertEquals(initialHops + 1, pingResult.hopCount());
		}
	}

	// Helper methods for tests: compute Nth host address (1-based) from a CIDR network string (a.b.c.d/m)
	private static String nthHostFromNetworkOrDefault(String cidr, int hostIndex, String defaultAddr) {
		if (cidr == null || cidr.isBlank()) return defaultAddr;
		try {
			String[] parts = cidr.split("/");
			IPAddress net = IPAddress.fromString(parts[0]);
			int prefix = Integer.parseInt(parts[1]);
			long netAsLong = ((long) net.getOctet1() << 24) | ((long) net.getOctet2() << 16) | ((long) net.getOctet3() << 8) | (net.getOctet4() & 0xffL);
			long host = netAsLong + hostIndex; // hostIndex 1 -> first host
			long mask = (prefix == 0) ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
			long broadcast = (netAsLong & mask) | (~mask & 0xFFFFFFFFL);
			if (host >= broadcast) {
				// fallback to default if overflow
				return defaultAddr;
			}
			int o1 = (int) ((host >> 24) & 0xFF);
			int o2 = (int) ((host >> 16) & 0xFF);
			int o3 = (int) ((host >> 8) & 0xFF);
			int o4 = (int) (host & 0xFF);
			return String.format("%d.%d.%d.%d/%d", o1, o2, o3, o4, prefix);
		} catch (Exception e) {
			return defaultAddr;
		}
	}

	private static String firstHostFromNetworkOrDefault(String cidr, String defaultAddr) {
		return nthHostFromNetworkOrDefault(cidr, 1, defaultAddr);
	}

	private static String trimAddressMask(String addrWithMask) {
		if (addrWithMask == null) return null;
		int idx = addrWithMask.indexOf('/');
		return idx == -1 ? addrWithMask : addrWithMask.substring(0, idx);
	}

	// Helper to extract Subnet from a CIDR or a default
	private static Subnet subnetFromCidrOrDefault(String cidr, String defaultCidr) {
		String use = (cidr == null || cidr.isBlank()) ? defaultCidr : cidr;
		try {
			String[] parts = use.split("/");
			IPAddress ip = IPAddress.fromString(parts[0]);
			int p = Integer.parseInt(parts[1]);
			SubnetMask sm = new SubnetMask(p);
			return new Subnet(ip, sm);
		} catch (Exception e) {
			return subnetFromCidrOrDefault(defaultCidr, defaultCidr);
		}
	}

	// Parse network prefix and return Subnet-like helper via IPAddress.fromStringPrefixOrDefault (added below) - not available, so implement small helper
	// We will add a small helper to create IPAddress from CIDR and expose network address and prefix
	// For brevity inside tests we use IPAddress.fromString and SubnetMask where needed; create helper below

	@ParameterizedTest
	@CsvFileSource(resources = "/network_configuration.csv", numLinesToSkip = 1)
	void testSixRouterSetup(String id, String vlanid, String rb_nodex, String x_xx, String nodeXX_rc, String x, String xx) {
		NetworkTopology topology = new NetworkTopology();

		Router ra = new Router("RA", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router rb = new Router("RB", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router rc = new Router("RC", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router rd = new Router("RD", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router rx = new Router("RX", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router rxx = new Router("RXX", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));

		topology.addRouter(ra);
		topology.addRouter(rb);
		topology.addRouter(rc);
		topology.addRouter(rd);
		topology.addRouter(rx);
		topology.addRouter(rxx);

		topology.addConnection(new Connection(ra.getInterfaces().get(1), rb.getInterfaces().get(1)));
		topology.addConnection(new Connection(rb.getInterfaces().getFirst(), rx.getInterfaces().getFirst()));
		topology.addConnection(new Connection(rx.getInterfaces().get(1), rxx.getInterfaces().get(1)));
		topology.addConnection(new Connection(rxx.getInterfaces().get(2), rc.getInterfaces().get(2)));
		topology.addConnection(new Connection(rc.getInterfaces().getFirst(), rd.getInterfaces().get(1)));

		// Helper to resolve network-derived addresses; fall back to defaults when CSV is empty
		String rxEth1 = firstHostFromNetworkOrDefault(x_xx, "192.168.10.1/24");
		String rxxEth1 = nthHostFromNetworkOrDefault(x_xx, 2, "192.168.10.2/24");

		String rbEth0 = firstHostFromNetworkOrDefault(rb_nodex, "1.1.10.25/29");
		String rxEth0 = nthHostFromNetworkOrDefault(rb_nodex, 2, "1.1.10.26/29");

		String rxxEth2 = firstHostFromNetworkOrDefault(nodeXX_rc, "2.2.10.89/29");
		String rcEth2 = nthHostFromNetworkOrDefault(nodeXX_rc, 2, "2.2.10.90/29");

		// Configure interfaces eth1 on RX and RXX
		rx.setMode(RouterMode.CONFIGURATION);
		rx.configureInterface("eth1", InterfaceAddress.fromString(rxEth1));
		rx.commitChanges();

		rxx.setMode(RouterMode.CONFIGURATION);
		rxx.configureInterface("eth1", InterfaceAddress.fromString(rxxEth1));
		rxx.commitChanges();

		// Try ping RX <-> RXX
		PingStatistics stats = rx.ping(trimAddressMask(rxxEth1), topology);
		assertEquals(4, stats.getSent());
		assertEquals(4, stats.getReceived(), "Should receive a reply between directly connected interfaces");

		PingStatistics stats1 = rxx.ping(trimAddressMask(rxEth1), topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should receive a reply between directly connected interfaces");

		// Configure interfaces eth0 (RB-RX) and eth2 (RXX-RC)
		rb.setMode(RouterMode.CONFIGURATION);
		rb.configureInterface("eth0", InterfaceAddress.fromString(rbEth0));
		rb.commitChanges();

		rx.setMode(RouterMode.CONFIGURATION);
		rx.configureInterface("eth0", InterfaceAddress.fromString(rxEth0));
		rx.commitChanges();

		rxx.setMode(RouterMode.CONFIGURATION);
		rxx.configureInterface("eth2", InterfaceAddress.fromString(rxxEth2));
		rxx.commitChanges();

		rc.setMode(RouterMode.CONFIGURATION);
		rc.configureInterface("eth2", InterfaceAddress.fromString(rcEth2));
		rc.commitChanges();

		// Ping RX -> RB
		PingStatistics stats2 = rx.ping(trimAddressMask(rbEth0), topology);
		assertEquals(4, stats2.getSent());
		assertEquals(4, stats2.getReceived(), "Should receive a reply from RB");

		// Ping RXX -> RC
		PingStatistics stats3 = rxx.ping(trimAddressMask(rcEth2), topology);
		assertEquals(4, stats3.getSent());
		assertEquals(4, stats3.getReceived(), "Should receive a reply from RC");

		// Configure remaining interfaces
		Host h1 = new Host("H1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 2, 2), new SubnetMask(8)), new IPAddress(192, 168, 2, 1)));
		Host h2 = new Host("H1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 4, 2), new SubnetMask(8)), new IPAddress(192, 168, 4, 1)));

		topology.addHost(h1);
		topology.addHost(h2);

		topology.addConnection(new Connection(h1.getHostInterface(), ra.getInterfaces().getFirst()));
		topology.addConnection(new Connection(rd.getInterfaces().getFirst(), h2.getHostInterface()));

		ra.setMode(RouterMode.CONFIGURATION);
		ra.configureInterface("eth0", InterfaceAddress.fromString("192.168.2.1/24"));
		ra.configureInterface("eth1", InterfaceAddress.fromString("192.168.1.1/30"));
		ra.commitChanges();

		rd.setMode(RouterMode.CONFIGURATION);
		rd.configureInterface("eth0", InterfaceAddress.fromString("192.168.4.1/24"));
		rd.configureInterface("eth1", InterfaceAddress.fromString("192.168.3.1/30"));
		rd.commitChanges();

		rb.configureInterface("eth1", InterfaceAddress.fromString("192.168.1.2/30"));
		rb.commitChanges();

		rc.configureInterface("eth0", InterfaceAddress.fromString("192.168.3.2/30"));
		rc.commitChanges();

		// Add static routes using next-hop addresses
		// RX Routes
		rx.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rbEth0)))); // To RA
		rx.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(30)), IPAddress.fromString(trimAddressMask(rbEth0)))); // To ra-rb
		Subnet nodeXXSubnet = subnetFromCidrOrDefault(nodeXX_rc, "2.2.10.88/29");
		rx.addRoute(new StaticRoutingEntry(new Subnet(nodeXXSubnet.networkAddress(), new SubnetMask(nodeXXSubnet.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxxEth1)))); // To nodeXX-rc
		rx.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 3, 0), new SubnetMask(30)), IPAddress.fromString(trimAddressMask(rxxEth1)))); // To rc-rd
		rx.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 4, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rxxEth1)))); // To RD
		rx.commitChanges();

		// RXX Routes
		rxx.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 4, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rcEth2)))); // To RD
		rxx.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 3, 0), new SubnetMask(30)), IPAddress.fromString(trimAddressMask(rcEth2)))); // To rc-rd
		Subnet rbNodeXSubnet = subnetFromCidrOrDefault(rb_nodex, "1.1.10.24/29");
		rxx.addRoute(new StaticRoutingEntry(new Subnet(rbNodeXSubnet.networkAddress(), new SubnetMask(rbNodeXSubnet.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxEth1)))); // To rb-nodeX
		rxx.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(30)), IPAddress.fromString(trimAddressMask(rxEth1)))); // To ra-rb
		rxx.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rxEth1)))); // To RA
		rxx.commitChanges();

		// External routers (RA, RB, RC, RD) need return routes to the internal "core" networks
		Subnet xXxSubnet3 = subnetFromCidrOrDefault(x_xx, "192.168.10.0/24");
		rb.addRoute(new StaticRoutingEntry(new Subnet(xXxSubnet3.networkAddress(), new SubnetMask(xXxSubnet3.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxEth0))));
		Subnet nodeXXSubnet3 = subnetFromCidrOrDefault(nodeXX_rc, "2.2.10.88/29");
		rb.addRoute(new StaticRoutingEntry(new Subnet(nodeXXSubnet3.networkAddress(), new SubnetMask(nodeXXSubnet3.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxEth0))));
		rb.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 4, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rxEth0))));
		rb.commitChanges();

		Subnet xXxSubnet4 = subnetFromCidrOrDefault(x_xx, "192.168.10.0/24");
		rc.addRoute(new StaticRoutingEntry(new Subnet(xXxSubnet4.networkAddress(), new SubnetMask(xXxSubnet4.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxxEth2))));
		Subnet rbNodeXSubnet4 = subnetFromCidrOrDefault(rb_nodex, "1.1.10.24/29");
		rc.addRoute(new StaticRoutingEntry(new Subnet(rbNodeXSubnet4.networkAddress(), new SubnetMask(rbNodeXSubnet4.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxxEth2))));
		rc.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rxxEth2))));
		rc.commitChanges();

		// End-of-Chain Routers (RA & RD)
		ra.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(0, 0, 0, 0), new SubnetMask(0)), IPAddress.fromString("192.168.1.2"))); // Default via RB
		ra.commitChanges();
		rd.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(0, 0, 0, 0), new SubnetMask(0)), IPAddress.fromString("192.168.3.2"))); // Default via RC
		rd.commitChanges();
	}
}
