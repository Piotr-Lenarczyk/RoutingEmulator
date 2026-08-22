package org.uj.routingemulator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.topology.NetworkTopology;
import org.uj.routingemulator.router.cli.*;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.RouterMode;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;

class RouterCLITest {

	private Router router;
	private CliSession session;
	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;

	@BeforeEach
	void setUp() {
		router = new Router("vyos");
		CommandExecutor executor = new DefaultCommandExecutor(new RouterCLIParser(CommandRegistry.defaultRegistry()));
		CommandExecutionContext context = new CommandExecutionContext(
				router,
				new NetworkTopology(),
				new PrintWriterCommandOutput(new PrintWriter(outputStream, true))
		);
		session = new CliSession(executor, context);
		System.setOut(new PrintStream(outputStream));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
	}

	private String normalizeOutput(String output) {
		return output.replaceAll("\r", "");
	}

	@Test
	void testConfigureCommandFromOperationalMode() {
		session.execute("configure");
		assertEquals(RouterMode.CONFIGURATION, router.getMode());
		assertTrue(outputStream.toString().contains("[edit]"));
	}

	@Test
	void testConfigureCommandFromConfigurationMode() {
		session.execute("configure");
		session.execute("configure");
		assertEquals(RouterMode.CONFIGURATION, router.getMode());
		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: [configure]"));
		assertTrue(output.contains("[edit]"));
	}

	@Test
	void testConfigureCommandWithWhitespace() {
		session.execute("  configure  ");
		assertEquals(RouterMode.CONFIGURATION, router.getMode());
		assertTrue(outputStream.toString().contains("[edit]"));
	}

	@Test
	void testConfigureCommandCaseSensitive() {
		session.execute("Configure");
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertTrue(outputStream.toString().contains("Command not recognized or not supported"));
	}

	@Test
	void testConfigureCommandWithExtraParameters() {
		session.execute("configure something");
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertTrue(outputStream.toString().contains("Command not recognized or not supported"));
	}

	@Test
	void testExitCommandWithoutChanges() {
		session.execute("configure");
		outputStream.reset();
		session.execute("exit");
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertTrue(outputStream.toString().contains("exit"));
	}

	@Test
	void testExitCommandWithUncommittedChanges() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		outputStream.reset();
		session.execute("exit");
		assertEquals(RouterMode.CONFIGURATION, router.getMode());
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("""
				Cannot exit: configuration modified.
				Use 'exit discard' to discard the changes and exit.
				[edit]
				"""));
	}

	@Test
	void testExitDiscardCommandWithUncommittedChanges() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		outputStream.reset();
		session.execute("exit discard");
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertTrue(outputStream.toString().contains("exit"));
	}

	@Test
	void testCommitCommandWithChanges() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		outputStream.reset();
		session.execute("commit");
		assertFalse(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
	}

	@Test
	void testCommitCommandWithoutChanges() {
		session.execute("configure");
		outputStream.reset();
		session.execute("commit");
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("No configuration changes to commit"));
		assertTrue(output.contains("[edit]"));
	}

	@Test
	void testCommitCommandInOperationalMode() {
		outputStream.reset();
		session.execute("commit");
		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: [commit]"));
	}

	@Test
	void testSetRouteNextHop() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 10.0.0.1/24");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1");
		assertTrue(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(1, router.getConfigSession().getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testSetRouteNextHopWithDistance() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 10.0.0.1/24");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 distance 50");
		assertTrue(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(1, router.getConfigSession().getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testSetRouteInterface() {
		session.execute("configure");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 interface eth0");
		assertTrue(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(1, router.getConfigSession().getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testSetRouteInterfaceWithDistance() {
		session.execute("configure");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 interface eth0 distance 100");
		assertTrue(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(1, router.getConfigSession().getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testSetDuplicateRouteShowsError() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1");
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Configuration path:"));
		assertTrue(output.contains("already exists"));
		assertFalse(output.contains("[edit]\n[edit]"));
	}

	@Test
	void testDeleteRouteNextHop() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1");
		outputStream.reset();
		session.execute("delete protocols static route 192.168.1.0/24 next-hop 10.0.0.1");
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(0, router.getConfigSession().getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testDeleteRouteNextHopWithDistance() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 distance 50");
		outputStream.reset();
		session.execute("delete protocols static route 192.168.1.0/24 next-hop 10.0.0.1 distance 50");
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(0, router.getConfigSession().getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testDeleteNonExistentRouteShowsError() {
		session.execute("configure");
		outputStream.reset();
		session.execute("delete protocols static route 192.168.1.0/24 next-hop 10.0.0.1");
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Nothing to delete"));
		assertFalse(output.endsWith("[edit]\n"));
	}

	@Test
	void testDisableRouteNextHop() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 disable");
		assertTrue(outputStream.toString().contains("[edit]"));
		assertTrue(router.getConfigSession().getStagedRoutingTable().getRoutingEntries().getFirst().isDisabled());
	}

	@Test
	void testDisableRouteInterface() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 interface eth0");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 interface eth0 disable");
		assertTrue(outputStream.toString().contains("[edit]"));
		assertTrue(router.getConfigSession().getStagedRoutingTable().getRoutingEntries().getFirst().isDisabled());
	}

	@Test
	void testDisableAlreadyDisabledRouteShowsError() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1");
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 disable");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 disable");
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("already exists"));
		assertFalse(output.endsWith("[edit]\n"));
	}

	@Test
	void testSetInterfaceAddress() {
		session.execute("configure");
		outputStream.reset();
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		assertTrue(router.hasUncommittedChanges());
		assertTrue(outputStream.toString().contains("[edit]"));
		assertNotNull(router.getConfigSession().getStagedInterfaces().getFirst().getSubnet());
	}

	@Test
	void testSetInterfaceNetworkAddressShowsError() {
		session.execute("configure");
		outputStream.reset();
		session.execute("set interfaces ethernet eth0 address 192.168.1.0/24");
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("network address") || output.contains("Network addresses"), "Should contain error about network address");
		assertTrue(output.contains("host address") || output.contains("Use a host address"), "Should suggest using a host address");
		assertFalse(output.endsWith("[edit]\n"), "Should not show [edit] on error");
	}

	@Test
	void testSetInterfaceBroadcastAddressShowsError() {
		session.execute("configure");
		outputStream.reset();
		session.execute("set interfaces ethernet eth0 address 192.168.1.255/24");
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("broadcast address") || output.contains("broadcast"), "Should contain error about broadcast address");
		assertTrue(output.contains("host address") || output.contains("Use a host address"), "Should suggest using a host address");
		assertFalse(output.endsWith("[edit]\n"), "Should not show [edit] on error");
	}

	@Test
	void testSetDuplicateInterfaceAddressShowsError() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		outputStream.reset();
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Configuration path:"));
		assertTrue(output.contains("already exists"));
		assertFalse(output.endsWith("[edit]\n"));
	}

	@Test
	void testDeleteInterfaceAddress() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		outputStream.reset();
		session.execute("delete interfaces ethernet eth0 address 192.168.1.1/24");
		assertTrue(outputStream.toString().contains("[edit]"));
		assertNull(router.getConfigSession().getStagedInterfaces().getFirst().getSubnet());
	}

	@Test
	void testDeleteNonExistentInterfaceAddressShowsError() {
		session.execute("configure");
		outputStream.reset();
		session.execute("delete interfaces ethernet eth0 address 192.168.1.1/24");
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Nothing to delete"));
		assertFalse(output.endsWith("[edit]\n"));
	}

	@Test
	void testDisableInterface() {
		session.execute("configure");
		outputStream.reset();
		session.execute("set interfaces ethernet eth0 disable");
		assertTrue(outputStream.toString().contains("[edit]"));
		assertTrue(router.hasUncommittedChanges());
	}

	@Test
	void testSetRouteInOperationalModeShowsError() {
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1");
		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: set [protocols]"));
	}

	@Test
	void testSetInterfaceInOperationalModeShowsError() {
		outputStream.reset();
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: set [interfaces]"));
	}

	@Test
	void testFullWorkflowWithCommit() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1");
		session.execute("set interfaces ethernet eth0 address 10.0.0.2/24");
		assertTrue(router.hasUncommittedChanges());
		assertEquals(1, router.getConfigSession().getStagedRoutingTable().getRoutingEntries().size());
		assertNotNull(router.getConfigSession().getStagedInterfaces().getFirst().getSubnet());

		session.execute("commit");
		assertFalse(router.hasUncommittedChanges());
		assertEquals(1, router.getRoutingTable().getRoutingEntries().size());
		assertNotNull(router.getInterfaces().getFirst().getSubnet());

		session.execute("exit");
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
	}

	@Test
	void testFullWorkflowWithDiscard() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1");
		session.execute("set interfaces ethernet eth0 address 10.0.0.2/24");
		assertTrue(router.hasUncommittedChanges());

		session.execute("exit discard");
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertFalse(router.hasUncommittedChanges());
		assertEquals(0, router.getRoutingTable().getRoutingEntries().size());
		assertNull(router.getInterfaces().getFirst().getSubnet());
	}

	@Test
	void testUnrecognizedCommandShowsError() {
		outputStream.reset();
		session.execute("invalid command");
		assertTrue(outputStream.toString().contains("Command not recognized or not supported"));
	}

	@Test
	void testDuplicateRouteAndInterfaceAfterCommitAndReconfigure() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 interface eth0");
		session.execute("set interfaces ethernet eth0 address 192.168.1.254/24");
		session.execute("commit");
		session.execute("exit");

		session.execute("configure");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 interface eth0");
		String output = normalizeOutput(outputStream.toString());
		assertTrue(output.contains("Configuration path:"));
		assertTrue(output.contains("already exists"));

		outputStream.reset();
		session.execute("set interfaces ethernet eth0 address 192.168.1.254/24");
		output = normalizeOutput(outputStream.toString());
		assertFalse(output.isEmpty(), "Should produce output");
	}

	@Test
	void testShowIpRouteInOperationalMode() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		session.execute("set protocols static route 10.0.0.0/8 next-hop 192.168.1.254");
		session.execute("commit");
		session.execute("exit");
		outputStream.reset();
		session.execute("show ip route");
		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("Codes:"), "Output should contain routing table legend");
	}

	@Test
	void testShowIpRouteInConfigurationModeFails() {
		session.execute("configure");
		outputStream.reset();
		session.execute("show ip route");
		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: show [ip]"));
	}

	@Test
	void testShowIpRouteWithDisabledRoute() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		session.execute("set protocols static route 10.0.0.0/8 next-hop 192.168.1.254");
		session.execute("set protocols static route 10.0.0.0/8 next-hop 192.168.1.254 disable");
		session.execute("commit");
		session.execute("exit");
		outputStream.reset();
		session.execute("show ip route");
		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("192.168.1.0/24"), "Output should contain connected route");
		assertFalse(output.contains("10.0.0.0/8"), "Disabled route should not appear in routing table");
	}

	@Test
	void testShowIpRouteWithInterfaceBasedRoute() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		session.execute("set protocols static route 10.0.0.0/8 interface eth0");
		session.execute("commit");
		session.execute("exit");
		outputStream.reset();
		session.execute("show ip route");
		String output = outputStream.toString();
		assertTrue(output.contains("S>* 10.0.0.0/8"));
		assertTrue(output.contains("via eth0"));
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
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 interface eth0");
		outputStream.reset();
		session.execute("delete protocols static route 192.168.1.0/24 interface eth0");
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(0, router.getConfigSession().getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testDeleteRouteInterfaceWithDistance() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 interface eth0 distance 100");
		outputStream.reset();
		session.execute("delete protocols static route 192.168.1.0/24 interface eth0 distance 100");
		assertTrue(outputStream.toString().contains("[edit]"));
		assertEquals(0, router.getConfigSession().getStagedRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testDisableRouteInterfaceWithDistance() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 interface eth0 distance 100");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 interface eth0 distance 100 disable");
		assertTrue(outputStream.toString().contains("[edit]"));
		assertTrue(router.getConfigSession().getStagedRoutingTable().getRoutingEntries().getFirst().isDisabled());
	}

	@Test
	void testDisableRouteNextHopWithDistance() {
		session.execute("configure");
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 distance 50");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 distance 50 disable");
		assertTrue(outputStream.toString().contains("[edit]"));
		assertTrue(router.getConfigSession().getStagedRoutingTable().getRoutingEntries().getFirst().isDisabled());
	}

	@Test
	void testDisableNonExistentRouteShowsError() {
		session.execute("configure");
		outputStream.reset();
		session.execute("set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 disable");
		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Should produce some output");
		assertTrue(output.contains("Configuration path:") && output.contains("does not exist"), "Should contain error message about route not found");
	}

	@Test
	void testConfigureInterfaceWithInvalidIPShowsError() {
		session.execute("configure");
		outputStream.reset();
		session.execute("set interfaces ethernet eth0 address 999.999.999.999/24");
		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Should produce some output");
		assertTrue(output.contains("Octet value must be"), "Should reject invalid octet");
	}

	@Test
	void testShowConfigurationInOperationalMode() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		session.execute("set protocols static route 10.0.0.0/8 next-hop 192.168.1.254");
		session.execute("commit");
		session.execute("exit");
		outputStream.reset();
		session.execute("show configuration");
		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("interfaces {"), "Should contain interfaces block");
		assertTrue(output.contains("ethernet eth0 {"), "Should contain eth0 configuration");
		assertTrue(output.contains("address 192.168.1.1/24"), "Should contain IP address");
		assertTrue(output.contains("protocols {"), "Should contain protocols block");
		assertTrue(output.contains("route 10.0.0.0/8"), "Should contain static route");

		String[] lines = output.split("\n");
		boolean foundEth0Block = false;
		boolean foundDisableInEth0 = false;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains("ethernet eth0 {")) {
				foundEth0Block = true;
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
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		session.execute("commit");
		outputStream.reset();
		session.execute("show configuration");
		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("interfaces {"), "Should show configuration even in config mode");
	}

	@Test
	void testShowConfigurationWithNoConfig() {
		outputStream.reset();
		session.execute("show configuration");
		String output = outputStream.toString();
		assertFalse(output.isEmpty(), "Output should not be empty");
		assertTrue(output.contains("/* No configuration */"), "Should show no configuration message");
	}

	@Test
	void testShowInterfacesInOperationalMode() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		session.execute("commit");
		session.execute("exit");
		outputStream.reset();
		session.execute("show interfaces");
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
		assertTrue(output.matches("(?s).*[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}.*"), "Should contain MAC address");
	}

	@Test
	void testShowInterfacesInConfigurationModeFails() {
		session.execute("configure");
		outputStream.reset();
		session.execute("show interfaces");
		String output = outputStream.toString();
		assertTrue(output.contains("Invalid command: show [interfaces]"));
	}

	@Test
	void testShowInterfacesWithDisabledInterface() {
		session.execute("configure");
		session.execute("set interfaces ethernet eth0 disable");
		session.execute("commit");
		session.execute("exit");
		outputStream.reset();
		session.execute("show interfaces");
		String output = outputStream.toString();
		assertTrue(output.contains("eth0"), "Should list eth0");
		assertTrue(output.contains("A/D") || output.contains("A/u"), "Should show admin down status");
	}
}