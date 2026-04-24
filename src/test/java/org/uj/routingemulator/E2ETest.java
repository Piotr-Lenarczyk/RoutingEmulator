package org.uj.routingemulator;

import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.RouterCLIParser;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

class E2ETest {
	@Test
	void testNextHopSubnetNotANetworkAddress() {
		Router router = new Router("R1");
		router.setMode(RouterMode.CONFIGURATION);

		// Prepare CLI parser and capture output
		RouterCLIParser parser = new RouterCLIParser();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		CLIContext.setWriter(pw);

		// Execute CLI command that contains mask in next-hop
		parser.executeCommand("set protocols static route 1.1.1.1/8 next-hop 2.2.2.2", router);

		String out = sw.toString();
		StaticRoutingEntry entry = new StaticRoutingEntry(new Subnet(new IPAddress(1, 1, 1, 1), new SubnetMask(8)), new IPAddress(2, 2, 2, 2));
		assertThat(out).contains("Error: 1.1.1.1/8 is not a valid IPv4 prefix");
		assertThat(out).contains("Invalid value");
		assertThat(out).contains("Value validation failed");
		assertThat(out).contains("Set failed");
		assertThat(out).contains("[edit]");
		assertFalse(router.getRoutingTable().contains(entry));
	}

	@Test
	void testNextHopAddressContainsMask() {
		Router router = new Router("R1");
		router.setMode(RouterMode.CONFIGURATION);

		// Prepare CLI parser and capture output
		RouterCLIParser parser = new RouterCLIParser();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		CLIContext.setWriter(pw);

		// Execute CLI command that contains mask in next-hop
		parser.executeCommand("set protocols static route 1.1.1.0/8 next-hop 2.2.2.2/8", router);

		String out = sw.toString();
		assertThat(out).contains("Error: 2.2.2.2/8 is not a valid IPv4 prefix");
		assertThat(out).contains("Invalid value");
		assertThat(out).contains("Value validation failed");
		assertThat(out).contains("Set failed");
		assertThat(out).contains("[edit]");
		CLIContext.clear();
	}
}
