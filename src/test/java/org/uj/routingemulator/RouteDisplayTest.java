package org.uj.routingemulator;

import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.router.InterfaceStatus;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteDisplayTest {
	@Test
	void showIpRouteGroupsEqualCostNextHopsUnderOnePrefix() {
		Router router = new Router("R1");
		router.setMode(RouterMode.CONFIGURATION);
		router.configureInterface("eth0", new InterfaceAddress(
				new IPAddress(192, 0, 2, 1), new SubnetMask(24)));

		Subnet destination = new Subnet(new IPAddress(198, 51, 100, 0), new SubnetMask(24));
		router.addRoute(new StaticRoutingEntry(destination, new IPAddress(192, 0, 2, 2), 10));
		router.addRoute(new StaticRoutingEntry(destination, new IPAddress(192, 0, 2, 3), 10));
		router.commitChanges();
		router.setMode(RouterMode.OPERATIONAL);
		router.findFromName("eth0").setStatus(InterfaceStatus.fromChars('u', 'u'));

		String output = router.showIpRoute();

		assertEquals(1, output.lines()
				.filter(line -> line.contains("198.51.100.0/24"))
				.count());
		assertTrue(output.contains("192.0.2.2"));
		assertTrue(output.contains("192.0.2.3"));
	}
}