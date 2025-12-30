package org.uj.routingemulator.switching;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a network switch device.
 * <p>
 * Switches are simplified L2 devices with multiple ports for connecting network devices.
 * This implementation focuses on physical connectivity rather than MAC learning or VLANs.
 */
@Getter
@Setter
public class Switch {
	private String name;
	private List<SwitchPort> ports;

	/**
	 * Creates a switch with specified name and ports.
	 *
	 * @param name the name of the switch
	 * @param ports the list of switch ports
	 */
	public Switch(String name, List<SwitchPort> ports) {
		this.name = name;
		this.ports = ports;
	}

	/**
	 * Creates a switch with specified name and empty port list.
	 *
	 * @param name the name of the switch
	 */
	public Switch(String name) {
		this.name = name;
		this.ports = new ArrayList<>();
	}

	/**
	 * Adds a port to this switch.
	 *
	 * @param port the port to add
	 */
	public void addPort(SwitchPort port) {
		this.ports.add(port);
	}

	/**
	 * Removes a port from this switch.
	 *
	 * @param port the port to remove
	 */
	public void removePort(SwitchPort port) {
		this.ports.remove(port);
	}

	/**
	 * Checks if this switch contains the specified port.
	 *
	 * @param port the port to check
	 * @return true if the port exists on this switch
	 */
	public boolean containsPort(SwitchPort port) {
		return this.ports.contains(port);
	}
}
