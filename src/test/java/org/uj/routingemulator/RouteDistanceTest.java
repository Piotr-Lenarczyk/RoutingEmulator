package org.uj.routingemulator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.RouterCLIParser;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteDistanceTest {
	private Router router;
	private RouterCLIParser parser;

	@BeforeEach
	void setUp() {
		router = new Router("distance-test");
		parser = new RouterCLIParser();
		CLIContext.setWriter(new PrintWriter(new ByteArrayOutputStream(), true));
		router.setMode(RouterMode.CONFIGURATION);
	}

	@AfterEach
	void clearCliContext() {
		CLIContext.clear();
	}

	@Test
	void settingDistanceAgainUpdatesOnlyTheMatchingNextHopPath() {
		parser.executeCommand("set protocols static route 198.51.100.0/24 next-hop 192.0.2.2 distance 50", router);
		parser.executeCommand("set protocols static route 198.51.100.0/24 next-hop 192.0.2.3 distance 75", router);

		parser.executeCommand("set protocols static route 198.51.100.0/24 next-hop 192.0.2.2 distance 20", router);

		assertEquals(2, router.getStagedRoutingTable().getRoutingEntries().size());
		assertTrue(router.getStagedRoutingTable().getRoutingEntries().stream()
				.anyMatch(route -> route.getNextHop().toString().equals("192.0.2.2")
						&& route.getAdministrativeDistance() == 20));
		assertTrue(router.getStagedRoutingTable().getRoutingEntries().stream()
				.anyMatch(route -> route.getNextHop().toString().equals("192.0.2.3")
						&& route.getAdministrativeDistance() == 75));
		assertEquals(1, router.getStagedRoutingTable().getRoutingEntries().stream()
				.filter(route -> route.getNextHop().toString().equals("192.0.2.2")).count());
	}

	@Test
	void settingDistanceAgainUpdatesTheMatchingInterfacePath() {
		parser.executeCommand("set protocols static route 198.51.100.0/24 interface eth0 distance 50", router);

		parser.executeCommand("set protocols static route 198.51.100.0/24 interface eth0 distance 20", router);

		assertEquals(1, router.getStagedRoutingTable().getRoutingEntries().size());
		assertEquals(20, router.getStagedRoutingTable().getRoutingEntries().getFirst().getAdministrativeDistance());
	}

	@Test
	void distanceUpdateIsStagedAndCanBeDiscarded() {
		parser.executeCommand("set protocols static route 198.51.100.0/24 next-hop 192.0.2.2 distance 50", router);
		router.commitChanges();

		parser.executeCommand("set protocols static route 198.51.100.0/24 next-hop 192.0.2.2 distance 20", router);
		assertEquals(50, router.getRoutingTable().getRoutingEntries().getFirst().getAdministrativeDistance());
		assertEquals(20, router.getStagedRoutingTable().getRoutingEntries().getFirst().getAdministrativeDistance());
		router.commitChanges();
		assertEquals(20, router.getRoutingTable().getRoutingEntries().getFirst().getAdministrativeDistance());

		parser.executeCommand("set protocols static route 198.51.100.0/24 next-hop 192.0.2.2 distance 30", router);

		router.discardChanges();

		assertEquals(20, router.getStagedRoutingTable().getRoutingEntries().getFirst().getAdministrativeDistance());
		assertEquals(1, router.getStagedRoutingTable().getRoutingEntries().size());
	}
}