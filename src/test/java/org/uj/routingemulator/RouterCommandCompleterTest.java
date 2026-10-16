package org.uj.routingemulator;

import org.jline.reader.Candidate;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.Test;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.RouterCommandCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterCommandCompleterTest {
	@Test
	void deleteCompletionOffersDisableForDisabledDistanceRoute() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		router.configureInterface("eth0", new InterfaceAddress(
				new IPAddress(192, 0, 2, 2), new SubnetMask(24)));
		StaticRoutingEntry route = new StaticRoutingEntry(
				new Subnet(new IPAddress(198, 51, 100, 0), new SubnetMask(24)),
				new IPAddress(192, 0, 2, 2), 50);
		router.addRoute(route);
		router.disableRoute(route);
		List<Candidate> routeCandidates = complete(router, "delete protocols static route ");
		List<Candidate> candidates = complete(router,
				"delete protocols static route 198.51.100.0/24 next-hop 192.0.2.2 distance 50 ");

		assertTrue(routeCandidates.stream().anyMatch(candidate -> candidate.value().equals("198.51.100.0/24")));
		assertTrue(candidates.stream().anyMatch(candidate -> candidate.value().equals("disable")));
	}

	@Test
	void deleteCompletionDoesNotOfferDisableForEnabledRoute() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		router.addRoute(new StaticRoutingEntry(
				new Subnet(new IPAddress(198, 51, 100, 0), new SubnetMask(24)),
				new IPAddress(192, 0, 2, 2)));
		List<Candidate> candidates = complete(router,
				"delete protocols static route 198.51.100.0/24 next-hop 192.0.2.2 ");

		assertFalse(candidates.stream().anyMatch(candidate -> candidate.value().equals("disable")));
	}

	@Test
	void deleteInterfaceCompletionOffersConfiguredVifId() {
		Router router = new Router("Router");
		router.setMode(RouterMode.CONFIGURATION);
		router.configureInterface("eth0.10", new InterfaceAddress(
				new IPAddress(192, 0, 2, 1), new SubnetMask(24)));

		List<Candidate> candidates = complete(router, "delete interfaces ethernet eth0 vif ");

		assertTrue(candidates.stream().anyMatch(candidate -> candidate.value().equals("10")));
	}

	private List<Candidate> complete(Router router, String line) {
		String[] words = line.trim().split("\\s+");
		ParsedLine parsedLine = new ParsedLine() {
			@Override
			public String word() {
				return line.endsWith(" ") ? "" : words[words.length - 1];
			}

			@Override
			public int wordCursor() {
				return word().length();
			}

			@Override
			public int wordIndex() {
				return words.length - 1;
			}

			@Override
			public List<String> words() {
				return Arrays.asList(words);
			}

			@Override
			public String line() {
				return line;
			}

			@Override
			public int cursor() {
				return line.length();
			}
		};
		List<Candidate> candidates = new ArrayList<>();
		new RouterCommandCompleter(router).complete(null, parsedLine, candidates);
		return candidates;
	}
}