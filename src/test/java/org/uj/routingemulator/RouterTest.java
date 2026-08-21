package org.uj.routingemulator;

import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.exceptions.ConfigurationNotFoundException;
import org.uj.routingemulator.router.exceptions.DuplicateConfigurationException;
import org.uj.routingemulator.router.exceptions.InvalidModeException;

import static org.junit.jupiter.api.Assertions.*;

class RouterTest {

	@Test
	void testDefaultRouterConstructor() {
		Router router = new Router("Router");
		assertEquals("Router", router.getName());
		assertTrue(router.getRoutingTable().getRoutingEntries().isEmpty());
		assertEquals(2, router.getInterfaces().size());
		assertEquals("eth0", router.getInterfaces().get(0).getInterfaceName());
		assertEquals("lo", router.getInterfaces().get(1).getInterfaceName());
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertEquals(router.getRoutingTable(), router.getConfigSession().getStagedRoutingTable());
		assertEquals(router.getInterfaces(), router.getConfigSession().getStagedInterfaces());
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
		assertEquals(router.getRoutingTable(), router.getConfigSession().getStagedRoutingTable());
		assertEquals(router.getInterfaces(), router.getConfigSession().getStagedInterfaces());
		assertFalse(router.hasUncommittedChanges());
	}

	@Test
	void testAddRouteInConfigurationMode() {
		RouterInterface iface = new RouterInterface("eth0");
		RouterInterface iface1 = new RouterInterface("eth1");
		RouterInterface iface2 = new RouterInterface("eth2");
		Router router = new Router("Router", java.util.List.of(iface, iface1, iface2));
		router.setMode(RouterMode.CONFIGURATION);

		router.getConfigSession().configureInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)));
		router.getConfigSession().configureInterface("eth1", new InterfaceAddress(new IPAddress(192, 168, 2, 1), new SubnetMask(24)));

		StaticRoutingEntry unicastDefaultDistance = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		StaticRoutingEntry unicastWithDistance = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 2, 0), new SubnetMask(24)), new IPAddress(192, 168, 2, 1), 150);
		StaticRoutingEntry nextHopDefaultDistance = new StaticRoutingEntry(new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)), iface1);
		StaticRoutingEntry nextHopWithDistance = new StaticRoutingEntry(new Subnet(new IPAddress(172, 16, 0, 0), new SubnetMask(12)), iface2, 200);

		router.getConfigSession().addRoute(unicastDefaultDistance);
		router.getConfigSession().addRoute(unicastWithDistance);
		router.getConfigSession().addRoute(nextHopDefaultDistance);
		router.getConfigSession().addRoute(nextHopWithDistance);

		assertEquals(4, router.getConfigSession().getStagedRoutingTable().getRoutingEntries().size());
		assertTrue(router.getConfigSession().getStagedRoutingTable().contains(unicastDefaultDistance));
		assertTrue(router.getConfigSession().getStagedRoutingTable().contains(unicastWithDistance));
		assertTrue(router.getConfigSession().getStagedRoutingTable().contains(nextHopDefaultDistance));
		assertTrue(router.getConfigSession().getStagedRoutingTable().contains(nextHopWithDistance));
	}

	@Test
	void testAddRouteInOperationalModeThrowsException() {
		Router router = new Router("Router");
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));

		RuntimeException exception = assertThrows(InvalidModeException.class, () -> router.getConfigSession().addRoute(entry));
		assertEquals("Invalid command: set [protocols]", exception.getMessage());
	}

	@Test
	void testAddDuplicateRouteThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		router.getConfigSession().configureInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)));

		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.getConfigSession().addRoute(entry);

		RuntimeException exception = assertThrows(DuplicateConfigurationException.class, () -> router.getConfigSession().addRoute(entry));
		assertEquals("Route already exists", exception.getMessage());
	}

	@Test
	void removeRouteInConfigurationMode() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		router.getConfigSession().configureInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)));

		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.getConfigSession().addRoute(entry);
		assertTrue(router.getConfigSession().getStagedRoutingTable().contains(entry));

		router.getConfigSession().removeRoute(entry);
		assertFalse(router.getConfigSession().getStagedRoutingTable().contains(entry));
	}

	@Test
	void testRemoveRouteInOperationalModeThrowsException() {
		Router router = new Router("Router");
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));

		RuntimeException exception = assertThrows(InvalidModeException.class, () -> router.getConfigSession().removeRoute(entry));
		assertEquals("Invalid command: delete [protocols]", exception.getMessage());
	}

	@Test
	void testRemoveNonExistentRouteThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));

		RuntimeException exception = assertThrows(ConfigurationNotFoundException.class, () -> router.getConfigSession().removeRoute(entry));
		assertEquals("Nothing to delete", exception.getMessage());
	}

	@Test
	void testConfigureInterfaceUpdatesInterfaceConfiguration() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		InterfaceAddress address = new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24));

		router.getConfigSession().configureInterface("eth0", address);

		RouterInterface iface = router.getConfigSession().getStagedInterfaces().stream()
				.filter(i -> i.getInterfaceName().equals("eth0"))
				.findFirst()
				.orElse(null);

		assertNotNull(iface);
		assertEquals(address, iface.getInterfaceAddress());
	}
}