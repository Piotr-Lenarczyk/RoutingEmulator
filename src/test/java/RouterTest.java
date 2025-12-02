import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RouterTest {
	@Test
	void testDefaultRouterConstructor() {
		Router router = new Router("Router");
		assertEquals("Router", router.getName());
		assertTrue(router.getRoutingTable().getRoutingEntries().isEmpty());
		assertEquals(2, router.getInterfaces().size());
		assertEquals("eth0", router.getInterfaces().get(0).getInterfaceName());
		assertEquals("lo", router.getInterfaces().get(1).getInterfaceName());
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertEquals(router.getRoutingTable(), router.getStagedRoutingTable());
		assertEquals(router.getInterfaces(), router.getStagedInterfaces());
		assertFalse(router.hasUncommittedChanges());
	}

	@Test
	void testRouterConstructorWithInterfaces() {
		RouterInterface iface1 = new RouterInterface("eth1");
		RouterInterface iface2 = new RouterInterface("eth2");
		Router router = new Router("Router", java.util.List.of(iface1, iface2));
		assertEquals("Router", router.getName());
		assertTrue(router.getRoutingTable().getRoutingEntries().isEmpty());
		assertEquals(2, router.getInterfaces().size());
		assertEquals("eth1", router.getInterfaces().get(0).getInterfaceName());
		assertEquals("eth2", router.getInterfaces().get(1).getInterfaceName());
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertEquals(router.getRoutingTable(), router.getStagedRoutingTable());
		assertEquals(router.getInterfaces(), router.getStagedInterfaces());
		assertFalse(router.hasUncommittedChanges());
	}

	@Test
	void testAddRouteInConfigurationMode() {
		RouterInterface iface1 = new RouterInterface("eth1");
		RouterInterface iface2 = new RouterInterface("eth2");
		Router router = new Router("Router", java.util.List.of(iface1, iface2));
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry unicastDefaultDistance = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		StaticRoutingEntry unicastWithDistance = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), new IPAddress(192, 168, 2, 1), 150);
		StaticRoutingEntry nextHopDefaultDistance = new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), iface1);
		StaticRoutingEntry nextHopWithDistance = new StaticRoutingEntry(new Subnet(new IPAddress(172, 16, 0, 0), new SubnetMask(12)), iface2, 200);

		router.addRoute(unicastDefaultDistance);
		router.addRoute(unicastWithDistance);
		router.addRoute(nextHopDefaultDistance);
		router.addRoute(nextHopWithDistance);

		assertEquals(4, router.getStagedRoutingTable().getRoutingEntries().size());
		assertTrue(router.getStagedRoutingTable().contains(unicastDefaultDistance));
		assertTrue(router.getStagedRoutingTable().contains(unicastWithDistance));
		assertTrue(router.getStagedRoutingTable().contains(nextHopDefaultDistance));
		assertTrue(router.getStagedRoutingTable().contains(nextHopWithDistance));
	}

	@Test
	void testAddRouteInOperationalModeThrowsException() {
		Router router = new Router("Router");
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.addRoute(entry);
		});
		assertEquals("Invalid command: set [protocols]", exception.getMessage());
	}

	@Test
	void testAddDuplicateRouteThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.addRoute(entry);
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.addRoute(entry);
		});
		assertEquals("Configuration path: %s already exists".formatted(entry.toString()), exception.getMessage());
	}

	@Test
	void removeRouteInConfigurationMode() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.addRoute(entry);
		assertTrue(router.getStagedRoutingTable().contains(entry));

		router.removeRoute(entry);
		assertFalse(router.getStagedRoutingTable().contains(entry));
	}

	@Test
	void testRemoveRouteInOperationalModeThrowsException() {
		Router router = new Router("Router");
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.removeRoute(entry);
		});
		assertEquals("Invalid command: delete [protocols]", exception.getMessage());
	}

	@Test
	void testRemoveNonExistentRouteThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.removeRoute(entry);
		});
		assertEquals("Configuration path: %s does not exist".formatted(entry.toString()), exception.getMessage());
	}

	@Test
	void testConfigureInterfaceInConfigurationMode() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		Subnet subnet = new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24));
		router.configureInterface("eth0", subnet);
		RouterInterface iface = router.getStagedInterfaces().stream()
				.filter(i -> i.getInterfaceName().equals("eth0"))
				.findFirst()
				.orElseThrow();
		assertEquals(subnet, iface.getSubnet());
	}

	@Test
	void testConfigureInterfaceInOperationalModeThrowsException() {
		Router router = new Router("Router");
		Subnet subnet = new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24));
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.configureInterface("eth0", subnet);
		});
		assertEquals("Invalid command: set [interfaces]", exception.getMessage());
	}

	@Test
	void testConfigureNonExistentInterfaceThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		Subnet subnet = new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24));
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.configureInterface("eth99", subnet);
		});
		assertEquals("WARN: interface eth99 does not exist, changes will not be commited", exception.getMessage());
	}

	@Test
	void testCommitChanges() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.addRoute(entry);
		assertTrue(router.hasUncommittedChanges());
		assertNotEquals(router.getStagedRoutingTable(), router.getRoutingTable());

		router.commitChanges();
		assertFalse(router.hasUncommittedChanges());
		assertTrue(router.getRoutingTable().contains(entry));
		assertEquals(router.getStagedRoutingTable(), router.getRoutingTable());
	}

	@Test
	void testCommitChangesInOperationalModeThrowsException() {
		Router router = new Router("Router");
		RuntimeException exception = assertThrows(RuntimeException.class, router::commitChanges);
		assertEquals("Invalid command: [commit]", exception.getMessage());
	}

	@Test
	void testDiscardChanges() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.addRoute(entry);
		assertTrue(router.hasUncommittedChanges());
		assertNotEquals(router.getStagedRoutingTable(), router.getRoutingTable());

		router.discardChanges();
		assertFalse(router.hasUncommittedChanges());
		assertFalse(router.getRoutingTable().contains(entry));
		assertEquals(router.getStagedRoutingTable(), router.getRoutingTable());
	}

	@Test
	void testDiscardChangesInOperationalModeThrowsException() {
		Router router = new Router("Router");
		RuntimeException exception = assertThrows(RuntimeException.class, router::discardChanges);
		assertEquals("Invalid command: [discard]", exception.getMessage());
	}

	@Test
	void setModeChangesRouterMode() {
		Router router = new Router("Router");
		assertEquals(RouterMode.OPERATIONAL, router.getMode());

		router.setMode(RouterMode.CONFIGURATION);
		assertEquals(RouterMode.CONFIGURATION, router.getMode());

		router.setMode(RouterMode.OPERATIONAL);
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
	}

	@Test
	void setModeWithUncommittedChangesThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.addRoute(entry);
		assertTrue(router.hasUncommittedChanges());

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.setMode(RouterMode.OPERATIONAL);
		});
		assertEquals("Cannot exit: configuration modified.\nUse 'exit discard' to discard the changes and exit.\n[edit]", exception.getMessage());
	}

	@Test
	void setModeForcedDiscardsUncommittedChanges() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.addRoute(entry);
		assertTrue(router.hasUncommittedChanges());
		router.setModeForced(RouterMode.OPERATIONAL);
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertFalse(router.hasUncommittedChanges());
		assertFalse(router.getRoutingTable().contains(entry));
	}

	@Test
	void testResetRestoresRouterToDefaultConfiguration() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.addRoute(entry);
		router.configureInterface("eth0", new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)));

		router.reset();

		assertEquals("Router", router.getName());
		assertTrue(router.getRoutingTable().getRoutingEntries().isEmpty());
		assertEquals(2, router.getInterfaces().size());
		assertEquals("eth0", router.getInterfaces().get(0).getInterfaceName());
		assertNull(router.getInterfaces().get(0).getSubnet());
		assertEquals("lo", router.getInterfaces().get(1).getInterfaceName());
		assertNull(router.getInterfaces().get(1).getSubnet());
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertEquals(router.getRoutingTable(), router.getStagedRoutingTable());
		assertEquals(router.getInterfaces(), router.getStagedInterfaces());
		assertFalse(router.hasUncommittedChanges());
	}
}
