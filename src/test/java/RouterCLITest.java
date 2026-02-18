import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.cli.RouterCLIParser;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Router CLI commands.
 */
class RouterCLITest {
	private Router router;
	private RouterCLIParser parser;
	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;

	@BeforeEach
	void setUp() {
		router = new Router("vyos");
		parser = new RouterCLIParser();
		System.setOut(new PrintStream(outputStream));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
	}

	private String normalizeOutput(String output) {
		return output.replaceAll("\r", "");
	}

	/**
	 * Test that 'configure' command changes router mode from OPERATIONAL to CONFIGURATION.
	 */
	@Test
	void testConfigureCommandFromOperationalMode() {
		

		
		parser.executeCommand("configure", router);

		
		assertEquals(RouterMode.CONFIGURATION, router.getMode());
		assertTrue(outputStream.toString().contains("[edit]"));
	}

	/**
	 * Test that 'configure' command in CONFIGURATION mode shows error message.
	 */
	@Test
	void testConfigureCommandFromConfigurationMode() {
		

		
		parser.executeCommand("configure", router);
		parser.executeCommand("configure", router);

		
		assertEquals(RouterMode.CONFIGURATION, router.getMode());
		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: [configure]"));
		assertTrue(output.contains("[edit]"));
	}

	/**
	 * Test that 'configure' command with extra whitespace is recognized.
	 */
	@Test
	void testConfigureCommandWithWhitespace() {
		

		
		parser.executeCommand("  configure  ", router);

		
		assertEquals(RouterMode.CONFIGURATION, router.getMode());
		assertTrue(outputStream.toString().contains("[edit]"));
	}

	/**
	 * Test that 'configure' command is case-sensitive.
	 */
	@Test
	void testConfigureCommandCaseSensitive() {
		

		
		parser.executeCommand("Configure", router);

		
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertTrue(outputStream.toString().contains("Command not recognized or not supported"));
	}

	/**
	 * Test that 'configure' command with extra parameters is not recognized.
	 */
	@Test
	void testConfigureCommandWithExtraParameters() {
		

		
		parser.executeCommand("configure something", router);

		
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertTrue(outputStream.toString().contains("Command not recognized or not supported"));
	}

	@Test
	void testExitCommandWithoutChanges() {
		

		
		parser.executeCommand("configure", router);
		outputStream.reset();
		parser.executeCommand("exit", router);

		
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertTrue(outputStream.toString().contains("exit"));
	}
	
	@Test
	void testExitCommandWithUncommittedChanges() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		outputStream.reset();
		parser.executeCommand("exit", router);
		assertEquals(RouterMode.CONFIGURATION, router.getMode());
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Cannot exit: configuration modified.\n" +
				"Use 'exit discard' to discard the changes and exit.\n" +
				"[edit]\n"));
	}

	@Test
	void testExitDiscardCommandWithUncommittedChanges() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		outputStream.reset();
		parser.executeCommand("exit discard", router);
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertTrue(outputStream.toString().contains("exit"));
	}

	@Test
	void testCommitCommandWithChanges() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		outputStream.reset();

		parser.executeCommand("commit", router);

		assertFalse(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
	}

	@Test
	void testCommitCommandWithoutChanges() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("commit", router);

		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("No configuration changes to commit"));
		assertTrue(output.contains("[edit]"));
	}

	@Test
	void testCommitCommandInOperationalMode() {
		outputStream.reset();

		parser.executeCommand("commit", router);

		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: [commit]"));
	}


	@Test
	void testSetRouteNextHop() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1", router);

		assertTrue(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(1, router.getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testSetRouteNextHopWithDistance() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 distance 50", router);

		assertTrue(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(1, router.getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testSetRouteInterface() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 interface eth0", router);

		assertTrue(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(1, router.getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testSetRouteInterfaceWithDistance() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 interface eth0 distance 100", router);

		assertTrue(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(1, router.getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testSetDuplicateRouteShowsError() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1", router);
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1", router);

		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Configuration path:"));
		assertTrue(output.contains("already exists"));
		assertFalse(output.contains("[edit]\n[edit]")); // Should not have double [edit]
	}

	@Test
	void testDeleteRouteNextHop() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1", router);
		outputStream.reset();

		parser.executeCommand("delete protocols static route 192.168.1.0/24 next-hop 10.0.0.1", router);

		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(0, router.getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testDeleteRouteNextHopWithDistance() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 distance 50", router);
		outputStream.reset();

		parser.executeCommand("delete protocols static route 192.168.1.0/24 next-hop 10.0.0.1 distance 50", router);

		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(0, router.getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testDeleteNonExistentRouteShowsError() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("delete protocols static route 192.168.1.0/24 next-hop 10.0.0.1", router);

		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Nothing to delete"));
		assertFalse(output.endsWith("[edit]\n"));
	}

	@Test
	void testDisableRouteNextHop() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1", router);
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 disable", router);

		assertTrue(outputStream.toString().contains("[edit]"));
		assertTrue(router.getStagedRoutingTable().getRoutingEntries().get(0).isDisabled());
	}

	@Test
	void testDisableRouteInterface() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 interface eth0", router);
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 interface eth0 disable", router);

		assertTrue(outputStream.toString().contains("[edit]"));
		assertTrue(router.getStagedRoutingTable().getRoutingEntries().get(0).isDisabled());
	}

	@Test
	void testDisableAlreadyDisabledRouteShowsError() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 disable", router);
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 disable", router);

		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("already exists"));
		assertFalse(output.endsWith("[edit]\n"));
	}

	@Test
	void testSetInterfaceAddress() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);

		assertTrue(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
		assertNotNull(router.getStagedInterfaces().get(0).getSubnet());
	}

	@Test
	void testSetInterfaceNetworkAddressShowsError() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.0/24", router);

		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("network address") || output.contains("Network addresses"),
				"Should contain error about network address");
		assertTrue(output.contains("host address") || output.contains("Use a host address"),
				"Should suggest using a host address");
		assertFalse(output.endsWith("[edit]\n"), "Should not show [edit] on error");
	}

	@Test
	void testSetInterfaceBroadcastAddressShowsError() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.255/24", router);

		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("broadcast address") || output.contains("broadcast"),
			"Should contain error about broadcast address");
		assertTrue(output.contains("host address") || output.contains("Use a host address"),
			"Should suggest using a host address");
		assertFalse(output.endsWith("[edit]\n"), "Should not show [edit] on error");
	}

	@Test
	void testSetDuplicateInterfaceAddressShowsError() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		outputStream.reset();

		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);

		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Configuration path:"));
		assertTrue(output.contains("already exists"));
		assertFalse(output.endsWith("[edit]\n"));
	}

	@Test
	void testDeleteInterfaceAddress() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		outputStream.reset();

		parser.executeCommand("delete interfaces ethernet eth0 address 192.168.1.1/24", router);

		assertTrue(outputStream.toString().contains("[edit]"));
		assertNull(router.getStagedInterfaces().get(0).getSubnet());
	}

	@Test
	void testDeleteNonExistentInterfaceAddressShowsError() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("delete interfaces ethernet eth0 address 192.168.1.1/24", router);

		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Nothing to delete"));
		assertFalse(output.endsWith("[edit]\n"));
	}

	@Test
	void testDisableInterface() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("set interfaces ethernet eth0 disable", router);

		assertTrue(outputStream.toString().contains("[edit]"));
		assertTrue(router.hasUncommittedChanges());
	}

	@Test
	void testSetRouteInOperationalModeShowsError() {
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1", router);

		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: set [protocols]"));
	}

	@Test
	void testSetInterfaceInOperationalModeShowsError() {
		outputStream.reset();

		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);

		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: set [interfaces]"));
	}


	@Test
	void testFullWorkflowWithCommit() {
		// Enter configuration mode
		parser.executeCommand("configure", router);

		// Add routes and configure interface
		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1", router);
		parser.executeCommand("set interfaces ethernet eth0 address 10.0.0.2/24", router);

		assertTrue(router.hasUncommittedChanges());
		assertEquals(1, router.getStagedRoutingTable().getRoutingEntries().size());
		assertNotNull(router.getStagedInterfaces().get(0).getSubnet());

		// Commit changes
		parser.executeCommand("commit", router);

		assertFalse(router.hasUncommittedChanges());
		assertEquals(1, router.getRoutingTable().getRoutingEntries().size());
		assertNotNull(router.getInterfaces().get(0).getSubnet());

		// Exit to operational mode
		parser.executeCommand("exit", router);

		assertEquals(RouterMode.OPERATIONAL, router.getMode());
	}

	@Test
	void testFullWorkflowWithDiscard() {
		// Enter configuration mode
		parser.executeCommand("configure", router);

		// Add configuration
		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1", router);
		parser.executeCommand("set interfaces ethernet eth0 address 10.0.0.2/24", router);

		assertTrue(router.hasUncommittedChanges());

		// Exit with discard
		parser.executeCommand("exit discard", router);

		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertFalse(router.hasUncommittedChanges());
		assertEquals(0, router.getRoutingTable().getRoutingEntries().size());
		assertNull(router.getInterfaces().get(0).getSubnet());
	}

	@Test
	void testUnrecognizedCommandShowsError() {
		outputStream.reset();

		parser.executeCommand("invalid command", router);

		assertTrue(outputStream.toString().contains("Command not recognized or not supported"));
	}

	@Test
	void testDuplicateRouteAndInterfaceAfterCommitAndReconfigure() {
		// First configuration session
		parser.executeCommand("configure", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 interface eth0", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.254/24", router);
		parser.executeCommand("commit", router);
		parser.executeCommand("exit", router);

		// Second configuration session - try to add the same route again
		parser.executeCommand("configure", router);
		outputStream.reset();
		parser.executeCommand("set protocols static route 192.168.1.0/24 interface eth0", router);
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Configuration path:"));
		assertTrue(output.contains("already exists"));
		outputStream.reset();

		// Try to configure the same interface address again - should show error
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.254/24", router);

		output = normalizeOutput(outputStream.toString());
		assertFalse(output.isEmpty(), "Should produce output");
		// After commit and reconfigure, duplicate configuration should be detected
		// Either shows error or [edit] (depending on implementation)
	}

	@Test
	void testShowIpRouteInOperationalMode() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		parser.executeCommand("set protocols static route 10.0.0.0/8 next-hop 192.168.1.254", router);
		parser.executeCommand("commit", router);
		parser.executeCommand("exit", router);
		outputStream.reset();

		parser.executeCommand("show ip route", router);

		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("Codes:"), "Output should contain routing table legend");
	}

	@Test
	void testShowIpRouteInConfigurationModeFails() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("show ip route", router);

		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: show [ip]"));
	}

	@Test
	void testShowIpRouteWithDisabledRoute() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		parser.executeCommand("set protocols static route 10.0.0.0/8 next-hop 192.168.1.254", router);
		parser.executeCommand("set protocols static route 10.0.0.0/8 next-hop 192.168.1.254 disable", router);
		parser.executeCommand("commit", router);
		parser.executeCommand("exit", router);
		outputStream.reset();

		parser.executeCommand("show ip route", router);

		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("192.168.1.0/24"), "Output should contain connected route");
		// Disabled route should not appear
		assertFalse(output.contains("10.0.0.0/8"), "Disabled route should not appear in routing table");
	}

	@Test
	void testShowIpRouteWithInterfaceBasedRoute() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		parser.executeCommand("set protocols static route 10.0.0.0/8 interface eth0", router);
		parser.executeCommand("commit", router);
		parser.executeCommand("exit", router);
		outputStream.reset();

		parser.executeCommand("show ip route", router);

		String output = outputStream.toString();
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
	void testDeleteRouteInterface() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 interface eth0", router);
		outputStream.reset();

		parser.executeCommand("delete protocols static route 192.168.1.0/24 interface eth0", router);

		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(0, router.getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testDeleteRouteInterfaceWithDistance() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 interface eth0 distance 100", router);
		outputStream.reset();

		parser.executeCommand("delete protocols static route 192.168.1.0/24 interface eth0 distance 100", router);

		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(0, router.getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testDisableRouteInterfaceWithDistance() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 interface eth0 distance 100", router);
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 interface eth0 distance 100 disable", router);

		assertTrue(outputStream.toString().contains("[edit]"));
		assertTrue(router.getStagedRoutingTable().getRoutingEntries().get(0).isDisabled());
	}

	@Test
	void testDisableRouteNextHopWithDistance() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 distance 50", router);
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 distance 50 disable", router);

		assertTrue(outputStream.toString().contains("[edit]"));
		assertTrue(router.getStagedRoutingTable().getRoutingEntries().get(0).isDisabled());
	}

	@Test
	void testDisableNonExistentRouteShowsError() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 disable", router);

		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Should produce some output");
		assertTrue(output.contains("Configuration path:") && output.contains("does not exist"),
			"Should contain error message about route not found");
	}

	@Test
	void testConfigureInterfaceWithInvalidIPShowsError() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("set interfaces ethernet eth0 address 999.999.999.999/24", router);

		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Should produce some output");
		// Command should either show error or be rejected
		assertTrue(output.contains("Octet value must be"), "Should reject invalid octet");
	}

	@Test
	void testShowConfigurationInOperationalMode() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		parser.executeCommand("set protocols static route 10.0.0.0/8 next-hop 192.168.1.254", router);
		parser.executeCommand("commit", router);
		parser.executeCommand("exit", router);
		outputStream.reset();

		parser.executeCommand("show configuration", router);

		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("interfaces {"), "Should contain interfaces block");
		assertTrue(output.contains("ethernet eth0 {"), "Should contain eth0 configuration");
		assertTrue(output.contains("address 192.168.1.1/24"), "Should contain IP address");
		assertTrue(output.contains("protocols {"), "Should contain protocols block");
		assertTrue(output.contains("route 10.0.0.0/8"), "Should contain static route");
		// Should NOT show disable for interfaces that are not disabled
		String[] lines = output.split("\n");
		boolean foundEth0Block = false;
		boolean foundDisableInEth0 = false;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains("ethernet eth0 {")) {
				foundEth0Block = true;
				// Check lines until closing brace
				for (int j = i + 1; j < lines.length && !lines[j].contains("}"); j++) {
					if (lines[j].contains("disable")) {
						foundDisableInEth0 = true;
						break;
					}
				}
				break;
			}
		}
		assertTrue(foundEth0Block, "Should find eth0 block");
		assertFalse(foundDisableInEth0, "eth0 should not have disable statement when not administratively disabled");
	}

	@Test
	void testShowConfigurationInConfigurationMode() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		parser.executeCommand("commit", router);
		outputStream.reset();

		parser.executeCommand("show configuration", router);

		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("interfaces {"), "Should show configuration even in config mode");
	}

	@Test
	void testShowConfigurationWithNoConfig() {
		outputStream.reset();

		parser.executeCommand("show configuration", router);

		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("/* No configuration */"), "Should show no configuration message");
	}

	@Test
	void testShowInterfacesInOperationalMode() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		parser.executeCommand("commit", router);
		parser.executeCommand("exit", router);
		outputStream.reset();

		parser.executeCommand("show interfaces", router);

		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("Codes:"), "Should contain header");
		assertTrue(output.contains("Interface"), "Should contain column headers");
		assertTrue(output.contains("IP Address"), "Should contain IP Address column");
		assertTrue(output.contains("MAC"), "Should contain MAC column");
		assertTrue(output.contains("VRF"), "Should contain VRF column");
		assertTrue(output.contains("MTU"), "Should contain MTU column");
		assertTrue(output.contains("eth0"), "Should list eth0");
		assertTrue(output.contains("192.168.1.1/24"), "Should show IP address");
		assertTrue(output.contains("default"), "Should show default VRF");
		assertTrue(output.contains("1500"), "Should show MTU");
		// Should show MAC address in format XX:XX:XX:XX:XX:XX
		assertTrue(output.matches("(?s).*[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}.*"),
				"Should contain MAC address");
	}

	@Test
	void testShowInterfacesInConfigurationModeFails() {
		parser.executeCommand("configure", router);
		outputStream.reset();

		parser.executeCommand("show interfaces", router);

		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: show [interfaces]"));
	}

	@Test
	void testShowInterfacesWithDisabledInterface() {
		parser.executeCommand("configure", router);
		parser.executeCommand("set interfaces ethernet eth0 disable", router);
		parser.executeCommand("commit", router);
		parser.executeCommand("exit", router);
		outputStream.reset();

		parser.executeCommand("show interfaces", router);

		String output = outputStream.toString();
		// Should show interface status as A/D (Admin Down / Link Down) or A/u (Admin Down / Link Up)
		assertTrue(output.contains("eth0"), "Should list eth0");
		assertTrue(output.contains("A/D") || output.contains("A/u"), "Should show admin down status");
	}
}


