package org.uj.routingemulator;

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
				new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)),
				new IPAddress(192, 168, 1, 254)
		));
	}

	@Test
	void testAddRouter() {
		topology.addDevice(router1);
		assertEquals(1, topology.getDevices().size());
		assertTrue(topology.getDevices().contains(router1));
	}

	@Test
	void testAddMultipleRouters() {
		topology.addDevice(router1);
		topology.addDevice(router2);
		assertEquals(2, topology.getDevices().size());
		assertTrue(topology.getDevices().contains(router1));
		assertTrue(topology.getDevices().contains(router2));
	}

	@Test
	void testAddSwitch() {
		topology.addDevice(switch1);
		assertEquals(1, topology.getDevices().size());
		assertTrue(topology.getDevices().contains(switch1));
	}

	@Test
	void testAddHost() {
		topology.addDevice(host1);
		assertEquals(1, topology.getDevices().size());
		assertTrue(topology.getDevices().contains(host1));
	}

	@Test
	void testAddConnection() {
		topology.addDevice(router1);
		topology.addDevice(router2);
		Connection connection = new Connection(
				router1.getInterfaces().getFirst(),
				router2.getInterfaces().getFirst()
		);
		topology.addConnection(connection);
		assertEquals(1, topology.getConnections().size());
		assertTrue(topology.getConnections().contains(connection));
	}

	@Test
	void testAddDuplicateConnectionThrowsException() {
		topology.addDevice(router1);
		topology.addDevice(router2);
		Connection connection = new Connection(
				router1.getInterfaces().getFirst(),
				router2.getInterfaces().getFirst()
		);
		topology.addConnection(connection);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> topology.addConnection(connection));
		assertTrue(exception.getMessage().contains("Connection already exists"));
	}

	@Test
	void testAddReverseConnectionThrowsException() {
		topology.addDevice(router1);
		topology.addDevice(router2);
		Connection connection1 = new Connection(
				router1.getInterfaces().getFirst(),
				router2.getInterfaces().getFirst()
		);
		Connection connection2 = new Connection(
				router2.getInterfaces().getFirst(),
				router1.getInterfaces().getFirst()
		);
		topology.addConnection(connection1);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> topology.addConnection(connection2));
		assertTrue(exception.getMessage().contains("Connection already exists"));
	}

	@Test
	void testAddConnectionWithAlreadyConnectedInterfaceThrowsException() {
		topology.addDevice(router1);
		topology.addDevice(router2);

		RouterInterface eth0R1 = router1.getInterfaces().getFirst();
		RouterInterface eth0R2 = router2.getInterfaces().get(0);
		RouterInterface eth1R2 = router2.getInterfaces().get(1);

		Connection connection1 = new Connection(eth0R1, eth0R2);
		topology.addConnection(connection1);

		Connection connection2 = new Connection(eth0R1, eth1R2);
		RuntimeException exception = assertThrows(RuntimeException.class, () -> topology.addConnection(connection2));
		assertTrue(exception.getMessage().contains("is already connected"));
	}

	@Test
	void testRemoveRouter() {
		topology.addDevice(router1);
		topology.addDevice(router2);
		Connection connection = new Connection(
				router1.getInterfaces().getFirst(),
				router2.getInterfaces().getFirst()
		);
		topology.addConnection(connection);

		topology.removeDevice(router1.getId());
		assertFalse(topology.getDevices().contains(router1));
		assertFalse(topology.getConnections().contains(connection));
	}

	@Test
	void testRemoveSwitch() {
		topology.addDevice(switch1);
		topology.addDevice(host1);
		Connection connection = new Connection(
				switch1.getPorts().getFirst(),
				host1.getHostInterface()
		);
		topology.addConnection(connection);

		topology.removeDevice(switch1.getId());
		assertFalse(topology.getDevices().contains(switch1));
		assertFalse(topology.getConnections().contains(connection));
	}

	@Test
	void testRemoveHost() {
		topology.addDevice(host1);
		topology.addDevice(switch1);
		Connection connection = new Connection(
				host1.getHostInterface(),
				switch1.getPorts().getFirst()
		);
		topology.addConnection(connection);

		topology.removeDevice(host1.getId());
		assertFalse(topology.getDevices().contains(host1));
		assertFalse(topology.getConnections().contains(connection));
	}

	@Test
	void testRemoveConnection() {
		topology.addDevice(router1);
		topology.addDevice(router2);
		Connection connection = new Connection(
				router1.getInterfaces().getFirst(),
				router2.getInterfaces().getFirst()
		);
		topology.addConnection(connection);

		topology.removeConnection(connection);
		assertFalse(topology.getConnections().contains(connection));
	}

	@Test
	void testVisualize() {
		topology.addDevice(router1);
		topology.addDevice(switch1);
		topology.addDevice(host1);

		Connection conn1 = new Connection(
				router1.getInterfaces().getFirst(),
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
		topology.addDevice(router1);
		topology.addDevice(router2);
		topology.addDevice(switch1);
		topology.addDevice(host1);

		topology.addConnection(new Connection(
				router1.getInterfaces().getFirst(),
				switch1.getPorts().get(0)
		));
		topology.addConnection(new Connection(
				router2.getInterfaces().getFirst(),
				switch1.getPorts().get(1)
		));

		assertEquals(4, topology.getDevices().size());
		assertEquals(2, topology.getConnections().size());
	}

	@Test
	void testTopologyConstructorWithParameters() {
		NetworkTopology topology2 = new NetworkTopology(
				List.of(host1, switch1, router1),
				List.of()
		);

		assertEquals(3, topology2.getDevices().size());
		assertEquals(0, topology2.getConnections().size());
	}

	@Test
	void testRemoveNonExistentDeviceDoesNotThrowException() {
		assertDoesNotThrow(() -> topology.removeDevice(router1.getId()));
		assertDoesNotThrow(() -> topology.removeDevice(switch1.getId()));
		assertDoesNotThrow(() -> topology.removeDevice(host1.getId()));
	}

	@Test
	void testMultipleConnectionsToSameDevice() {
		topology.addDevice(router1);
		topology.addDevice(router2);

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
		topology.addDevice(router1);
		topology.addDevice(router2);

		Connection connection = new Connection(
				router1.getInterfaces().getFirst(),
				router2.getInterfaces().getFirst()
		);
		topology.addConnection(connection);

		Connection connectionToRemove = new Connection(
				router1.getInterfaces().getFirst(),
				router2.getInterfaces().getFirst()
		);
		topology.removeConnection(connectionToRemove);

		assertEquals(0, topology.getConnections().size());
	}

	@Test
	void testRemoveRouterWithMultipleConnections() {
		topology.addDevice(router1);
		topology.addDevice(router2);
		topology.addDevice(switch1);

		Connection conn1 = new Connection(
				router1.getInterfaces().get(0),
				router2.getInterfaces().getFirst()
		);
		Connection conn2 = new Connection(
				router1.getInterfaces().get(1),
				switch1.getPorts().getFirst()
		);

		topology.addConnection(conn1);
		topology.addConnection(conn2);

		assertEquals(2, topology.getConnections().size());

		topology.removeDevice(router1.getId());
		assertEquals(0, topology.getConnections().size());
	}

	@Test
	void testVisualizationContainsConnectionDetails() {
		topology.addDevice(router1);
		topology.addDevice(router2);

		Connection connection = new Connection(
				router1.getInterfaces().getFirst(),
				router2.getInterfaces().getFirst()
		);
		topology.addConnection(connection);

		String visualization = topology.visualize();
		assertTrue(visualization.contains("eth0"));
		assertTrue(visualization.contains("< >") || visualization.contains("--"));
	}
}