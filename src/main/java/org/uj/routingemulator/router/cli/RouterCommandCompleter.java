package org.uj.routingemulator.router.cli;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.uj.routingemulator.router.InterfaceType;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;

import java.util.List;

/**
 * JLine completer for VyOS-style router commands.
 * Provides context-aware command completion based on the router's current mode and partial input.
 */
public class RouterCommandCompleter implements Completer {

	private static final String PROTOCOLS = "protocols";
	private static final String STATIC = "static";
	private static final String ETHERNET = "ethernet";
	private static final String ROUTE = "route";
	private static final String NEXT_HOP = "next-hop";
	private static final String INTERFACE = "interface";
	private static final String INTERFACES = "interfaces";
	private static final String DELETE = "delete";
	private static final String INTERFACE_UPPERCASE = "Interface ";
	private static final String ADDRESS = "address";
	private static final String DISABLE = "disable";

	private final Router router;

	public RouterCommandCompleter(Router router) {
		this.router = router;
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		// Don't trim - we need trailing spaces to detect completion state
		String buffer = line.line();
		String bufferTrimmed = buffer.trim();
		String[] words = bufferTrimmed.isEmpty() ? new String[]{""} : bufferTrimmed.split("\\s+");

		// Get the current word being typed (might be incomplete)
		String currentWord = line.word();

		// Check if the line ends with a space - this means we're completing the NEXT word (empty)
		boolean endsWithSpace = buffer.endsWith(" ") || buffer.endsWith("\t");

		// If line ends with space, we're completing a new empty word
		if (endsWithSpace) {
			currentWord = "";
			// Add empty word to the array to represent the position we're completing
			String[] newWords = new String[words.length + 1];
			System.arraycopy(words, 0, newWords, 0, words.length);
			newWords[words.length] = "";
			words = newWords;
		} else if (!currentWord.isEmpty() && isCompleteCommand(words, currentWord)) {
			// Check if current word is a complete match for any command at this level
			// If so, treat it as if we're completing the next level
			currentWord = "";
			String[] newWords = new String[words.length + 1];
			System.arraycopy(words, 0, newWords, 0, words.length);
			newWords[words.length] = "";
			words = newWords;
		}

		if (router.getMode() == RouterMode.OPERATIONAL) {
			completeOperationalMode(words, currentWord, candidates);
		} else {
			completeConfigurationMode(words, currentWord, candidates);
		}
	}

	private boolean isCompleteCommand(String[] words, String currentWord) {
		if (router.getMode() == RouterMode.OPERATIONAL) {
			if (words.length == 1) {
				return currentWord.equalsIgnoreCase("show");
			} else if (words.length == 2 && words[0].equalsIgnoreCase("show")) {
				return currentWord.equalsIgnoreCase("ip");
			}
		} else {
			return verifyConfigurationMode(words, currentWord);
		}
		return false;
	}

	private boolean verifyConfigurationMode(String[] words, String currentWord) {
		if (words.length == 1) {
			return currentWord.equalsIgnoreCase("set") || currentWord.equalsIgnoreCase(DELETE);
		} else if (words.length == 2 && (words[0].equalsIgnoreCase("set") || words[0].equalsIgnoreCase(DELETE))) {
			return currentWord.equalsIgnoreCase(INTERFACES) || currentWord.equalsIgnoreCase(PROTOCOLS);
		} else if (words.length == 3 && words[1].equalsIgnoreCase(INTERFACES)) {
			return currentWord.equalsIgnoreCase(ETHERNET);
		} else if (words.length == 3 && words[1].equalsIgnoreCase(PROTOCOLS)) {
			return currentWord.equalsIgnoreCase(STATIC);
		} else if (words.length == 4 && words[2].equalsIgnoreCase(STATIC)) {
			return currentWord.equalsIgnoreCase(ROUTE);
		} else if (words.length == 6 && words[3].equalsIgnoreCase(ROUTE)) {
			return currentWord.equalsIgnoreCase(NEXT_HOP) || currentWord.equalsIgnoreCase(INTERFACE);
		}
		return false;
	}

	private void completeOperationalMode(String[] words, String currentWord, List<Candidate> candidates) {
		if (words.length <= 1) {
			addCandidateIfMatches(candidates, "configure", "Enter configuration mode", currentWord);
			addCandidateIfMatches(candidates, "show", "Show information", currentWord);
		} else if (words[0].equalsIgnoreCase("show")) {
			if (words.length == 2) {
				addCandidateIfMatches(candidates, "ip", "Show IP information", currentWord);
				addCandidateIfMatches(candidates, INTERFACES, "Show interface information", currentWord);
				addCandidateIfMatches(candidates, "configuration", "Show configuration", currentWord);
			} else if (words.length == 3 && words[1].equalsIgnoreCase("ip")) {
				addCandidateIfMatches(candidates, ROUTE, "Show IP routing table", currentWord);
			}
		}
	}

	private void completeConfigurationMode(String[] words, String currentWord, List<Candidate> candidates) {
		if (words.length <= 1) {
			addCandidateIfMatches(candidates, "set", "Add or modify configuration", currentWord);
			addCandidateIfMatches(candidates, DELETE, "Remove configuration", currentWord);
			addCandidateIfMatches(candidates, "show", "Show current configuration", currentWord);
			addCandidateIfMatches(candidates, "commit", "Apply configuration changes", currentWord);
			addCandidateIfMatches(candidates, "exit", "Exit configuration mode", currentWord);
		} else if (words[0].equalsIgnoreCase("set") || words[0].equalsIgnoreCase(DELETE)) {
			completeSetDeleteCommand(words, currentWord, candidates);
		} else if (words[0].equalsIgnoreCase("show") && words.length == 2) {
			addCandidateIfMatches(candidates, "configuration", "Show current configuration", currentWord);
		}
	}

	private void completeSetDeleteCommand(String[] words, String currentWord, List<Candidate> candidates) {
		boolean isDelete = words[0].equalsIgnoreCase(DELETE);

		if (words.length == 2) {
			addCandidateIfMatches(candidates, INTERFACES, "Configure interfaces", currentWord);

			// Tylko podpowiedz 'protocols' przy 'delete', jesli istnieje jakakolwiek trasa statyczna
			if (!isDelete || !router.getStagedRoutingTable().getRoutingEntries().isEmpty()) {
				addCandidateIfMatches(candidates, PROTOCOLS, "Configure protocols", currentWord);
			}
		} else if (words[1].equalsIgnoreCase(INTERFACES)) {
			completeInterfaces(words, currentWord, candidates);
		} else if (words[1].equalsIgnoreCase(PROTOCOLS)) {
			completeProtocols(words, currentWord, candidates);
		}
	}

	private void completeProtocols(String[] words, String currentWord, List<Candidate> candidates) {
		boolean isDelete = words[0].equalsIgnoreCase(DELETE);

		if (words.length == 3) {
			if (!isDelete || !router.getStagedRoutingTable().getRoutingEntries().isEmpty()) {
				addCandidateIfMatches(candidates, STATIC, "Static routing", currentWord);
			}
		} else if (words.length == 4 && words[2].equalsIgnoreCase(STATIC)) {
			if (!isDelete || !router.getStagedRoutingTable().getRoutingEntries().isEmpty()) {
				addCandidateIfMatches(candidates, ROUTE, "Configure static route", currentWord);
			}
		} else if (words.length == 5 && words[3].equalsIgnoreCase(ROUTE)) {
			if (!isDelete) {
				// Hint for IPv4 Route when setting
				candidates.add(new Candidate(currentWord, "<x.x.x.x/x>", null, "IPv4 static route", null, null, false));
			}

			// Suggest existing routes for convenience (or as the ONLY options for delete)
			for (StaticRoutingEntry entry : router.getStagedRoutingTable().getRoutingEntries()) {
				addCandidateIfMatches(candidates, entry.getSubnet().toString(), null, currentWord);
			}
		} else if (words.length == 6 && words[3].equalsIgnoreCase(ROUTE)) {
			String targetSubnet = words[4];

			// For delete commands, verify the subnet actually exists in the router before offering subcommands
			if (isDelete) {
				boolean routeExists = router.getStagedRoutingTable().getRoutingEntries().stream()
						.anyMatch(entry -> entry.getSubnet().toString().equals(targetSubnet));
				if (!routeExists) return;
			}

			addCandidateIfMatches(candidates, NEXT_HOP, "Specify next-hop IP address", currentWord);
			addCandidateIfMatches(candidates, INTERFACE, "Specify outgoing interface", currentWord);
		} else {
			completeRouteType(words, currentWord, candidates, isDelete);
		}
	}

	private void completeRouteType(String[] words, String currentWord, List<Candidate> candidates, boolean isDelete) {
		String targetSubnet = words[4];

		if (words.length == 7 && words[5].equalsIgnoreCase(NEXT_HOP)) {
			if (!isDelete) {
				candidates.add(new Candidate(currentWord, "<x.x.x.x>", null, "IPv4 gateway address", null, null, false));
			} else {
				// For delete: suggest only next-hops that actually exist for this subnet
				router.getStagedRoutingTable().getRoutingEntries().stream()
						.filter(e -> e.getSubnet().toString().equals(targetSubnet) && e.getNextHop() != null)
						.forEach(e -> addCandidateIfMatches(candidates, e.getNextHop().toString(), null, currentWord));
			}
		} else if (words.length == 7 && words[5].equalsIgnoreCase(INTERFACE)) {
			if (!isDelete) {
				candidates.add(new Candidate(currentWord, "<ethN>", null, "Ethernet interface name", null, null, false));
				for (RouterInterface iface : router.getInterfaces()) {
					addCandidateIfMatches(candidates, iface.getInterfaceName(), null, currentWord);
				}
			} else {
				// For delete: suggest only interfaces that actually exist for this route
				router.getStagedRoutingTable().getRoutingEntries().stream()
						.filter(e -> e.getSubnet().toString().equals(targetSubnet) && e.getRouterInterface() != null)
						.forEach(e -> addCandidateIfMatches(candidates, e.getRouterInterface().getInterfaceName(), null, currentWord));
			}
		} else if (words.length == 8 && (words[5].equalsIgnoreCase(NEXT_HOP) || words[5].equalsIgnoreCase(INTERFACE))) {
			addCandidateIfMatches(candidates, "distance", "Set administrative distance", currentWord);
			addCandidateIfMatches(candidates, DISABLE, "Disable route", currentWord);
		} else if (words.length == 9 && words[7].equalsIgnoreCase("distance")) {
			if (!isDelete) {
				candidates.add(new Candidate(currentWord, "<1-255>", null, "Administrative distance", null, null, false));
			} else {
				// For delete distance: find the exact route and suggest its current distance
				router.getStagedRoutingTable().getRoutingEntries().stream()
						.filter(e -> e.getSubnet().toString().equals(targetSubnet))
						.forEach(e -> addCandidateIfMatches(candidates, String.valueOf(e.getAdministrativeDistance()), null, currentWord));
			}
		}
	}

	private void completeInterfaces(String[] words, String currentWord, List<Candidate> candidates) {
		boolean isDelete = words[0].equalsIgnoreCase(DELETE);

		if (words.length == 3) {
			if (!isDelete || hasInterfaceOfType(InterfaceType.ETHERNET)) {
				addCandidateIfMatches(candidates, ETHERNET, "Configure Ethernet interface", currentWord);
			}
			if (!isDelete || hasInterfaceOfType(InterfaceType.DUMMY)) {
				addCandidateIfMatches(candidates, "dummy", "Configure Dummy interface", currentWord);
			}
		} else if (words.length == 4) {
			checkInterfaceType(words, currentWord, candidates, isDelete);
		} else if (words.length == 5) {
			String targetIface = words[3];
			RouterInterface matchedInterface = router.findFromName(targetIface);

			// Only offer subcommands if the interface exists or if we are setting a new one
			if (!isDelete || matchedInterface != null) {
				if (!isDelete || matchedInterface.getInterfaceAddress() != null) {
					addCandidateIfMatches(candidates, ADDRESS, "Set IP address", currentWord);
				}
				if (!isDelete || matchedInterface.isDisabled()) {
					addCandidateIfMatches(candidates, DISABLE, "Disable interface", currentWord);
				}
			}

			if (words[2].equalsIgnoreCase(ETHERNET)) {
				if (!isDelete || hasVifChild(targetIface)) {
					addCandidateIfMatches(candidates, "vif", "Virtual Local Area Network (VLAN) ID", currentWord);
				}
			}
		} else if (words.length == 6) {
			suggestInterfaceArgument(words, currentWord, candidates, isDelete);
		} else if (words.length == 7) {
			if (words[4].equalsIgnoreCase("vif")) {
				String targetIface = words[3] + "." + words[5];
				RouterInterface matchedInterface = router.findFromName(targetIface);

				if (!isDelete || matchedInterface != null) {
					if (!isDelete || matchedInterface.getInterfaceAddress() != null) {
						addCandidateIfMatches(candidates, ADDRESS, "Set IP address", currentWord);
					}
					if (!isDelete || matchedInterface.isDisabled()) {
						addCandidateIfMatches(candidates, DISABLE, "Disable interface", currentWord);
					}
				}
			}
		} else if (words.length == 8 && words[4].equalsIgnoreCase("vif") && words[6].equalsIgnoreCase(ADDRESS)) {
			String targetIface = words[3] + "." + words[5];
			suggestAddressArgument(targetIface, currentWord, candidates, isDelete);
		}
	}

	private void suggestInterfaceArgument(String[] words, String currentWord, List<Candidate> candidates, boolean isDelete) {
		if (words[4].equalsIgnoreCase(ADDRESS)) {
			suggestAddressArgument(words[3], currentWord, candidates, isDelete);
		} else if (words[4].equalsIgnoreCase("vif")) {
			if (!isDelete) {
				candidates.add(new Candidate(currentWord, "0-4094", null, "Virtual Local Area Network (VLAN) ID", null, null, false));
			} else {
				RouterInterface parent = router.findFromName(words[3]);
				if (parent != null) {
					router.getStagedInterfaces().stream()
							.filter(i -> i.isChildOf(parent))
							.map(RouterInterface::getVlanId)
							.filter(id -> id != null)
							.forEach(id -> addCandidateIfMatches(candidates, id, null, currentWord));
				}
			}
		}
	}

	private void suggestAddressArgument(String interfaceName, String currentWord, List<Candidate> candidates, boolean isDelete) {
		if (!isDelete) {
			candidates.add(new Candidate(currentWord, "<x.x.x.x/prefix>", null, "IPv4 address and prefix", null, null, false));
		} else {
			// For delete, only suggest the currently configured IP address of this interface
			RouterInterface matchedInterface = router.findFromName(interfaceName);
			if (matchedInterface != null && matchedInterface.getInterfaceAddress() != null) {
				addCandidateIfMatches(candidates, matchedInterface.getInterfaceAddress().toString(), null, currentWord);
			}
		}
	}

	private void checkInterfaceType(String[] words, String currentWord, List<Candidate> candidates, boolean isDelete) {
		if (words[2].equalsIgnoreCase(ETHERNET)) {
			if (!isDelete) {
				candidates.add(new Candidate(currentWord, "ethN", null, "Ethernet interface name", null, null, false));
			}
			for (RouterInterface iface : router.getInterfaces()) {
				if (iface.getType() == InterfaceType.ETHERNET) {
					addCandidateIfMatches(candidates, iface.getInterfaceName(), null, currentWord);
				}
			}
		} else if (words[2].equalsIgnoreCase("dummy")) {
			if (!isDelete) {
				candidates.add(new Candidate(currentWord, "dumN", null, "Dummy interface name", null, null, false));
			}
			for (RouterInterface iface : router.getInterfaces()) {
				if (iface.getType() == InterfaceType.DUMMY) {
					addCandidateIfMatches(candidates, iface.getInterfaceName(), null, currentWord);
				}
			}
			if (!isDelete) {
				addCandidateIfMatches(candidates, "dum0", null, currentWord);
			}
		}
	}

	private void addCandidateIfMatches(List<Candidate> candidates, String value, String description, String currentWord) {
		if (currentWord == null || currentWord.isEmpty() || value.toLowerCase().startsWith(currentWord.toLowerCase())) {
			candidates.add(new Candidate(value, value, null, description, null, null, true));
		}
	}

	private boolean hasInterfaceOfType(InterfaceType type) {
		return router.getStagedInterfaces().stream().anyMatch(i -> i.getType() == type);
	}

	private boolean hasVifChild(String parentInterfaceName) {
		RouterInterface parent = router.findFromName(parentInterfaceName);
		if (parent == null) return false;
		return router.getStagedInterfaces().stream().anyMatch(i -> i.isChildOf(parent));
	}
}