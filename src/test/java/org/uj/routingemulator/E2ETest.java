package org.uj.routingemulator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.uj.routingemulator.common.addressing.IPAddress;
import org.uj.routingemulator.common.addressing.InterfaceAddress;
import org.uj.routingemulator.common.addressing.Subnet;
import org.uj.routingemulator.common.addressing.SubnetMask;
import org.uj.routingemulator.common.ping.PingResult;
import org.uj.routingemulator.common.ping.PingService;
import org.uj.routingemulator.common.ping.PingStatistics;
import org.uj.routingemulator.common.topology.Connection;
import org.uj.routingemulator.common.topology.NetworkTopology;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.cli.*;
import org.uj.routingemulator.router.model.*;
import org.uj.routingemulator.router.session.RouterConfigurationService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class E2ETest {
	private final RouterConfigurationService routerConfigurationService = new RouterConfigurationService();

	private static String nthHostFromNetworkOrDefault(String cidr, int hostIndex, String defaultAddr) {
		if (cidr == null || cidr.isBlank()) return defaultAddr;
		try {
			String[] parts = cidr.split("/");
			IPAddress net = IPAddress.fromString(parts[0]);
			int prefix = Integer.parseInt(parts[1]);

			long netAsLong = ((long) net.octet1() << 24)
					| ((long) net.octet2() << 16)
					| ((long) net.octet3() << 8)
					| (net.octet4() & 0xffL);

			long host = netAsLong + hostIndex;

			long mask = (prefix == 0) ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
			long broadcast = (netAsLong & mask) | (~mask & 0xFFFFFFFFL);

			if (host >= broadcast) {
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

	@Test
	void testNextHopSubnetNotANetworkAddress() {
		Router router = new Router("R1");
		RouterModeController.setMode(router, RouterMode.CONFIGURATION);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		CommandExecutionContext context = new CommandExecutionContext(router, new NetworkTopology(), new PrintWriterCommandOutput(pw));
		CliSession session = new CliSession(new DefaultCommandExecutor(new RouterCLIParser(CommandRegistry.defaultRegistry())), context);

		session.execute("set protocols static route 1.1.1.1/8 next-hop 2.2.2.2");

		String out = sw.toString();
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(1, 1, 1, 1), new SubnetMask(8)), new IPAddress(2, 2, 2, 2));

		assertThat(out).contains("Error: 1.1.1.1/8 is not a valid IPv4 prefix")
				.contains("Invalid value")
				.contains("Value validation failed")
				.contains("Set failed")
				.contains("[edit]");
		assertFalse(router.getRoutingTable().contains(entry));
	}

	@Test
	void testNextHopAddressContainsMask() {
		Router router = new Router("R1");
		RouterModeController.setMode(router, RouterMode.CONFIGURATION);

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		CommandExecutionContext context = new CommandExecutionContext(router, new NetworkTopology(), new PrintWriterCommandOutput(pw));
		CliSession session = new CliSession(new DefaultCommandExecutor(new RouterCLIParser(CommandRegistry.defaultRegistry())), context);

		session.execute("set protocols static route 1.1.1.0/8 next-hop 2.2.2.2/8");

		String out = sw.toString();
		assertThat(out).contains("Error: 2.2.2.2/8 is not a valid IPv4 prefix")
				.contains("Invalid value")
				.contains("Value validation failed")
				.contains("Set failed")
				.contains("[edit]");
	}

	@Test
	void testTripleRouterSetup() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("H1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(10, 0, 0, 2), new SubnetMask(8)), new IPAddress(10, 0, 0, 1)));
		Host h2 = new Host("H2", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(20, 0, 0, 2), new SubnetMask(8)), new IPAddress(20, 0, 0, 1)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r3 = new Router("R3", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

		topology.addDevice(h1);
		topology.addDevice(h2);
		topology.addDevice(r1);
		topology.addDevice(r2);
		topology.addDevice(r3);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r2.getInterfaces().get(1), r3.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r3.getInterfaces().get(1), h2.getHostInterface()));

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r1, "eth0", InterfaceAddress.fromString("10.0.0.1/8"));
		routerConfigurationService.configureInterface(r1, "eth1", InterfaceAddress.fromString("192.168.0.1/25"));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r2, "eth0", InterfaceAddress.fromString("192.168.0.2/25"));
		routerConfigurationService.configureInterface(r2, "eth1", InterfaceAddress.fromString("192.168.0.129/26"));
		r2.getConfigSession().commit();

		RouterModeController.setMode(r3, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r3, "eth0", InterfaceAddress.fromString("192.168.0.130/26"));
		routerConfigurationService.configureInterface(r3, "eth1", InterfaceAddress.fromString("20.0.0.1/8"));
		r3.getConfigSession().commit();

		PingStatistics stats = new PingService().ping(r1, IPAddress.fromString("192.168.0.2"), 4, 64, topology);
		assertEquals(4, stats.getSent());
		assertEquals(4, stats.getReceived(), "Should receive a reply from a directly connected router");

		PingStatistics stats1 = new PingService().ping(r2, IPAddress.fromString("192.168.0.130"), 4, 64, topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should receive a reply from a directly connected router");

		PingStatistics stats2 = new PingService().ping(r1, IPAddress.fromString("192.168.0.130"), 4, 64, topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Should not receive a reply from an indirectly connected router");

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 128), new SubnetMask(26)), r1.findFromName("eth1")));
		r1.getConfigSession().commit();

		PingStatistics stats3 = new PingService().ping(r1, IPAddress.fromString("192.168.0.130"), 4, 64, topology);
		assertEquals(4, stats3.getSent());
		assertEquals(0, stats3.getReceived(), "Should not receive a reply due to packet drop at R3 (no route back to R1)");

		RouterModeController.setMode(r3, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r3, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 0), new SubnetMask(25)), r3.findFromName("eth0")));
		r3.getConfigSession().commit();

		PingStatistics stats4 = new PingService().ping(r1, IPAddress.fromString("192.168.0.130"), 4, 64, topology);
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

		topology.addDevice(h1);
		topology.addDevice(h2);
		topology.addDevice(r1);
		topology.addDevice(r2);
		topology.addDevice(r3);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r2.getInterfaces().get(1), r3.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r3.getInterfaces().get(1), h2.getHostInterface()));

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r1, "eth0", InterfaceAddress.fromString("10.0.0.1/8"));
		routerConfigurationService.configureInterface(r1, "eth1", InterfaceAddress.fromString("192.168.0.1/25"));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r2, "eth0", InterfaceAddress.fromString("192.168.0.2/25"));
		routerConfigurationService.configureInterface(r2, "eth1", InterfaceAddress.fromString("192.168.0.129/26"));
		r2.getConfigSession().commit();

		RouterModeController.setMode(r3, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r3, "eth0", InterfaceAddress.fromString("192.168.0.130/26"));
		routerConfigurationService.configureInterface(r3, "eth1", InterfaceAddress.fromString("20.0.0.1/8"));
		r3.getConfigSession().commit();

		PingStatistics stats = new PingService().ping(r1, IPAddress.fromString("192.168.0.2"), 4, 64, topology);
		assertEquals(4, stats.getSent());
		assertEquals(4, stats.getReceived(), "Should receive a reply from a directly connected router");

		PingStatistics stats1 = new PingService().ping(r2, IPAddress.fromString("192.168.0.130"), 4, 64, topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should receive a reply from a directly connected router");

		PingStatistics stats2 = new PingService().ping(r1, IPAddress.fromString("192.168.0.130"), 4, 64, topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Should not receive a reply from an indirectly connected router");

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 128), new SubnetMask(26)), r1.findFromName("eth1")));
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r1.findFromName("eth1")));
		r1.getConfigSession().commit();

		PingStatistics stats3 = new PingService().ping(r1, IPAddress.fromString("192.168.0.130"), 4, 64, topology);
		assertEquals(4, stats3.getSent());
		assertEquals(0, stats3.getReceived(), "Should not receive a reply due to packet drop at R3 (no route back to R1)");

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth0")));
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth1")));
		r2.getConfigSession().commit();

		RouterModeController.setMode(r3, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r3, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 0), new SubnetMask(25)), r3.findFromName("eth0")));
		routerConfigurationService.addRoute(r3, new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r3.findFromName("eth0")));
		r3.getConfigSession().commit();

		PingStatistics stats4 = new PingService().ping(r1, IPAddress.fromString("192.168.0.130"), 4, 64, topology);
		assertEquals(4, stats4.getSent());
		assertEquals(4, stats4.getReceived(), "Should succeed due to correct return route");

		PingStatistics e2estats = new PingService().ping(h1, "20.0.0.2", 4, topology);
		assertEquals(4, e2estats.getSent());
		assertEquals(4, e2estats.getReceived(), "End-to-end ping should succeed with full static routing path");
	}

	@Test
	void testTripleRouterRoutingLoop() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("H1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(10, 0, 0, 2), new SubnetMask(8)), new IPAddress(10, 0, 0, 1)));
		Host h2 = new Host("H2", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(20, 0, 0, 2), new SubnetMask(8)), new IPAddress(20, 0, 0, 1)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r3 = new Router("R3", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

		topology.addDevice(h1);
		topology.addDevice(h2);
		topology.addDevice(r1);
		topology.addDevice(r2);
		topology.addDevice(r3);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r2.getInterfaces().get(1), r3.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r3.getInterfaces().get(1), h2.getHostInterface()));

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r1, "eth0", InterfaceAddress.fromString("10.0.0.1/8"));
		routerConfigurationService.configureInterface(r1, "eth1", InterfaceAddress.fromString("192.168.0.1/25"));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r2, "eth0", InterfaceAddress.fromString("192.168.0.2/25"));
		routerConfigurationService.configureInterface(r2, "eth1", InterfaceAddress.fromString("192.168.0.129/26"));
		r2.getConfigSession().commit();

		RouterModeController.setMode(r3, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r3, "eth0", InterfaceAddress.fromString("192.168.0.130/26"));
		routerConfigurationService.configureInterface(r3, "eth1", InterfaceAddress.fromString("20.0.0.1/8"));
		r3.getConfigSession().commit();

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 128), new SubnetMask(26)), r1.findFromName("eth1")));
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r1.findFromName("eth1")));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth0")));
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth1")));
		r2.getConfigSession().commit();

		RouterModeController.setMode(r3, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r3, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 0), new SubnetMask(25)), r3.findFromName("eth0")));
		routerConfigurationService.addRoute(r3, new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r3.findFromName("eth0")));
		r3.getConfigSession().commit();

		PingStatistics stats = new PingService().ping(h1, "30.0.0.2", 4, topology);
		assertEquals(4, stats.getSent());
		assertEquals(0, stats.getReceived(), "Should not receive a reply from a non-existent destination");

		PingStatistics stats1 = new PingService().ping(r1, IPAddress.fromString("30.0.0.2"), 4, 64, topology);
		assertEquals(4, stats1.getSent());
		assertEquals(0, stats1.getReceived(), "Should not receive a reply from a non-existent destination");

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(30, 0, 0, 0), new SubnetMask(8)), r1.findFromName("eth1")));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(30, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth1")));
		r2.getConfigSession().commit();

		RouterModeController.setMode(r3, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r3, new StaticRoutingEntry(new Subnet(new IPAddress(30, 0, 0, 0), new SubnetMask(8)), r3.findFromName("eth0")));
		r3.getConfigSession().commit();

		PingStatistics stats2 = new PingService().ping(h1, "30.0.0.2", 4, topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Should not receive a reply due to a routing loop and TTL expiry");

		PingStatistics stats3 = new PingService().ping(r1, IPAddress.fromString("30.0.0.2"), 4, 64, topology);
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

		topology.addDevice(h1);
		topology.addDevice(h2);
		topology.addDevice(r1);
		topology.addDevice(r2);
		topology.addDevice(r3);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(2), r3.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r2.getInterfaces().get(1), r3.getInterfaces().get(1)));
		topology.addConnection(new Connection(r3.getInterfaces().get(2), h2.getHostInterface()));

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r1, "eth0", InterfaceAddress.fromString("10.0.0.1/8"));
		routerConfigurationService.configureInterface(r1, "eth1", InterfaceAddress.fromString("192.168.0.1/25"));
		routerConfigurationService.configureInterface(r1, "eth2", InterfaceAddress.fromString("192.168.0.193/26"));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r2, "eth0", InterfaceAddress.fromString("192.168.0.2/25"));
		routerConfigurationService.configureInterface(r2, "eth1", InterfaceAddress.fromString("192.168.0.129/26"));
		r2.getConfigSession().commit();

		RouterModeController.setMode(r3, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r3, "eth0", InterfaceAddress.fromString("192.168.0.194/26"));
		routerConfigurationService.configureInterface(r3, "eth1", InterfaceAddress.fromString("192.168.0.130/26"));
		routerConfigurationService.configureInterface(r3, "eth2", InterfaceAddress.fromString("20.0.0.1/8"));
		r3.getConfigSession().commit();

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 128), new SubnetMask(26)), r1.findFromName("eth1")));
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 128), new SubnetMask(26)), r1.findFromName("eth2")));
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r1.findFromName("eth2"), 1));
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r1.findFromName("eth1"), 2));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth0")));
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth1"), 2));
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(20, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth1")));
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 192), new SubnetMask(26)), r2.findFromName("eth0")));
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 192), new SubnetMask(26)), r2.findFromName("eth1")));
		r2.getConfigSession().commit();

		RouterModeController.setMode(r3, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r3, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 0), new SubnetMask(25)), r3.findFromName("eth0")));
		routerConfigurationService.addRoute(r3, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 0, 0), new SubnetMask(25)), r3.findFromName("eth1")));
		routerConfigurationService.addRoute(r3, new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r3.findFromName("eth0")));
		routerConfigurationService.addRoute(r3, new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r3.findFromName("eth1"), 2));
		r3.getConfigSession().commit();

		PingStatistics stats = new PingService().ping(h1, "192.168.0.129", 4, topology);
		assertEquals(4, stats.getSent());
		assertEquals(4, stats.getReceived(), "Should receive a reply from R2");

		PingStatistics stats1 = new PingService().ping(h1, "192.168.0.130", 4, topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should receive a reply from R2");

		PingStatistics stats2 = new PingService().ping(r2, IPAddress.fromString("10.0.0.1"), 4, 64, topology);
		assertEquals(4, stats2.getSent());
		assertEquals(4, stats2.getReceived(), "Should receive a reply from H1");

		int initialHops = stats2.results().getFirst().hopCount();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.disableRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), r2.findFromName("eth0")));
		r2.getConfigSession().commit();

		PingStatistics stats3 = new PingService().ping(r2, IPAddress.fromString("10.0.0.1"), 4, 64, topology);
		assertEquals(4, stats3.getSent());
		assertEquals(4, stats3.getReceived(), "Should receive a reply from H1");

		for (PingResult pingResult : stats3.results()) {
			assertEquals(initialHops + 1, pingResult.hopCount());
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

		topology.addDevice(ra);
		topology.addDevice(rb);
		topology.addDevice(rc);
		topology.addDevice(rd);
		topology.addDevice(rx);
		topology.addDevice(rxx);

		topology.addConnection(new Connection(ra.getInterfaces().get(1), rb.getInterfaces().get(1)));
		topology.addConnection(new Connection(rb.getInterfaces().getFirst(), rx.getInterfaces().getFirst()));
		topology.addConnection(new Connection(rx.getInterfaces().get(1), rxx.getInterfaces().get(1)));
		topology.addConnection(new Connection(rxx.getInterfaces().get(2), rc.getInterfaces().get(2)));
		topology.addConnection(new Connection(rc.getInterfaces().getFirst(), rd.getInterfaces().get(1)));

		String rxEth1 = firstHostFromNetworkOrDefault(x_xx, "192.168.10.1/24");
		String rxxEth1 = nthHostFromNetworkOrDefault(x_xx, 2, "192.168.10.2/24");
		String rbEth0 = firstHostFromNetworkOrDefault(rb_nodex, "1.1.10.25/29");
		String rxEth0 = nthHostFromNetworkOrDefault(rb_nodex, 2, "1.1.10.26/29");
		String rxxEth2 = firstHostFromNetworkOrDefault(nodeXX_rc, "2.2.10.89/29");
		String rcEth2 = nthHostFromNetworkOrDefault(nodeXX_rc, 2, "2.2.10.90/29");

		RouterModeController.setMode(rx, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(rx, "eth1", InterfaceAddress.fromString(rxEth1));
		rx.getConfigSession().commit();

		RouterModeController.setMode(rxx, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(rxx, "eth1", InterfaceAddress.fromString(rxxEth1));
		rxx.getConfigSession().commit();

		PingStatistics stats = new PingService().ping(rx, IPAddress.fromString(trimAddressMask(rxxEth1)), 4, 64, topology);
		assertEquals(4, stats.getSent());
		assertEquals(4, stats.getReceived(), "Should receive a reply between directly connected interfaces");

		PingStatistics stats1 = new PingService().ping(rxx, IPAddress.fromString(trimAddressMask(rxEth1)), 4, 64, topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should receive a reply between directly connected interfaces");

		RouterModeController.setMode(rb, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(rb, "eth0", InterfaceAddress.fromString(rbEth0));
		rb.getConfigSession().commit();

		RouterModeController.setMode(rx, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(rx, "eth0", InterfaceAddress.fromString(rxEth0));
		rx.getConfigSession().commit();

		RouterModeController.setMode(rxx, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(rxx, "eth2", InterfaceAddress.fromString(rxxEth2));
		rxx.getConfigSession().commit();

		RouterModeController.setMode(rc, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(rc, "eth2", InterfaceAddress.fromString(rcEth2));
		rc.getConfigSession().commit();

		PingStatistics stats2 = new PingService().ping(rx, IPAddress.fromString(trimAddressMask(rbEth0)), 4, 64, topology);
		assertEquals(4, stats2.getSent());
		assertEquals(4, stats2.getReceived(), "Should receive a reply from RB");

		PingStatistics stats3 = new PingService().ping(rxx, IPAddress.fromString(trimAddressMask(rcEth2)), 4, 64, topology);
		assertEquals(4, stats3.getSent());
		assertEquals(4, stats3.getReceived(), "Should receive a reply from RC");

		Host h1 = new Host("H1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 2, 2), new SubnetMask(8)), new IPAddress(192, 168, 2, 1)));
		Host h2 = new Host("H1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 4, 2), new SubnetMask(8)), new IPAddress(192, 168, 4, 1)));

		topology.addDevice(h1);
		topology.addDevice(h2);

		topology.addConnection(new Connection(h1.getHostInterface(), ra.getInterfaces().getFirst()));
		topology.addConnection(new Connection(rd.getInterfaces().getFirst(), h2.getHostInterface()));

		RouterModeController.setMode(ra, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(ra, "eth0", InterfaceAddress.fromString("192.168.2.1/24"));
		routerConfigurationService.configureInterface(ra, "eth1", InterfaceAddress.fromString("192.168.1.1/30"));
		ra.getConfigSession().commit();

		RouterModeController.setMode(rd, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(rd, "eth0", InterfaceAddress.fromString("192.168.4.1/24"));
		routerConfigurationService.configureInterface(rd, "eth1", InterfaceAddress.fromString("192.168.3.1/30"));
		rd.getConfigSession().commit();

		RouterModeController.setMode(rb, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(rb, "eth1", InterfaceAddress.fromString("192.168.1.2/30"));
		rb.getConfigSession().commit();

		RouterModeController.setMode(rc, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(rc, "eth0", InterfaceAddress.fromString("192.168.3.2/30"));
		rc.getConfigSession().commit();

		RouterModeController.setMode(rx, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(rx, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rbEth0))));
		routerConfigurationService.addRoute(rx, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(30)), IPAddress.fromString(trimAddressMask(rbEth0))));

		Subnet nodeXXSubnet = subnetFromCidrOrDefault(nodeXX_rc, "2.2.10.88/29");
		routerConfigurationService.addRoute(rx, new StaticRoutingEntry(new Subnet(nodeXXSubnet.networkAddress(), new SubnetMask(nodeXXSubnet.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxxEth1))));
		routerConfigurationService.addRoute(rx, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 3, 0), new SubnetMask(30)), IPAddress.fromString(trimAddressMask(rxxEth1))));
		routerConfigurationService.addRoute(rx, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 4, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rxxEth1))));
		rx.getConfigSession().commit();

		RouterModeController.setMode(rxx, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(rxx, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 4, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rcEth2))));
		routerConfigurationService.addRoute(rxx, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 3, 0), new SubnetMask(30)), IPAddress.fromString(trimAddressMask(rcEth2))));

		Subnet rbNodeXSubnet = subnetFromCidrOrDefault(rb_nodex, "1.1.10.24/29");
		routerConfigurationService.addRoute(rxx, new StaticRoutingEntry(new Subnet(rbNodeXSubnet.networkAddress(), new SubnetMask(rbNodeXSubnet.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxEth1))));
		routerConfigurationService.addRoute(rxx, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(30)), IPAddress.fromString(trimAddressMask(rxEth1))));
		routerConfigurationService.addRoute(rxx, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rxEth1))));
		rxx.getConfigSession().commit();

		Subnet xXxSubnet3 = subnetFromCidrOrDefault(x_xx, "192.168.10.0/24");
		RouterModeController.setMode(rb, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(rb, new StaticRoutingEntry(new Subnet(xXxSubnet3.networkAddress(), new SubnetMask(xXxSubnet3.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxEth0))));

		Subnet nodeXXSubnet3 = subnetFromCidrOrDefault(nodeXX_rc, "2.2.10.88/29");
		routerConfigurationService.addRoute(rb, new StaticRoutingEntry(new Subnet(nodeXXSubnet3.networkAddress(), new SubnetMask(nodeXXSubnet3.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxEth0))));
		routerConfigurationService.addRoute(rb, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 4, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rxEth0))));
		rb.getConfigSession().commit();

		Subnet xXxSubnet4 = subnetFromCidrOrDefault(x_xx, "192.168.10.0/24");
		RouterModeController.setMode(rc, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(rc, new StaticRoutingEntry(new Subnet(xXxSubnet4.networkAddress(), new SubnetMask(xXxSubnet4.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxxEth2))));

		Subnet rbNodeXSubnet4 = subnetFromCidrOrDefault(rb_nodex, "1.1.10.24/29");
		routerConfigurationService.addRoute(rc, new StaticRoutingEntry(new Subnet(rbNodeXSubnet4.networkAddress(), new SubnetMask(rbNodeXSubnet4.subnetMask().shortMask())), IPAddress.fromString(trimAddressMask(rxxEth2))));
		routerConfigurationService.addRoute(rc, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), IPAddress.fromString(trimAddressMask(rxxEth2))));
		rc.getConfigSession().commit();

		RouterModeController.setMode(ra, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(ra, new StaticRoutingEntry(new Subnet(new IPAddress(0, 0, 0, 0), new SubnetMask(0)), IPAddress.fromString("192.168.1.2")));
		ra.getConfigSession().commit();

		RouterModeController.setMode(rd, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(rd, new StaticRoutingEntry(new Subnet(new IPAddress(0, 0, 0, 0), new SubnetMask(0)), IPAddress.fromString("192.168.3.2")));
		rd.getConfigSession().commit();
	}
}