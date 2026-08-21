package org.uj.routingemulator;

import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogicalErrorTest {
	private final RouterConfigurationService routerConfigurationService = new RouterConfigurationService();

	@Test
	void testNoReturnRoute() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
		Host h2 = new Host("PC2", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 2, 1), new SubnetMask(24)), new IPAddress(192, 168, 2, 254)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

		topology.addDevice(h1);
		topology.addDevice(h2);
		topology.addDevice(r1);
		topology.addDevice(r2);

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r1, "eth0", InterfaceAddress.fromString("192.168.1.254/24"));
		routerConfigurationService.configureInterface(r1, "eth1", InterfaceAddress.fromString("192.168.3.1/24"));
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), r1.findFromName("eth1")));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r2, "eth0", InterfaceAddress.fromString("192.168.3.2/24"));
		routerConfigurationService.configureInterface(r2, "eth1", InterfaceAddress.fromString("192.168.2.254/24"));
		r2.getConfigSession().commit();

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r2.getInterfaces().get(1), h2.getHostInterface()));

		PingStatistics stats = new PingService().ping(h1, "192.168.1.254", 4, topology);
		assertEquals(4, stats.getSent());
		assertEquals(4, stats.getReceived(), "Should be able to ping default gateway");

		PingStatistics stats1 = new PingService().ping(h1, "192.168.3.1", 4, topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should be able to ping connected router interface");

		PingStatistics stats2 = new PingService().ping(h1, "192.168.3.2", 4, topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Should not be able to ping indirectly connected router interface");

		PingStatistics stats3 = new PingService().ping(h1, "192.168.2.254", 4, topology);
		assertEquals(4, stats3.getSent());
		assertEquals(0, stats3.getReceived(), "Should not be able to ping indirectly connected router interface");

		PingStatistics stats4 = new PingService().ping(h1, "192.168.2.1", 4, topology);
		assertEquals(4, stats4.getSent());
		assertEquals(0, stats4.getReceived(), "Should not receive a reply without return route");
	}

	@Test
	void testRoutingAsymmetry() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
		Host h2 = new Host("PC2", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 2, 1), new SubnetMask(24)), new IPAddress(192, 168, 2, 254)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r3 = new Router("R3", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r4 = new Router("R4", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));

		topology.addDevice(h1);
		topology.addDevice(h2);
		topology.addDevice(r1);
		topology.addDevice(r2);
		topology.addDevice(r3);
		topology.addDevice(r4);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(h2.getHostInterface(), r2.getInterfaces().get(1)));
		topology.addConnection(new Connection(r2.getInterfaces().get(2), r3.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r3.getInterfaces().get(1), r4.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(2), r4.getInterfaces().get(1)));

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r1, "eth0", InterfaceAddress.fromString("192.168.1.254/24"));
		routerConfigurationService.configureInterface(r1, "eth1", InterfaceAddress.fromString("192.168.3.1/24"));
		routerConfigurationService.configureInterface(r1, "eth2", InterfaceAddress.fromString("192.168.6.1/24"));
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), r1.findFromName("eth1")));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r2, "eth0", InterfaceAddress.fromString("192.168.3.2/24"));
		routerConfigurationService.configureInterface(r2, "eth1", InterfaceAddress.fromString("192.168.2.254/24"));
		routerConfigurationService.configureInterface(r2, "eth2", InterfaceAddress.fromString("192.168.4.1/24"));
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), r2.findFromName("eth2")));
		r2.getConfigSession().commit();

		RouterModeController.setMode(r3, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r3, "eth0", InterfaceAddress.fromString("192.168.4.2/24"));
		routerConfigurationService.configureInterface(r3, "eth1", InterfaceAddress.fromString("192.168.5.1/24"));
		routerConfigurationService.addRoute(r3, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), r3.findFromName("eth1")));
		r3.getConfigSession().commit();

		RouterModeController.setMode(r4, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r4, "eth0", InterfaceAddress.fromString("192.168.5.2/24"));
		routerConfigurationService.configureInterface(r4, "eth1", InterfaceAddress.fromString("192.168.6.2/24"));
		r4.getConfigSession().commit();

		PingStatistics stats1 = new PingService().ping(h1, "192.168.1.254", 4, topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should reach default gateway");

		PingStatistics stats2 = new PingService().ping(h1, "192.168.2.1", 4, topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Reply lost due to broken return path at R4");
	}

	@Test
	void testBlackholeRoute() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
		Host h2 = new Host("PC2", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 2, 1), new SubnetMask(24)), new IPAddress(192, 168, 2, 254)));
		Host h3 = new Host("PC3", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 3, 1), new SubnetMask(24)), new IPAddress(192, 168, 3, 254)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));

		topology.addDevice(h1);
		topology.addDevice(h2);
		topology.addDevice(h3);
		topology.addDevice(r1);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(h2.getHostInterface(), r1.getInterfaces().get(1)));
		topology.addConnection(new Connection(h3.getHostInterface(), r1.getInterfaces().get(2)));

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r1, "eth0", InterfaceAddress.fromString("192.168.1.254/24"));
		routerConfigurationService.configureInterface(r1, "eth1", InterfaceAddress.fromString("192.168.2.254/24"));
		routerConfigurationService.disableInterface(r1, "eth2");
		routerConfigurationService.configureInterface(r1, "eth2", InterfaceAddress.fromString("192.168.3.254/24"));
		r1.getConfigSession().commit();

		PingStatistics stats = new PingService().ping(h1, "192.168.3.1", 4, topology);
		assertEquals(4, stats.getSent());
		assertEquals(0, stats.getReceived(), "Should not receive a reply due to blackhole route");

		PingStatistics stats1 = new PingService().ping(h1, "192.168.3.254", 4, topology);
		assertEquals(4, stats1.getSent());
		assertEquals(0, stats1.getReceived(), "Should not receive a reply from a disabled interface");

		PingStatistics stats2 = new PingService().ping(h1, "192.168.2.1", 4, topology);
		assertEquals(4, stats2.getSent());
		assertEquals(4, stats2.getReceived(), "Should receive a reply from a direct route");

		PingStatistics stats3 = new PingService().ping(h1, "192.168.2.254", 4, topology);
		assertEquals(4, stats3.getSent());
		assertEquals(4, stats3.getReceived(), "Should receive a reply from enabled interface");
	}

	@Test
	void testRouteViaNonexistentNextHop() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));

		topology.addDevice(h1);
		topology.addDevice(r1);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r1, "eth0", InterfaceAddress.fromString("192.168.1.254/24"));
		routerConfigurationService.configureInterface(r1, "eth1", InterfaceAddress.fromString("192.168.2.1/24"));
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(0, 0, 0, 0), new SubnetMask(0)), new IPAddress(192, 168, 2, 2)));
		r1.getConfigSession().commit();

		PingStatistics stats1 = new PingService().ping(h1, "192.168.3.1", 4, topology);
		assertEquals(4, stats1.getSent());
		assertEquals(0, stats1.getReceived(), "Should not receive a reply from a route via nonexistent next-hop");
	}

	@Test
	void testNextHopOnLocalInterface() {
		NetworkTopology networkTopology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
		Host h2 = new Host("PC2", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 2, 1), new SubnetMask(24)), new IPAddress(192, 168, 2, 254)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

		networkTopology.addDevice(h1);
		networkTopology.addDevice(h2);
		networkTopology.addDevice(r1);
		networkTopology.addDevice(r2);

		networkTopology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		networkTopology.addConnection(new Connection(h2.getHostInterface(), r2.getInterfaces().getFirst()));
		networkTopology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().get(1)));

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r1, "eth0", InterfaceAddress.fromString("192.168.1.254/24"));
		routerConfigurationService.configureInterface(r1, "eth1", InterfaceAddress.fromString("192.168.3.1/30"));
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), new IPAddress(192, 168, 3, 1)));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r2, "eth0", InterfaceAddress.fromString("192.168.2.254/24"));
		routerConfigurationService.configureInterface(r2, "eth1", InterfaceAddress.fromString("192.168.3.2/30"));
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 3, 1)));
		r2.getConfigSession().commit();

		PingStatistics stats = new PingService().ping(h1, "192.168.2.1", 4, networkTopology);
		assertEquals(4, stats.getSent());
		assertEquals(0, stats.getReceived(), "Should not receive a reply due to invalid next-hop configuration");
	}

	@Test
	void testNextHopNotOnTheNeighbor() {
		NetworkTopology networkTopology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
		Host h2 = new Host("PC2", new HostInterface("Ethernet0", new InterfaceAddress(new IPAddress(192, 168, 2, 1), new SubnetMask(24)), new IPAddress(192, 168, 2, 254)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

		networkTopology.addDevice(h1);
		networkTopology.addDevice(h2);
		networkTopology.addDevice(r1);
		networkTopology.addDevice(r2);

		networkTopology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		networkTopology.addConnection(new Connection(h2.getHostInterface(), r2.getInterfaces().getFirst()));
		networkTopology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().get(1)));

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r1, "eth0", InterfaceAddress.fromString("192.168.1.254/24"));
		routerConfigurationService.configureInterface(r1, "eth1", InterfaceAddress.fromString("192.168.3.1/30"));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.configureInterface(r2, "eth0", InterfaceAddress.fromString("192.168.2.254/24"));
		routerConfigurationService.configureInterface(r2, "eth1", InterfaceAddress.fromString("192.168.3.2/30"));
		r2.getConfigSession().commit();

		RouterModeController.setMode(r1, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r1, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), new IPAddress(192, 168, 4, 1)));
		r1.getConfigSession().commit();

		RouterModeController.setMode(r2, RouterMode.CONFIGURATION);
		routerConfigurationService.addRoute(r2, new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 3, 1)));
		r2.getConfigSession().commit();

		PingStatistics stats = new PingService().ping(h1, "192.168.2.1", 4, networkTopology);
		assertEquals(4, stats.getSent());
		assertEquals(0, stats.getReceived(), "Should not receive a reply due to next-hop not being on the neighbor");
	}
}