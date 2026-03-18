import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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


		// Same subnet → should work
		PingStatistics stats1 = h1.ping("192.168.1.254", topology);
		assertEquals(4, stats1.getSent());
		assertEquals(4, stats1.getReceived(), "Should reach default gateway");

		// Asymmetric routing → should fail
		PingStatistics stats2 = h1.ping("192.168.2.1", topology);
		assertEquals(4, stats2.getSent());
		assertEquals(0, stats2.getReceived(), "Reply lost due to broken return path at R4");
	}
}