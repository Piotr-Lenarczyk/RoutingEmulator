import org.junit.jupiter.api.Test;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.cli.RouterCLIParser;
import org.uj.routingemulator.router.config.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationTest {

	@Test
	public void testConfigurationSaveAndLoad() {
		// Create and configure router
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		RouterCLIParser cli = new RouterCLIParser();
		cli.executeCommand("configure", router);
		cli.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		cli.executeCommand("set interfaces ethernet eth1 address 192.168.2.1/24", router);
		cli.executeCommand("set protocols static route 192.168.3.0/24 next-hop 192.168.1.254", router);
		cli.executeCommand("set protocols static route 10.0.0.0/8 interface eth1 distance 5", router);
		cli.executeCommand("commit", router);

		// Generate configuration
		ConfigurationGenerator generator = new CommandConfigurationGenerator();
		String config = generator.generateConfiguration(router);

		System.out.println("=== Generated Configuration ===");
		System.out.println(config);

		// Load configuration into new router
		Router newRouter = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		ConfigurationParser parser = new CommandConfigurationParser();
		parser.loadConfiguration(newRouter, config);

		// Verify by generating config from new router
		String verifyConfig = generator.generateConfiguration(newRouter);
		System.out.println("\n=== Verification - New Router Configuration ===");
		System.out.println(verifyConfig);

		// Assert they match
		assertEquals(config, verifyConfig, "Configurations should match");

		// Verify specific settings
		assertEquals(2, newRouter.getInterfaces().size());
		assertNotNull(newRouter.findFromName("eth0").getInterfaceAddress());
		assertNotNull(newRouter.findFromName("eth1").getInterfaceAddress());
		assertEquals(2, newRouter.getRoutingTable().getRoutingEntries().size());
	}

	@Test
	public void testConfigurationWithDisabledInterface() {
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		RouterCLIParser cli = new RouterCLIParser();
		cli.executeCommand("configure", router);
		cli.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		cli.executeCommand("set interfaces ethernet eth0 disable", router);
		cli.executeCommand("commit", router);

		ConfigurationGenerator generator = new CommandConfigurationGenerator();
		String config = generator.generateConfiguration(router);

		Router newRouter = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		ConfigurationParser parser = new CommandConfigurationParser();
		parser.loadConfiguration(newRouter, config);

		assertTrue(newRouter.findFromName("eth0").isDisabled());
	}

	@Test
	public void testConfigurationWithCommentsAndEmptyLines() {
		String config = """
				# Interface configuration
				set interfaces ethernet eth0 address 192.168.1.1/24
				
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
	public void testInvalidConfiguration() {
		String invalidConfig = "set protocols static route 192.168.1.0/24 invalid-option value";

		Router router = new Router("R1", List.of(new RouterInterface("eth0")));
		ConfigurationParser parser = new CommandConfigurationParser();

		assertThrows(ConfigurationParseException.class, () -> {
			parser.loadConfiguration(router, invalidConfig);
		});
	}

	@Test
	public void testConfigurationRollbackOnError() {
		Router router = new Router("R1", List.of(new RouterInterface("eth0")));
		RouterCLIParser cli = new RouterCLIParser();
		cli.executeCommand("configure", router);
		cli.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		cli.executeCommand("commit", router);

		// Configuration with error in second line
		String invalidConfig = """
				set interfaces ethernet eth0 address 10.0.0.1/24
				set protocols static route 192.168.1.0/24 invalid-option value
				""";

		ConfigurationParser parser = new CommandConfigurationParser();
		assertThrows(ConfigurationParseException.class, () -> {
			parser.loadConfiguration(router, invalidConfig);
		});

		// Verify original configuration is preserved
		assertEquals("192.168.1.1", router.findFromName("eth0").getInterfaceAddress().getIpAddress().toString());
	}

	@Test
	public void testOverwriteExistingConfiguration() {
		// Create router and configure it
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		RouterCLIParser cli = new RouterCLIParser();
		cli.executeCommand("configure", router);
		cli.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		cli.executeCommand("set interfaces ethernet eth1 address 192.168.2.1/24", router);
		cli.executeCommand("set protocols static route 10.0.0.0/8 next-hop 192.168.1.254", router);
		cli.executeCommand("commit", router);

		// Generate configuration from this router
		ConfigurationGenerator generator = new CommandConfigurationGenerator();
		String savedConfig = generator.generateConfiguration(router);

		System.out.println("=== Initial Configuration ===");
		System.out.println(savedConfig);

		// Now change the router's configuration completely via CLI
		cli.executeCommand("configure", router);
		cli.executeCommand("set interfaces ethernet eth0 address 10.10.10.1/24", router);
		cli.executeCommand("set protocols static route 172.16.0.0/16 next-hop 10.10.10.254", router);
		cli.executeCommand("commit", router);

		String modifiedConfig = generator.generateConfiguration(router);
		System.out.println("\n=== Modified Configuration ===");
		System.out.println(modifiedConfig);

		// Now load the original saved configuration - it should overwrite the modified one
		ConfigurationParser parser = new CommandConfigurationParser();
		parser.loadConfiguration(router, savedConfig);

		String restoredConfig = generator.generateConfiguration(router);
		System.out.println("\n=== Restored Configuration ===");
		System.out.println(restoredConfig);

		// Verify the configuration was restored to original
		assertEquals(savedConfig, restoredConfig, "Configuration should be restored to original");
		assertEquals("192.168.1.1", router.findFromName("eth0").getInterfaceAddress().getIpAddress().toString());
		assertEquals("192.168.2.1", router.findFromName("eth1").getInterfaceAddress().getIpAddress().toString());
		assertEquals(1, router.getRoutingTable().getRoutingEntries().size());
	}

	@Test
	public void testConfigurationWithNonExistentInterface() {
		// Router with default constructor (eth0, lo)
		Router router = new Router("R1");

		// Configuration tries to use eth1 which doesn't exist
		String config = """
				set interfaces ethernet eth0 address 192.168.1.1/24
				set interfaces ethernet eth1 address 192.168.2.1/24
				""";

		ConfigurationParser parser = new CommandConfigurationParser();
		ConfigurationParseException exception = assertThrows(ConfigurationParseException.class, () -> {
			parser.loadConfiguration(router, config);
		});

		assertTrue(exception.getMessage().contains("eth1") && exception.getMessage().contains("does not exist"),
			"Exception should mention eth1 does not exist");

		// Verify that first valid command was rolled back
		assertNull(router.findFromName("eth0").getInterfaceAddress(),
			"Configuration should be rolled back on error");
	}

	@Test
	public void testConfigurationWithNonExistentInterfaceInRoute() {
		// Router with default constructor (eth0, lo)
		Router router = new Router("R1");

		// Configuration tries to use eth1 in route which doesn't exist
		String config = """
				set interfaces ethernet eth0 address 192.168.1.1/24
				set protocols static route 10.0.0.0/8 interface eth1
				""";

		ConfigurationParser parser = new CommandConfigurationParser();
		ConfigurationParseException exception = assertThrows(ConfigurationParseException.class, () -> {
			parser.loadConfiguration(router, config);
		});

		assertTrue(exception.getMessage().contains("eth1") && exception.getMessage().contains("does not exist"),
			"Exception should mention eth1 does not exist");

		// Verify configuration was rolled back
		assertNull(router.findFromName("eth0").getInterfaceAddress(),
			"Configuration should be rolled back on error");
	}

	@Test
	public void testConfigurationWithCorrectInterfaces() {
		// Router with eth0, eth1
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

		// Configuration for this specific router
		String config = """
				set interfaces ethernet eth0 address 192.168.1.1/24
				set interfaces ethernet eth1 address 192.168.2.1/24
				set protocols static route 10.0.0.0/8 interface eth1
				""";

		ConfigurationParser parser = new CommandConfigurationParser();
		// This should succeed
		assertDoesNotThrow(() -> parser.loadConfiguration(router, config));

		// Verify configuration was applied
		assertEquals("192.168.1.1", router.findFromName("eth0").getInterfaceAddress().getIpAddress().toString());
		assertEquals("192.168.2.1", router.findFromName("eth1").getInterfaceAddress().getIpAddress().toString());
		assertEquals(1, router.getRoutingTable().getRoutingEntries().size());
	}

	@Test
	public void testHierarchicalConfigurationFormat() {
		// Router with eth0, eth1
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));

		// Hierarchical configuration format
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

		// Verify configuration was applied
		assertEquals("192.168.1.1", router.findFromName("eth0").getInterfaceAddress().getIpAddress().toString());
		assertEquals("192.168.2.1", router.findFromName("eth1").getInterfaceAddress().getIpAddress().toString());
		assertEquals(1, router.getRoutingTable().getRoutingEntries().size());
		assertEquals(5, router.getRoutingTable().getRoutingEntries().get(0).getAdministrativeDistance());
	}

	@Test
	public void testAutomaticFormatDetection() {
		Router router1 = new Router("R1", List.of(new RouterInterface("eth0")));
		Router router2 = new Router("R2", List.of(new RouterInterface("eth0")));

		// Command format
		String commandConfig = "set interfaces ethernet eth0 address 192.168.1.1/24";
		ConfigurationParser parser1 = ConfigurationFactory.getParser(commandConfig);
		assertDoesNotThrow(() -> parser1.loadConfiguration(router1, commandConfig));

		// Hierarchical format
		String hierarchicalConfig = """
				interfaces {
				    ethernet eth0 {
				        address 192.168.1.1/24
				    }
				}
				""";
		ConfigurationParser parser2 = ConfigurationFactory.getParser(hierarchicalConfig);
		assertDoesNotThrow(() -> parser2.loadConfiguration(router2, hierarchicalConfig));

		// Both should result in same configuration
		assertEquals(router1.findFromName("eth0").getInterfaceAddress().toString(),
				router2.findFromName("eth0").getInterfaceAddress().toString());
	}

	@Test
	public void testHierarchicalConfigurationGenerator() {
		Router router = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		RouterCLIParser cli = new RouterCLIParser();
		cli.executeCommand("configure", router);
		cli.executeCommand("set interfaces ethernet eth0 address 192.168.1.1/24", router);
		cli.executeCommand("set interfaces ethernet eth1 address 192.168.2.1/24", router);
		cli.executeCommand("set protocols static route 10.0.0.0/8 interface eth1", router);
		cli.executeCommand("commit", router);

		// Generate hierarchical format
		ConfigurationGenerator generator = ConfigurationFactory.getHierarchicalGenerator();
		String config = generator.generateConfiguration(router);

		System.out.println("=== Hierarchical Configuration ===");
		System.out.println(config);

		// Verify it can be parsed back
		Router newRouter = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1")));
		ConfigurationParser parser = new HierarchicalConfigurationParser();
		assertDoesNotThrow(() -> parser.loadConfiguration(newRouter, config));

		assertEquals("192.168.1.1", newRouter.findFromName("eth0").getInterfaceAddress().getIpAddress().toString());
		assertEquals("192.168.2.1", newRouter.findFromName("eth1").getInterfaceAddress().getIpAddress().toString());
	}
}

