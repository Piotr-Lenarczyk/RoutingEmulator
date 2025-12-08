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
		assertTrue(output.contains("Error:"));
		assertTrue(output.contains("is not a valid host IP"));
		assertTrue(output.contains("Set failed"));
		assertFalse(output.endsWith("[edit]\n"));
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
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.255/24", router);
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
		parser.executeCommand("set interfaces ethernet eth0 address 192.168.1.255/24", router);

		output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Configuration path:"));
		assertTrue(output.contains("already exists"));
	}

}
