package org.uj.routingemulator.router.cli;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;

import java.util.List;

/**
 * JLine completer for VyOS-style router commands.
 * Provides context-aware command completion based on the router's current mode and partial input.
 */
public class RouterCommandCompleter implements Completer {
	private final Router router;
	private final List<RouterCommand> commands;

	public RouterCommandCompleter(Router router, List<RouterCommand> commands) {
		this.router = router;
		this.commands = commands;
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
		} else if (!currentWord.isEmpty()) {
			// Check if current word is a complete match for any command at this level
			// If so, treat it as if we're completing the next level
			if (isCompleteCommand(words, currentWord)) {
				// Current word is complete, move to next level
				currentWord = "";
				String[] newWords = new String[words.length + 1];
				System.arraycopy(words, 0, newWords, 0, words.length);
				newWords[words.length] = "";
				words = newWords;
			}
		}

		if (router.getMode() == RouterMode.OPERATIONAL) {
			completeOperationalMode(words, currentWord, candidates);
		} else {
			completeConfigurationMode(words, currentWord, candidates);
		}
	}

	/**
	 * Checks if the current word is a complete command at the current level AND has subcommands.
	 * We only want to advance to the next level if the command expects more input.
	 *
	 * @param words All words in the command so far
	 * @param currentWord The word being typed
	 * @return true if currentWord is a complete valid command that expects subcommands
	 */
	private boolean isCompleteCommand(String[] words, String currentWord) {
		if (router.getMode() == RouterMode.OPERATIONAL) {
			if (words.length == 1) {
				// "configure" has no subcommands, "show" does
				return currentWord.equalsIgnoreCase("show");
			} else if (words.length == 2 && words[0].equalsIgnoreCase("show")) {
				// "interfaces" and "configuration" are complete, "ip" has subcommands
				return currentWord.equalsIgnoreCase("ip");
			}
		} else { // CONFIGURATION mode
			if (words.length == 1) {
				// Only "set" and "delete" have subcommands, others are complete commands
				return currentWord.equalsIgnoreCase("set") ||
				       currentWord.equalsIgnoreCase("delete");
			} else if (words.length == 2 && (words[0].equalsIgnoreCase("set") || words[0].equalsIgnoreCase("delete"))) {
				// Both "interfaces" and "protocols" have subcommands
				return currentWord.equalsIgnoreCase("interfaces") ||
				       currentWord.equalsIgnoreCase("protocols");
			} else if (words.length == 3 && words[1].equalsIgnoreCase("interfaces")) {
				// "ethernet" has subcommands (interface name)
				return currentWord.equalsIgnoreCase("ethernet");
			} else if (words.length == 3 && words[1].equalsIgnoreCase("protocols")) {
				// "static" has subcommands
				return currentWord.equalsIgnoreCase("static");
			} else if (words.length == 4 && words[2].equalsIgnoreCase("static")) {
				// "route" expects a destination
				return currentWord.equalsIgnoreCase("route");
			} else if (words.length == 6 && words[3].equalsIgnoreCase("route")) {
				// "next-hop" and "interface" expect values
				return currentWord.equalsIgnoreCase("next-hop") ||
				       currentWord.equalsIgnoreCase("interface");
			}
			// Note: "address", "disable", "distance" are terminal - they don't have subcommands
		}
		return false;
	}

	private void completeOperationalMode(String[] words, String currentWord, List<Candidate> candidates) {
		// Top-level commands
		if (words.length <= 1) {
			addCandidateIfMatches(candidates, "configure", "Enter configuration mode", currentWord);
			addCandidateIfMatches(candidates, "show", "Show information", currentWord);
		} else if (words[0].equalsIgnoreCase("show")) {
			// 'show' commands
			if (words.length == 2) {
				addCandidateIfMatches(candidates, "ip", "Show IP information", currentWord);
				addCandidateIfMatches(candidates, "interfaces", "Show interface information", currentWord);
				addCandidateIfMatches(candidates, "configuration", "Show configuration", currentWord);
			}
		} else if (words.length == 3 && words[1].equalsIgnoreCase("ip")) {
			addCandidateIfMatches(candidates, "route", "Show IP routing table", currentWord);
		}
	}

	private void completeConfigurationMode(String[] words, String currentWord, List<Candidate> candidates) {
		if (words.length <= 1) {
			addCandidateIfMatches(candidates, "set", "Add or modify configuration", currentWord);
			addCandidateIfMatches(candidates, "delete", "Remove configuration", currentWord);
			addCandidateIfMatches(candidates, "show", "Show current configuration", currentWord);
			addCandidateIfMatches(candidates, "commit", "Apply configuration changes", currentWord);
			addCandidateIfMatches(candidates, "exit", "Exit configuration mode", currentWord);
		} else if (words[0].equalsIgnoreCase("set") || words[0].equalsIgnoreCase("delete")) {
			completeSetDeleteCommand(words, currentWord, candidates);
		} else if (words[0].equalsIgnoreCase("show")) {
			if (words.length == 2) {
				addCandidateIfMatches(candidates, "configuration", "Show current configuration", currentWord);
			}
		}
	}

	private void completeSetDeleteCommand(String[] words, String currentWord, List<Candidate> candidates) {
		if (words.length == 2) {
			addCandidateIfMatches(candidates, "interfaces", "Configure interfaces", currentWord);
			addCandidateIfMatches(candidates, "protocols", "Configure protocols", currentWord);
		} else if (words.length == 3 && words[1].equalsIgnoreCase("interfaces")) {
			addCandidateIfMatches(candidates, "ethernet", "Configure Ethernet interface", currentWord);
		} else if (words.length == 4 && words[1].equalsIgnoreCase("interfaces") && words[2].equalsIgnoreCase("ethernet")) {
			for (RouterInterface iface: router.getInterfaces()) {
				addCandidateIfMatches(candidates, iface.getInterfaceName(), "Interface " + iface.getInterfaceName(), currentWord);
			}
		} else if (words.length == 5 && words[1].equalsIgnoreCase("interfaces")) {
			// After interface name (e.g., "set interfaces ethernet eth0 ...")
			if (currentWord.isEmpty()) {
				// Show hints for what can be configured
				addCandidateIfMatches(candidates, "address", "Set IP address", currentWord);
				addCandidateIfMatches(candidates, "disable", "Disable interface", currentWord);
			} else {
				addCandidateIfMatches(candidates, "address", "Set IP address", currentWord);
				addCandidateIfMatches(candidates, "disable", "Disable interface", currentWord);
			}
		} else if (words.length == 6 && words[1].equalsIgnoreCase("interfaces") && words[4].equalsIgnoreCase("address")) {
			// After "address" keyword - user needs to enter IP address
			if (currentWord.isEmpty()) {
				// Show hint about IP address format
				candidates.add(new Candidate("<x.x.x.x/prefix>", "<x.x.x.x/prefix>", null,
					"Enter IP address with prefix (e.g., 192.168.1.1/24)", null, null, false));
			}
		} else if (words.length == 3 && words[1].equalsIgnoreCase("protocols")) {
			addCandidateIfMatches(candidates, "static", "Static routing", currentWord);
		} else if (words.length == 4 && words[1].equalsIgnoreCase("protocols") && words[2].equalsIgnoreCase("static")) {
			addCandidateIfMatches(candidates, "route", "Configure static route", currentWord);
		} else if (words.length == 5 && words[1].equalsIgnoreCase("protocols") && words[3].equalsIgnoreCase("route")) {
			// After "route" keyword - user needs to enter destination network
			if (currentWord.isEmpty()) {
				// Show hint about destination network format
				candidates.add(new Candidate("<x.x.x.x/prefix>", "<x.x.x.x/prefix>", null,
					"Enter destination network (e.g., 192.168.1.0/24)", null, null, false));
			}
		} else if (words.length == 6 && words[1].equalsIgnoreCase("protocols") && words[3].equalsIgnoreCase("route")) {
			// After destination network - show next-hop or interface options
			addCandidateIfMatches(candidates, "next-hop", "Specify next-hop IP address", currentWord);
			addCandidateIfMatches(candidates, "interface", "Specify outgoing interface", currentWord);
		} else if (words.length == 7 && words[5].equalsIgnoreCase("next-hop")) {
			// After "next-hop" keyword - user needs to enter next-hop IP
			if (currentWord.isEmpty()) {
				candidates.add(new Candidate("<x.x.x.x>", "<x.x.x.x>", null,
					"Enter next-hop IP address (e.g., 192.168.1.254)", null, null, false));
			}
		} else if (words.length == 7 && words[5].equalsIgnoreCase("interface")) {
			// After "interface" keyword - show available interfaces
			for (RouterInterface iface: router.getInterfaces()) {
				addCandidateIfMatches(candidates, iface.getInterfaceName(), "Interface " + iface.getInterfaceName(), currentWord);
			}
		} else if (words.length == 8 && (words[5].equalsIgnoreCase("next-hop") || words[5].equalsIgnoreCase("interface"))) {
			// After next-hop IP or interface name - show optional parameters
			addCandidateIfMatches(candidates, "distance", "Set administrative distance", currentWord);
			addCandidateIfMatches(candidates, "disable", "Disable route", currentWord);
		} else if (words.length == 9 && words[7].equalsIgnoreCase("distance")) {
			// After "distance" keyword - user needs to enter distance value
			if (currentWord.isEmpty()) {
				candidates.add(new Candidate("<1-255>", "<1-255>", null,
					"Enter administrative distance (1-255, default: 1)", null, null, false));
			}
		}
	}

	/**
	 * Adds a candidate only if it starts with the current word (case-insensitive).
	 *
	 * @param candidates List to add the candidate to
	 * @param value The completion value
	 * @param description Description of the completion
	 * @param currentWord The word currently being typed by the user
	 */
	private void addCandidateIfMatches(List<Candidate> candidates, String value, String description, String currentWord) {
		// If currentWord is empty or value starts with currentWord (case-insensitive), add it
		if (currentWord == null || currentWord.isEmpty() ||
		    value.toLowerCase().startsWith(currentWord.toLowerCase())) {
			candidates.add(new Candidate(value, value, null, description, null, null, true));
		}
	}
}
