import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.switching.Switch;
import org.uj.routingemulator.switching.SwitchPort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NetworkTopology class.
 */
class NetworkTopologyTest {

	private NetworkTopology topology;
	private Router router1;
	private Router router2;
	private Switch switch1;
	private Host host1;

	@BeforeEach
	void setUp() {
		topology = new NetworkTopology();

		router1 = new Router("R1", List.of(
			new RouterInterface("eth0"),
			new RouterInterface("eth1")
		));

		router2 = new Router("R2", List.of(
			new RouterInterface("eth0"),
			new RouterInterface("eth1")
		));

		switch1 = new Switch("SW1", List.of(
			new SwitchPort("GigabitEthernet0/1"),
			new SwitchPort("GigabitEthernet0/2")
		));

		host1 = new Host("PC1", new HostInterface(
			"Ethernet0",
			new Subnet(new IPAddress(192, 168, 1, 1), new SubnetMask(24)),
			new IPAddress(192, 168, 1, 254)
		));
	}

	@Test
	void testAddRouter() {
		topology.addRouter(router1);

		assertEquals(1, topology.getRouters().size());
		assertTrue(topology.getRouters().contains(router1));
	}

	@Test
	void testAddMultipleRouters() {
		topology.addRouter(router1);
		topology.addRouter(router2);

		assertEquals(2, topology.getRouters().size());
		assertTrue(topology.getRouters().contains(router1));
		assertTrue(topology.getRouters().contains(router2));
	}

	@Test
	void testAddSwitch() {
		topology.addSwitch(switch1);

		assertEquals(1, topology.getSwitches().size());
		assertTrue(topology.getSwitches().contains(switch1));
	}

	@Test
	void testAddHost() {
		topology.addHost(host1);

		assertEquals(1, topology.getHosts().size());
		assertTrue(topology.getHosts().contains(host1));
	}

	@Test
	void testAddConnection() {
		topology.addRouter(router1);
		topology.addRouter(router2);

		Connection connection = new Connection(
			router1.getInterfaces().get(0),
			router2.getInterfaces().get(0)
		);

		topology.addConnection(connection);

		assertEquals(1, topology.getConnections().size());
		assertTrue(topology.getConnections().contains(connection));
	}

	@Test
	void testAddDuplicateConnectionThrowsException() {
		topology.addRouter(router1);
		topology.addRouter(router2);

		Connection connection = new Connection(
			router1.getInterfaces().get(0),
			router2.getInterfaces().get(0)
		);

		topology.addConnection(connection);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			topology.addConnection(connection);
		});

		assertTrue(exception.getMessage().contains("Connection already exists"));
	}

	@Test
	void testAddReverseConnectionThrowsException() {
		topology.addRouter(router1);
		topology.addRouter(router2);

		Connection connection1 = new Connection(
			router1.getInterfaces().get(0),
			router2.getInterfaces().get(0)
		);

		Connection connection2 = new Connection(
			router2.getInterfaces().get(0),
			router1.getInterfaces().get(0)
		);

		topology.addConnection(connection1);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			topology.addConnection(connection2);
		});

		assertTrue(exception.getMessage().contains("Connection already exists"));
	}

	@Test
	void testAddConnectionWithAlreadyConnectedInterfaceThrowsException() {
		topology.addRouter(router1);
		topology.addRouter(router2);

		RouterInterface eth0R1 = router1.getInterfaces().get(0);
		RouterInterface eth0R2 = router2.getInterfaces().get(0);
		RouterInterface eth1R2 = router2.getInterfaces().get(1);

		Connection connection1 = new Connection(eth0R1, eth0R2);
		topology.addConnection(connection1);

		// Try to connect eth0R1 to another interface (it's already connected)
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			topology.addConnection(new Connection(eth0R1, eth1R2));
		});

		assertTrue(exception.getMessage().contains("is already connected"));
	}

	@Test
	void testRemoveRouter() {
		topology.addRouter(router1);
		topology.addRouter(router2);

		Connection connection = new Connection(
			router1.getInterfaces().get(0),
			router2.getInterfaces().get(0)
		);
		topology.addConnection(connection);

		topology.removeRouter(router1);

		assertFalse(topology.getRouters().contains(router1));
		// Connection should also be removed
		assertFalse(topology.getConnections().contains(connection));
	}

	@Test
	void testRemoveSwitch() {
		topology.addSwitch(switch1);
		topology.addHost(host1);

		Connection connection = new Connection(
			switch1.getPorts().get(0),
			host1.getHostInterface()
		);
		topology.addConnection(connection);

		topology.removeSwitch(switch1);

		assertFalse(topology.getSwitches().contains(switch1));
		// Connection should also be removed
		assertFalse(topology.getConnections().contains(connection));
	}

	@Test
	void testRemoveHost() {
		topology.addHost(host1);
		topology.addSwitch(switch1);

		Connection connection = new Connection(
			host1.getHostInterface(),
			switch1.getPorts().get(0)
		);
		topology.addConnection(connection);

		topology.removeHost(host1);

		assertFalse(topology.getHosts().contains(host1));
		// Connection should also be removed
		assertFalse(topology.getConnections().contains(connection));
	}

	@Test
	void testRemoveConnection() {
		topology.addRouter(router1);
		topology.addRouter(router2);

		Connection connection = new Connection(
			router1.getInterfaces().get(0),
			router2.getInterfaces().get(0)
		);
		topology.addConnection(connection);

		topology.removeConnection(connection);

		assertFalse(topology.getConnections().contains(connection));
	}

	@Test
	void testVisualize() {
		topology.addRouter(router1);
		topology.addSwitch(switch1);
		topology.addHost(host1);

		Connection conn1 = new Connection(
			router1.getInterfaces().get(0),
			switch1.getPorts().get(0)
		);
		Connection conn2 = new Connection(
			switch1.getPorts().get(1),
			host1.getHostInterface()
		);
		topology.addConnection(conn1);
		topology.addConnection(conn2);

		String visualization = topology.visualize();

		assertNotNull(visualization);
		assertTrue(visualization.contains("Network Topology"));
		assertTrue(visualization.contains("R1"));
		assertTrue(visualization.contains("SW1"));
		assertTrue(visualization.contains("PC1"));
		assertTrue(visualization.contains("Connections"));
	}

	@Test
	void testEmptyTopologyVisualization() {
		String visualization = topology.visualize();

		assertNotNull(visualization);
		assertTrue(visualization.contains("Network Topology"));
		assertTrue(visualization.contains("Hosts:"));
		assertTrue(visualization.contains("Switches:"));
		assertTrue(visualization.contains("Routers:"));
		assertTrue(visualization.contains("Connections:"));
	}

	@Test
	void testComplexTopology() {
		// Create a more complex topology: R1 -- SW1 -- PC1
		//                                    |
		//                                   R2
		topology.addRouter(router1);
		topology.addRouter(router2);
		topology.addSwitch(switch1);
		topology.addHost(host1);

		topology.addConnection(new Connection(
			router1.getInterfaces().get(0),
			switch1.getPorts().get(0)
		));

		topology.addConnection(new Connection(
			router2.getInterfaces().get(0),
			switch1.getPorts().get(1)
		));

		assertEquals(2, topology.getRouters().size());
		assertEquals(1, topology.getSwitches().size());
		assertEquals(1, topology.getHosts().size());
		assertEquals(2, topology.getConnections().size());
	}

	@Test
	void testTopologyConstructorWithParameters() {
		NetworkTopology topology2 = new NetworkTopology(
			List.of(host1),
			List.of(switch1),
			List.of(router1),
			List.of()
		);

		assertEquals(1, topology2.getHosts().size());
		assertEquals(1, topology2.getSwitches().size());
		assertEquals(1, topology2.getRouters().size());
		assertEquals(0, topology2.getConnections().size());
	}

	@Test
	void testRemoveNonExistentDeviceDoesNotThrowException() {
		// Should not throw exception when removing non-existent device
		assertDoesNotThrow(() -> topology.removeRouter(router1));
		assertDoesNotThrow(() -> topology.removeSwitch(switch1));
		assertDoesNotThrow(() -> topology.removeHost(host1));
	}

	@Test
	void testMultipleConnectionsToSameDevice() {
		topology.addRouter(router1);
		topology.addRouter(router2);

		// Connect both interfaces of router2 to router1's interfaces
		Connection conn1 = new Connection(
			router1.getInterfaces().get(0),
			router2.getInterfaces().get(0)
		);
		Connection conn2 = new Connection(
			router1.getInterfaces().get(1),
			router2.getInterfaces().get(1)
		);

		topology.addConnection(conn1);
		topology.addConnection(conn2);

		assertEquals(2, topology.getConnections().size());
	}

	@Test
	void testRemoveConnectionByInterfaces() {
		topology.addRouter(router1);
		topology.addRouter(router2);

		Connection connection = new Connection(
			router1.getInterfaces().get(0),
			router2.getInterfaces().get(0)
		);
		topology.addConnection(connection);

		// Create new connection with same interfaces and remove
		Connection connectionToRemove = new Connection(
			router1.getInterfaces().get(0),
			router2.getInterfaces().get(0)
		);
		topology.removeConnection(connectionToRemove);

		assertEquals(0, topology.getConnections().size());
	}

	@Test
	void testRemoveRouterWithMultipleConnections() {
		topology.addRouter(router1);
		topology.addRouter(router2);
		topology.addSwitch(switch1);

		// Router1 has two connections
		Connection conn1 = new Connection(
			router1.getInterfaces().get(0),
			router2.getInterfaces().get(0)
		);
		Connection conn2 = new Connection(
			router1.getInterfaces().get(1),
			switch1.getPorts().get(0)
		);

		topology.addConnection(conn1);
		topology.addConnection(conn2);

		assertEquals(2, topology.getConnections().size());

		// Remove router1 - both connections should be removed
		topology.removeRouter(router1);

		assertEquals(0, topology.getConnections().size());
	}

	@Test
	void testVisualizationContainsConnectionDetails() {
		topology.addRouter(router1);
		topology.addRouter(router2);

		Connection connection = new Connection(
			router1.getInterfaces().get(0),
			router2.getInterfaces().get(0)
		);
		topology.addConnection(connection);

		String visualization = topology.visualize();

		assertTrue(visualization.contains("eth0"));
		assertTrue(visualization.contains("<──>") || visualization.contains("--"));
	}
}

