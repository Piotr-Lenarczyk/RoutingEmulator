package org.uj.routingemulator;

import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.NetworkTopology;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.cli.*;
import org.uj.routingemulator.router.config.*;

import java.io.PrintWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {

	private static Router getConfiguration() {
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		CliSession session = new CliSession(new DefaultCommandExecutor(new RouterCLIParser(CommandRegistry.defaultRegistry())),
				new CommandExecutionContext(router, new NetworkTopology(), new PrintWriterCommandOutput(new PrintWriter(System.out))));

		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.254/24");
		session.execute("set interfaces ethernet eth1 address 192.168.2.1/24");
		session.execute("set protocols static route 192.168.3.0/24 next-hop 192.168.1.254");
		session.execute("set protocols static route 10.0.0.0/8 interface eth1 distance 5");
		session.execute("commit");

		return router;
	}

	private static String getString() {
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		CliSession session = new CliSession(new DefaultCommandExecutor(new RouterCLIParser(CommandRegistry.defaultRegistry())),
				new CommandExecutionContext(router, new NetworkTopology(), new PrintWriterCommandOutput(new PrintWriter(System.out))));

		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		session.execute("set interfaces ethernet eth1 address 192.168.2.1/24");
		session.execute("set protocols static route 10.0.0.0/8 interface eth1");
		session.execute("commit");

		ConfigurationGenerator generator = ConfigurationFactory.getHierarchicalGenerator();
		return generator.generateConfiguration(router);
	}

	@Test
	void testConfigurationWithDisabledInterface() {
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		CliSession session = new CliSession(new DefaultCommandExecutor(new RouterCLIParser(CommandRegistry.defaultRegistry())),
				new CommandExecutionContext(router, new NetworkTopology(), new PrintWriterCommandOutput(new PrintWriter(System.out))));

		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		session.execute("set interfaces ethernet eth0 disable");
		session.execute("commit");

		ConfigurationGenerator generator = new CommandConfigurationGenerator();
		String config = generator.generateConfiguration(router);

		Router newRouter = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		ConfigurationParser parser = new CommandConfigurationParser();
		parser.loadConfiguration(newRouter, config);

		assertTrue(newRouter.findFromName("eth0").isDisabled());
	}

	@Test
	void testConfigurationWithCommentsAndEmptyLines() {
		String config = """
				# Interface configuration
				set interfaces ethernet eth0 address 192.168.1.254/24
				
				# Route configuration
				set protocols static route 192.168.2.0/24 next-hop 192.168.1.254
				""";

		Router router = new Router("R1", List.of(new RouterInterface("eth0")));
		ConfigurationParser parser = new CommandConfigurationParser();
		parser.loadConfiguration(router, config);

		assertNotNull(router.findFromName("eth0").getInterfaceAddress());
		assertEquals(1, router.getRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testConfigurationSaveAndLoad() {
		Router router = getConfiguration();

		ConfigurationGenerator generator = new CommandConfigurationGenerator();
		String config = generator.generateConfiguration(router);

		System.out.println("=== Generated Configuration ===");
		System.out.println(config);

		Router newRouter = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		ConfigurationParser parser = new CommandConfigurationParser();
		parser.loadConfiguration(newRouter, config);

		String verifyConfig = generator.generateConfiguration(newRouter);
		System.out.println("\n=== Verification - New Router Configuration ===");
		System.out.println(verifyConfig);

		assertEquals(config, verifyConfig, "Configurations should match");
		assertEquals(2, newRouter.getInterfaces().size());
		assertNotNull(newRouter.findFromName("eth0").getInterfaceAddress());
		assertNotNull(newRouter.findFromName("eth1").getInterfaceAddress());
		assertEquals(2, newRouter.getRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testInvalidConfiguration() {
		String invalidConfig = "set protocols static route 192.168.1.0/24 invalid-option value";
		Router router = new Router("R1", List.of(new RouterInterface("eth0")));
		ConfigurationParser parser = new CommandConfigurationParser();

		assertThrows(ConfigurationParseException.class, () -> parser.loadConfiguration(router, invalidConfig));
	}

	@Test
	void testOverwriteExistingConfiguration() {
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		CliSession session = new CliSession(new DefaultCommandExecutor(new RouterCLIParser(CommandRegistry.defaultRegistry())),
				new CommandExecutionContext(router, new NetworkTopology(), new PrintWriterCommandOutput(new PrintWriter(System.out))));

		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.254/24");
		session.execute("set interfaces ethernet eth1 address 192.168.2.1/24");
		session.execute("set protocols static route 10.0.0.0/8 next-hop 192.168.1.254");
		session.execute("commit");

		ConfigurationGenerator generator = new CommandConfigurationGenerator();
		String savedConfig = generator.generateConfiguration(router);

		System.out.println("=== Initial Configuration ===");
		System.out.println(savedConfig);

		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 10.10.10.254/24");
		session.execute("set protocols static route 172.16.0.0/16 next-hop 10.10.10.254");
		session.execute("commit");

		String modifiedConfig = generator.generateConfiguration(router);
		System.out.println("\n=== Modified Configuration ===");
		System.out.println(modifiedConfig);

		ConfigurationParser parser = new CommandConfigurationParser();
		parser.loadConfiguration(router, savedConfig);

		String restoredConfig = generator.generateConfiguration(router);
		System.out.println("\n=== Restored Configuration ===");
		System.out.println(restoredConfig);

		assertEquals(savedConfig, restoredConfig, "Configuration should be restored to original");
		assertEquals("192.168.1.254", router.findFromName("eth0").getInterfaceAddress().ipAddress().toString());
		assertEquals("192.168.2.1", router.findFromName("eth1").getInterfaceAddress().ipAddress().toString());
		assertEquals(1, router.getRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testConfigurationRollbackOnError() {
		Router router = new Router("R1", List.of(new RouterInterface("eth0")));
		CliSession session = new CliSession(new DefaultCommandExecutor(new RouterCLIParser(CommandRegistry.defaultRegistry())),
				new CommandExecutionContext(router, new NetworkTopology(), new PrintWriterCommandOutput(new PrintWriter(System.out))));

		session.execute("configure");
		session.execute("set interfaces ethernet eth0 address 192.168.1.1/24");
		session.execute("commit");

		String invalidConfig = """
				set interfaces ethernet eth0 address 10.0.0.1/24
				set protocols static route 192.168.1.0/24 invalid-option value
				""";

		ConfigurationParser parser = new CommandConfigurationParser();

		assertThrows(ConfigurationParseException.class, () -> parser.loadConfiguration(router, invalidConfig));
		assertEquals("192.168.1.1", router.findFromName("eth0").getInterfaceAddress().ipAddress().toString());
	}

	@Test
	void testConfigurationWithNonExistentInterface() {
		Router router = new Router("R1");
		String config = """
				set interfaces ethernet eth0 address 192.168.1.1/24
				set interfaces ethernet eth1 address 192.168.2.1/24
				""";

		ConfigurationParser parser = new CommandConfigurationParser();

		ConfigurationParseException exception = assertThrows(ConfigurationParseException.class, () -> parser.loadConfiguration(router, config));
		assertTrue(exception.getMessage().contains("eth1") && exception.getMessage().contains("does not exist"), "Exception should mention eth1 does not exist");
		assertNull(router.findFromName("eth0").getInterfaceAddress(), "Configuration should be rolled back on error");
	}

	@Test
	void testConfigurationWithCorrectInterfaces() {
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		String config = """
				set interfaces ethernet eth0 address 192.168.1.1/24
				set interfaces ethernet eth1 address 192.168.2.1/24
				set protocols static route 10.0.0.0/8 interface eth1
				""";

		ConfigurationParser parser = new CommandConfigurationParser();

		assertDoesNotThrow(() -> parser.loadConfiguration(router, config));
		assertEquals("192.168.1.1", router.findFromName("eth0").getInterfaceAddress().ipAddress().toString());
		assertEquals("192.168.2.1", router.findFromName("eth1").getInterfaceAddress().ipAddress().toString());
		assertEquals(1, router.getRoutingTable().getRoutingEntries().size());
	}

	@Test
	void testConfigurationWithNonExistentInterfaceInRoute() {
		Router router = new Router("R1");
		String config = """
				set interfaces ethernet eth0 address 192.168.1.1/24
				set protocols static route 10.0.0.0/8 interface eth1
				""";

		ConfigurationParser parser = new CommandConfigurationParser();

		ConfigurationParseException exception = assertThrows(ConfigurationParseException.class, () -> parser.loadConfiguration(router, config));
		assertTrue(exception.getMessage().contains("eth1") && exception.getMessage().contains("does not exist"), "Exception should mention eth1 does not exist");
		assertNull(router.findFromName("eth0").getInterfaceAddress(), "Configuration should be rolled back on error");
	}

	@Test
	void testAutomaticFormatDetection() {
		Router router1 = new Router("R1", List.of(new RouterInterface("eth0")));
		Router router2 = new Router("R2", List.of(new RouterInterface("eth0")));

		String commandConfig = "set interfaces ethernet eth0 address 192.168.1.1/24";
		ConfigurationParser parser1 = ConfigurationFactory.getParser(commandConfig);
		assertDoesNotThrow(() -> parser1.loadConfiguration(router1, commandConfig));

		String hierarchicalConfig = """
				interfaces {
				    ethernet eth0 {
				        address 192.168.1.1/24
				    }
				}
				""";
		ConfigurationParser parser2 = ConfigurationFactory.getParser(hierarchicalConfig);
		assertDoesNotThrow(() -> parser2.loadConfiguration(router2, hierarchicalConfig));

		assertEquals(router1.findFromName("eth0").getInterfaceAddress().toString(), router2.findFromName("eth0").getInterfaceAddress().toString());
	}

	@Test
	void testHierarchicalConfigurationFormat() {
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		String config = """
				interfaces {
				    ethernet eth0 {
				        address 192.168.1.1/24
				    }
				    ethernet eth1 {
				        address 192.168.2.1/24
				    }
				}
				protocols {
				    static {
				        route 10.0.0.0/8 {
				            interface eth1
				            distance 5
				        }
				    }
				}
				""";

		ConfigurationParser parser = new HierarchicalConfigurationParser();

		assertDoesNotThrow(() -> parser.loadConfiguration(router, config));
		assertEquals("192.168.1.1", router.findFromName("eth0").getInterfaceAddress().ipAddress().toString());
		assertEquals("192.168.2.1", router.findFromName("eth1").getInterfaceAddress().ipAddress().toString());
		assertEquals(1, router.getRoutingTable().getRoutingEntries().size());
		assertEquals(5, router.getRoutingTable().getRoutingEntries().getFirst().getAdministrativeDistance());
	}

	@Test
	void testHierarchicalConfigurationGenerator() {
		String config = getString();
		System.out.println("=== Hierarchical Configuration ===");
		System.out.println(config);

		Router newRouter = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		ConfigurationParser parser = new HierarchicalConfigurationParser();

		assertDoesNotThrow(() -> parser.loadConfiguration(newRouter, config));
		assertEquals("192.168.1.1", newRouter.findFromName("eth0").getInterfaceAddress().ipAddress().toString());
		assertEquals("192.168.2.1", newRouter.findFromName("eth1").getInterfaceAddress().ipAddress().toString());
	}
}