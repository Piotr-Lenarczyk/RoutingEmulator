import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.common.exceptions.RoutingLoopException;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.exceptions.InterfaceNotFoundException;
import org.uj.routingemulator.router.exceptions.InterfaceUnavailableException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LogicalErrorTest {

	/**
	 * This test simulates a scenario where a ping is sent to a destination that has no return route.
	 * The expected behavior is that the ping should fail with an appropriate error message.
	 */
	@Test
	public void testNoReturnRoute() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
		Host h2 = new Host("PC2", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 2, 1), new SubnetMask(24)), new IPAddress(192, 168, 2, 254)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

		topology.addHost(h1);
		topology.addHost(h2);
		topology.addRouter(r1);
		topology.addRouter(r2);

		r1.setMode(RouterMode.CONFIGURATION);
		r1.configureInterface("eth0", InterfaceAddress.fromString("192.168.1.254/24"));
		r1.configureInterface("eth1", InterfaceAddress.fromString("192.168.3.1/24"));
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), r1.findFromName("eth1")));
		r1.commitChanges();

		r2.setMode(RouterMode.CONFIGURATION);
		r2.configureInterface("eth0", InterfaceAddress.fromString("192.168.3.2/24"));
		r2.configureInterface("eth1", InterfaceAddress.fromString("192.168.2.254/24"));
		r2.commitChanges();

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r2.getInterfaces().get(1), h2.getHostInterface()));

		// Same subnet, default gateway
		PingStatistics stats = h1.ping("192.168.1.254", topology);
		assertEquals(4, stats.getSent());
		assertEquals(4, stats.getReceived(), "Should be able to ping default gateway");

		// Different subnet but router can respond locally without forwarding
		PingStatistics stats1 = h1.ping("192.168.3.1", topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should be able to ping connected router interface");

		// No return route from R2 to R1
		PingStatistics stats2 = h1.ping("192.168.3.2", topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Should not be able to ping indirectly connected router interface");

		// No return route from R2 to R1
		PingStatistics stats3 = h1.ping("192.168.2.254", topology);
		assertEquals(4, stats3.getSent());
		assertEquals(0, stats3.getReceived(), "Should not be able to ping indirectly connected router interface");

		// PC2 will respond but ping will fail on R2
		PingStatistics stats4 = h1.ping("192.168.2.1", topology);
		assertEquals(4, stats4.getSent());
		assertEquals(0, stats4.getReceived(), "Should not receive a reply without return route");
	}

	/**
	 * This test simulates a scenario where a ping is sent to a destination that has an asymmetric, misconfigured route.
	 * The expected behavior is that the ping should fail due to the lack of a return path.
	 */
	@Test
	public void testRoutingAsymmetry() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
		Host h2 = new Host("PC2", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 2, 1), new SubnetMask(24)), new IPAddress(192, 168, 2, 254)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r3 = new Router("R3", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r4 = new Router("R4", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));

		topology.addHost(h1);
		topology.addHost(h2);
		topology.addRouter(r1);
		topology.addRouter(r2);
		topology.addRouter(r3);
		topology.addRouter(r4);

		// H1 -> R1 - R2 <- H2
		//       |    |
		//       X    |
		//       |    |
		//       R4 - R3
		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(h2.getHostInterface(), r2.getInterfaces().get(1)));
		topology.addConnection(new Connection(r2.getInterfaces().get(2), r3.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r3.getInterfaces().get(1), r4.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(2), r4.getInterfaces().get(1)));

		r1.setMode(RouterMode.CONFIGURATION);
		r1.configureInterface("eth0", InterfaceAddress.fromString("192.168.1.254/24"));
		r1.configureInterface("eth1", InterfaceAddress.fromString("192.168.3.1/24"));
		r1.configureInterface("eth2", InterfaceAddress.fromString("192.168.6.1/24"));
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)),
				r1.findFromName("eth1")));
		r1.commitChanges();

		r2.setMode(RouterMode.CONFIGURATION);
		r2.configureInterface("eth0", InterfaceAddress.fromString("192.168.3.2/24"));
		r2.configureInterface("eth1", InterfaceAddress.fromString("192.168.2.254/24"));
		r2.configureInterface("eth2", InterfaceAddress.fromString("192.168.4.1/24"));
		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)),
				r2.findFromName("eth2")));
		r2.commitChanges();

		r3.setMode(RouterMode.CONFIGURATION);
		r3.configureInterface("eth0", InterfaceAddress.fromString("192.168.4.2/24"));
		r3.configureInterface("eth1", InterfaceAddress.fromString("192.168.5.1/24"));
		r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)),
				r3.findFromName("eth1")));
		r3.commitChanges();

		r4.setMode(RouterMode.CONFIGURATION);
		r4.configureInterface("eth0", InterfaceAddress.fromString("192.168.5.2/24"));
		r4.configureInterface("eth1", InterfaceAddress.fromString("192.168.6.2/24"));
		r4.commitChanges();


		// Same subnet - should work
		PingStatistics stats1 = h1.ping("192.168.1.254", topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should reach default gateway");

		// Asymmetric routing - should fail
		PingStatistics stats2 = h1.ping("192.168.2.1", topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Reply lost due to broken return path at R4");
	}

	/**
	 * This test simulates a scenario where a ping is sent via shutdown or unreachable in L2 interface
	 * Ping will not be successful and the router should warn the user about the issue
	 */
	@Test
	public void testBlackholeRoute() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
		Host h2 = new Host("PC2", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 2, 1), new SubnetMask(24)), new IPAddress(192, 168, 2, 254)));
		Host h3 = new Host("PC3", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 3, 1), new SubnetMask(24)), new IPAddress(192, 168, 3, 254)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));

		topology.addHost(h1);
		topology.addHost(h2);
		topology.addHost(h3);
		topology.addRouter(r1);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(h2.getHostInterface(), r1.getInterfaces().get(1)));
		topology.addConnection(new Connection(h3.getHostInterface(), r1.getInterfaces().get(2)));

		r1.setMode(RouterMode.CONFIGURATION);
		r1.configureInterface("eth0", InterfaceAddress.fromString("192.168.1.254/24"));
		r1.configureInterface("eth1", InterfaceAddress.fromString("192.168.2.254/24"));
		r1.disableInterface("eth2");
		InterfaceUnavailableException ex = assertThrows(InterfaceUnavailableException.class, () ->
				r1.configureInterface("eth2", InterfaceAddress.fromString("192.168.3.254/24"))
		);
		assertThat(ex.getMessage()).contains("Interface eth2 is disabled");
		assertThat(ex.getMessage()).contains("Packets routed through this interface will be dropped");
		assertThat(ex.getMessage()).contains("Would you like to proceed anyway? (Y/N)");
		r1.confirm();
		r1.commitChanges();

		PingStatistics stats = h1.ping("192.168.3.1", topology);
		assertEquals(4, stats.getSent());
		assertEquals(0, stats.getReceived(), "Should not receive a reply due to blackhole route");

		PingStatistics stats1 = h1.ping("192.168.3.254", topology);
		assertEquals(4, stats1.getSent());
		assertEquals(0, stats1.getReceived(), "Should not receive a reply from a disabled interface");

		PingStatistics stats2 = h1.ping("192.168.2.1", topology);
		assertEquals(4, stats2.getSent());
		assertEquals(4, stats2.getReceived(), "Should receive a reply from a direct route");

		PingStatistics stats3 = h1.ping("192.168.2.254", topology);
		assertEquals(4, stats3.getSent());
		assertEquals(4, stats3.getReceived(), "Should receive a reply from enabled interface");
	}

	@Test
	public void testRouteViaNonexistentNextHop() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));

		topology.addHost(h1);
		topology.addRouter(r1);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));

		r1.setMode(RouterMode.CONFIGURATION);
		r1.configureInterface("eth0", InterfaceAddress.fromString("192.168.1.254/24"));
		r1.configureInterface("eth1", InterfaceAddress.fromString("192.168.2.1/24"));
		InterfaceNotFoundException ex = assertThrows(InterfaceNotFoundException.class, () ->
				r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(0, 0, 0, 0), new SubnetMask(0)), new IPAddress(192, 168, 2, 2)))
		);
		assertThat(ex.getMessage()).contains("Next-hop interface 192.168.2.2/24 not found on the router");
		assertThat(ex.getMessage()).contains("Packets routed through this interface will be dropped");
		assertThat(ex.getMessage()).contains("Would you like to proceed anyway? (Y/N)");
		r1.confirm();
		r1.commitChanges();

		PingStatistics stats1 = h1.ping("192.168.3.1", topology);
		assertEquals(4, stats1.getSent());
		assertEquals(0, stats1.getReceived(), "Should not receive a reply from a route via nonexistent next-hop");
	}

	/**
	 * This test checks if the system can detect if a routing loop was created due to misconfiguration.
	 * Upon issuing a router configuration command that would create a loop, system should warn the user,
	 * display a confirmation dialog but let them proceed if they choose to
	 */
	@Disabled("Not yet implemented")
	@Test
	public void testRoutingLoop() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
		Host h2 = new Host("PC2", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 2, 1), new SubnetMask(24)), new IPAddress(192, 168, 2, 254)));
		Host h3 = new Host("PC3", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 3, 1), new SubnetMask(24)), new IPAddress(192, 168, 3, 254)));

		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r3 = new Router("R3", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));

		topology.addHost(h1);
		topology.addHost(h2);
		topology.addHost(h3);
		topology.addRouter(r1);
		topology.addRouter(r2);
		topology.addRouter(r3);

		topology.addConnection(new Connection(h1.getHostInterface(), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(h2.getHostInterface(), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(h3.getHostInterface(), r3.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().get(1)));
		topology.addConnection(new Connection(r2.getInterfaces().get(2), r3.getInterfaces().get(1)));
		topology.addConnection(new Connection(r3.getInterfaces().get(2), r1.getInterfaces().get(2)));

		r1.setMode(RouterMode.CONFIGURATION);
		r1.configureInterface("eth0", InterfaceAddress.fromString("192.168.1.254/24"));
		r1.configureInterface("eth1", InterfaceAddress.fromString("192.168.4.1/30"));
		r1.configureInterface("eth2", InterfaceAddress.fromString("192.168.4.10/30"));
		r1.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 3, 0), new SubnetMask(24)), r1.findFromName("eth1")));
		r1.commitChanges();

		r2.setMode(RouterMode.CONFIGURATION);
		r2.configureInterface("eth0", InterfaceAddress.fromString("192.168.2.254/24"));
		r2.configureInterface("eth1", InterfaceAddress.fromString("192.168.4.2/30"));
		r2.configureInterface("eth2", InterfaceAddress.fromString("192.168.4.5/30"));
		r2.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 3, 0), new SubnetMask(24)), r2.findFromName("eth2")));
		r2.commitChanges();

		r3.setMode(RouterMode.CONFIGURATION);
		r3.configureInterface("eth0", InterfaceAddress.fromString("192.168.3.254/24"));
		r3.configureInterface("eth1", InterfaceAddress.fromString("192.168.4.6/30"));
		r3.configureInterface("eth2", InterfaceAddress.fromString("192.168.4.9/30"));
		try {
			r3.addRoute(new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), r3.findFromName("eth1")));
		} catch (RoutingLoopException e) {
			assertThat(e.getMessage()).contains("Routing loop detected");
			assertThat(e.getMessage()).contains("Would you like to proceed anyway? (Y/N)");
		}
		r3.confirm();
		r3.commitChanges();

		PingStatistics stats2 = h1.ping("192.168.3.1", topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Should drop packets due to routing loop");
	}
}
