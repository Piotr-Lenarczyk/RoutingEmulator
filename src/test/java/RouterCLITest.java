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
public class RouterCLITest {
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

	/**
	 * Test that 'configure' command changes router mode from OPERATIONAL to CONFIGURATION.
	 */
	@Test
	void testConfigureCommandFromOperationalMode() {
		// Given
		router.setMode(RouterMode.OPERATIONAL);

		// When
		parser.executeCommand("configure", router);

		// Then
		assertEquals(RouterMode.CONFIGURATION, router.getMode());
		assertTrue(outputStream.toString().contains("[edit]"));
	}

	/**
	 * Test that 'configure' command in CONFIGURATION mode shows error message.
	 */
	@Test
	void testConfigureCommandFromConfigurationMode() {
		// Given
		router.setMode(RouterMode.CONFIGURATION);

		// When
		parser.executeCommand("configure", router);

		// Then
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
		// Given
		router.setMode(RouterMode.OPERATIONAL);

		// When
		parser.executeCommand("  configure  ", router);

		// Then
		assertEquals(RouterMode.CONFIGURATION, router.getMode());
		assertTrue(outputStream.toString().contains("[edit]"));
	}

	/**
	 * Test that 'configure' command is case-sensitive.
	 */
	@Test
	void testConfigureCommandCaseSensitive() {
		// Given
		router.setMode(RouterMode.OPERATIONAL);

		// When
		parser.executeCommand("Configure", router);

		// Then
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertTrue(outputStream.toString().contains("Command not recognized or not supported"));
	}

	/**
	 * Test that 'configure' command with extra parameters is not recognized.
	 */
	@Test
	void testConfigureCommandWithExtraParameters() {
		// Given
		router.setMode(RouterMode.OPERATIONAL);

		// When
		parser.executeCommand("configure something", router);

		// Then
		assertEquals(RouterMode.OPERATIONAL, router.getMode());
		assertTrue(outputStream.toString().contains("Command not recognized or not supported"));
	}
}
