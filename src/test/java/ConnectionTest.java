import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.Connection;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.switching.SwitchPort;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Connection class.
 */
class ConnectionTest {

	private RouterInterface routerInterface1;
	private RouterInterface routerInterface2;
	private SwitchPort switchPort1;
	private HostInterface hostInterface1;

	@BeforeEach
	void setUp() {
		routerInterface1 = new RouterInterface("eth0");
		routerInterface2 = new RouterInterface("eth1");
		switchPort1 = new SwitchPort("GigabitEthernet0/1");
		hostInterface1 = new HostInterface(
			"Ethernet0",
			new Subnet(new IPAddress(192, 168, 1, 1), new SubnetMask(24)),
			new IPAddress(192, 168, 1, 254)
		);
	}

	@Test
	void testConnectionCreationRouterToRouter() {
		Connection connection = new Connection(routerInterface1, routerInterface2);

		assertNotNull(connection);
		assertEquals(routerInterface1, connection.getInterfaceA());
		assertEquals(routerInterface2, connection.getInterfaceB());
	}

	@Test
	void testConnectionCreationRouterToSwitch() {
		Connection connection = new Connection(routerInterface1, switchPort1);

		assertNotNull(connection);
		assertEquals(routerInterface1, connection.getInterfaceA());
		assertEquals(switchPort1, connection.getInterfaceB());
	}

	@Test
	void testConnectionCreationSwitchToHost() {
		Connection connection = new Connection(switchPort1, hostInterface1);

		assertNotNull(connection);
		assertEquals(switchPort1, connection.getInterfaceA());
		assertEquals(hostInterface1, connection.getInterfaceB());
	}

	@Test
	void testConnectionEquality() {
		Connection connection1 = new Connection(routerInterface1, routerInterface2);
		Connection connection2 = new Connection(routerInterface1, routerInterface2);

		assertEquals(connection1, connection2);
		assertEquals(connection1.hashCode(), connection2.hashCode());
	}

	@Test
	void testConnectionInequality() {
		Connection connection1 = new Connection(routerInterface1, routerInterface2);
		Connection connection2 = new Connection(routerInterface1, switchPort1);

		assertNotEquals(connection1, connection2);
	}

	@Test
	void testConnectionToString() {
		Connection connection = new Connection(routerInterface1, routerInterface2);
		String toString = connection.toString();

		assertNotNull(toString);
		// Just verify toString returns something meaningful
		assertFalse(toString.isEmpty());
	}

	@Test
	void testConnectionWithSameInterfaceTwice() {
		// Should be able to create connection with same interface on both ends
		// (though it doesn't make practical sense)
		Connection connection = new Connection(routerInterface1, routerInterface1);

		assertNotNull(connection);
		assertEquals(routerInterface1, connection.getInterfaceA());
		assertEquals(routerInterface1, connection.getInterfaceB());
	}

	@Test
	void testConnectionImmutability() {
		Connection connection = new Connection(routerInterface1, routerInterface2);

		var ifaceA = connection.getInterfaceA();
		var ifaceB = connection.getInterfaceB();

		// Verify getters return the same instances
		assertSame(ifaceA, connection.getInterfaceA());
		assertSame(ifaceB, connection.getInterfaceB());
	}

	@Test
	void testConnectionBetweenDifferentInterfaceTypes() {
		// Router to Switch
		Connection conn1 = new Connection(routerInterface1, switchPort1);
		assertNotNull(conn1);

		// Switch to Host
		Connection conn2 = new Connection(switchPort1, hostInterface1);
		assertNotNull(conn2);

		// Router to Host (theoretically possible, though not typical)
		Connection conn3 = new Connection(routerInterface1, hostInterface1);
		assertNotNull(conn3);
	}

	@Test
	void testMultipleConnectionsWithSameInterfaces() {
		Connection connection1 = new Connection(routerInterface1, routerInterface2);
		Connection connection2 = new Connection(routerInterface1, routerInterface2);

		// They should be equal
		assertEquals(connection1, connection2);

		// But they are different objects
		assertNotSame(connection1, connection2);
	}

	@Test
	void testConnectionHashCodeConsistency() {
		Connection connection = new Connection(routerInterface1, routerInterface2);

		int hashCode1 = connection.hashCode();
		int hashCode2 = connection.hashCode();

		assertEquals(hashCode1, hashCode2, "Hash code should be consistent");
	}
}

