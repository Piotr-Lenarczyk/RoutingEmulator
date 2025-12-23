import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.router.*;
import org.junit.jupiter.api.Test;

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
		assertEquals("Route already exists", exception.getMessage());
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
		assertEquals("Nothing to delete", exception.getMessage());
	}

	@Test
	void testConfigureInterfaceUpdatesInterfaceConfiguration() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		InterfaceAddress address = new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24));
		router.configureInterface("eth0", address);
		RouterInterface iface = router.getStagedInterfaces().stream()
				.filter(i -> i.getInterfaceName().equals("eth0"))
				.findFirst()
				.orElseThrow();
		assertEquals(address, iface.getInterfaceAddress());
		// Also verify subnet calculation
		assertEquals(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), iface.getSubnet());
	}

	@Test
	void testConfigureInterfaceInOperationalModeThrowsException() {
		Router router = new Router("Router");
		InterfaceAddress address = new InterfaceAddress(new IPAddress(192, 168, 1, 0), new SubnetMask(24));
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.configureInterface("eth0", address);
		});
		assertEquals("Invalid command: set [interfaces]", exception.getMessage());
	}

	@Test
	void testConfigureNonExistentInterfaceThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		InterfaceAddress address = new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24));
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.configureInterface("eth99", address);
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
		router.configureInterface("eth0", new InterfaceAddress(new IPAddress(10, 0, 0, 1), new SubnetMask(8)));

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

	// Tests for new functionality

	@Test
	void testDisableRouteInConfigurationMode() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.addRoute(entry);

		router.disableRoute(entry);

		assertTrue(router.getStagedRoutingTable().getRoutingEntries().get(0).isDisabled());
		assertTrue(router.hasUncommittedChanges());
	}

	@Test
	void testDisableNonExistentRouteThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.disableRoute(entry);
		});
		assertEquals("Route not found", exception.getMessage());
	}

	@Test
	void testDisableAlreadyDisabledRouteThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.addRoute(entry);
		router.disableRoute(entry);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.disableRoute(entry);
		});
		assertEquals("Route already exists", exception.getMessage());
	}

	@Test
	void testDisableInterfaceInConfigurationMode() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);

		router.disableInterface("eth0");

		RouterInterface iface = router.getStagedInterfaces().stream()
				.filter(i -> i.getInterfaceName().equals("eth0"))
				.findFirst()
				.orElseThrow();
		assertEquals(new InterfaceStatus(AdminState.ADMIN_DOWN, LinkState.DOWN), iface.getStatus());
		assertTrue(router.hasUncommittedChanges());
	}

	@Test
	void testDisableInterfaceInOperationalModeThrowsException() {
		Router router = new Router("Router");

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.disableInterface("eth0");
		});
		assertEquals("Invalid command: set [interfaces]", exception.getMessage());
	}

	@Test
	void testDisableNonExistentInterfaceThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.disableInterface("eth99");
		});
		assertEquals("WARN: interface eth99 does not exist, changes will not be commited", exception.getMessage());
	}

	@Test
	void testDeleteInterfaceAddressInConfigurationMode() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		InterfaceAddress address = new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24));
		router.configureInterface("eth0", address);

		router.deleteInterfaceAddress("eth0");

		RouterInterface iface = router.getStagedInterfaces().stream()
				.filter(i -> i.getInterfaceName().equals("eth0"))
				.findFirst()
				.orElseThrow();
		assertNull(iface.getSubnet());
		assertTrue(router.hasUncommittedChanges());
	}

	@Test
	void testDeleteInterfaceAddressInOperationalModeThrowsException() {
		Router router = new Router("Router");

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.deleteInterfaceAddress("eth0");
		});
		assertEquals("Invalid command: delete [interfaces]", exception.getMessage());
	}

	@Test
	void testDeleteInterfaceAddressFromNonExistentInterfaceThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.deleteInterfaceAddress("eth99");
		});
		assertEquals("WARN: interface eth99 does not exist, changes will not be commited", exception.getMessage());
	}

	@Test
	void testDeleteInterfaceAddressWithNoAddressThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.deleteInterfaceAddress("eth0");
		});
		assertEquals("No value to delete", exception.getMessage());
	}

	@Test
	void testCommitChangesWithNoChangesThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.commitChanges();
		});
		assertEquals("No configuration changes to commit", exception.getMessage());
	}

	@Test
	void testConfigureInterfaceWithNetworkAddressThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		InterfaceAddress networkAddress = new InterfaceAddress(new IPAddress(192, 168, 1, 0), new SubnetMask(24));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.configureInterface("eth0", networkAddress);
		});
		assertTrue(exception.getMessage().contains("Cannot assign network address"));
		assertTrue(exception.getMessage().contains("Use a host address instead"));
	}

	@Test
	void testConfigureInterfaceWithBroadcastAddressThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		InterfaceAddress broadcastAddress = new InterfaceAddress(new IPAddress(192, 168, 1, 255), new SubnetMask(24));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.configureInterface("eth0", broadcastAddress);
		});
		assertTrue(exception.getMessage().contains("Cannot assign broadcast address"));
		assertTrue(exception.getMessage().contains("Use a host address instead"));
	}

	@Test
	void testConfigureDuplicateInterfaceAddressThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		InterfaceAddress address = new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24));
		router.configureInterface("eth0", address);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.configureInterface("eth0", address);
		});
		assertEquals("Configuration already exists", exception.getMessage());
	}

	@Test
	void testFindFromNameReturnsExistingInterface() {
		Router router = new Router("Router");

		RouterInterface iface = router.findFromName("eth0");

		assertNotNull(iface);
		assertEquals("eth0", iface.getInterfaceName());
	}

	@Test
	void testFindFromNameReturnsNullForNonExistentInterface() {
		Router router = new Router("Router");

		RouterInterface iface = router.findFromName("eth99");

		assertNull(iface);
	}

	@Test
	void testDisableRoute() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.addRoute(entry);
		assertFalse(entry.isDisabled());

		router.disableRoute(entry);
		assertTrue(entry.isDisabled());
	}

	@Test
	void testDisableRouteInOperationalModeThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)), new IPAddress(192, 168, 1, 1));
		router.addRoute(entry);
		router.commitChanges();
		router.setMode(RouterMode.OPERATIONAL);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			router.disableRoute(entry);
		});
		assertEquals("Invalid command: set [protocols]", exception.getMessage());
	}

	@Test
	void testDisableInterface() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		router.configureInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)));

		RouterInterface iface = router.findFromName("eth0");
		assertNotNull(iface);
		assertNotEquals("ADMIN_DOWN", iface.getStatus().getAdmin().toString());

		router.disableInterface("eth0");
		assertEquals("ADMIN_DOWN", iface.getStatus().getAdmin().toString());
		assertEquals("DOWN", iface.getStatus().getLink().toString());
	}

	@Test
	void testShowIpRouteInOperationalMode() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		router.configureInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)));
		router.addRoute(new StaticRoutingEntry(
			new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)),
			new IPAddress(192, 168, 1, 254)
		));
		router.commitChanges();
		router.setMode(RouterMode.OPERATIONAL);

		String output = router.showIpRoute();

		assertNotNull(output);
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("Codes:"), "Output should contain routing table legend");
		assertTrue(output.contains("192.168.1.0/24"), "Output should contain configured subnet");
		assertTrue(output.contains("10.0.0.0/8"), "Output should contain static route");
	}

	@Test
	void testShowIpRouteInConfigurationModeThrowsException() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);

		RuntimeException exception = assertThrows(RuntimeException.class, router::showIpRoute);
		assertEquals("Invalid command: show [ip]", exception.getMessage());
	}

	@Test
	void testShowIpRouteWithDisabledRouteNotShown() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		router.configureInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)));

		StaticRoutingEntry entry = new StaticRoutingEntry(
			new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)),
			new IPAddress(192, 168, 1, 254)
		);
		router.addRoute(entry);
		router.disableRoute(entry);
		router.commitChanges();
		router.setMode(RouterMode.OPERATIONAL);

		String output = router.showIpRoute();

		// Disabled route should not appear in output
		assertFalse(output.contains("10.0.0.0/8"));
	}

	@Test
	void testShowIpRouteWithDisabledInterfaceNotShown() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		router.configureInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)));
		router.disableInterface("eth0");
		router.commitChanges();
		router.setMode(RouterMode.OPERATIONAL);

		String output = router.showIpRoute();

		// Connected route for disabled interface should not appear
		assertFalse(output.contains("192.168.1.0/24"));
	}

	@Test
	void testShowIpRouteWithInterfaceBasedStaticRoute() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		router.configureInterface("eth0", new InterfaceAddress(new IPAddress(192, 168, 1, 1), new SubnetMask(24)));

		RouterInterface eth0 = router.findFromName("eth0");
		router.addRoute(new StaticRoutingEntry(
			new Subnet(new IPAddress(10, 0, 0, 0), new SubnetMask(8)),
			eth0
		));
		router.commitChanges();
		router.setMode(RouterMode.OPERATIONAL);

		String output = router.showIpRoute();

		assertTrue(output.contains("S>* 10.0.0.0/8"));
		assertTrue(output.contains("via eth0"));
		// Find the line with 10.0.0.0/8 and verify it doesn't say "is directly connected"
		String[] lines = output.split("\n");
		for (String line : lines) {
			if (line.contains("10.0.0.0/8")) {
				assertFalse(line.contains("is directly connected"), "Static route should use 'via' not 'is directly connected'");
				break;
			}
		}
	}

	@Test
	void testCommitChangesCreatesDeepCopy() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);

		StaticRoutingEntry entry = new StaticRoutingEntry(
			new Subnet(new IPAddress(192, 168, 1, 0), new SubnetMask(24)),
			new IPAddress(192, 168, 1, 1)
		);
		router.addRoute(entry);
		router.commitChanges();

		// Modify staged table
		router.removeRoute(entry);

		// Original routing table should still contain the entry
		assertTrue(router.getRoutingTable().contains(entry));
		assertFalse(router.getStagedRoutingTable().contains(entry));
	}
}
