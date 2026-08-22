package org.uj.routingemulator.switching;

import lombok.Getter;
import lombok.Setter;
import org.uj.routingemulator.common.topology.Device;
import org.uj.routingemulator.common.topology.DeviceId;
import org.uj.routingemulator.common.topology.NetworkInterface;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a network switch device.
 * <p>
 * Switches are simplified L2 devices with multiple ports for connecting network devices.
 * This implementation focuses on physical connectivity rather than MAC learning or VLANs.
 */
@Getter
@Setter
public class Switch implements Device {
	private static final Logger logger = Logger.getLogger(Switch.class.getName());

	private final DeviceId id = DeviceId.generate();
	private String name;
	private LinkedList<SwitchPort> ports;

	/**
	 * Creates a switch with specified name and ports.
	 * @param name  the name of the switch
	 * @param ports the list of switch ports
	 */
	public Switch(String name, List<SwitchPort> ports) {
		this.name = name;
		this.ports = new LinkedList<>(ports);
		logger.fine("Creating new switch: %s with ports: %s".formatted(name, ports));
	}

	/**
	 * Creates a switch with specified name and empty port list.
	 * Default behavior creates two ports (GigabitEthernet0/1 and GigabitEthernet0/2)
	 * to make simple topologies easier to build in tests.
	 * @param name the name of the switch
	 */
	public Switch(String name) {
		this.name = name;
		this.ports = new LinkedList<>();
		// Add two default ports so tests that expect ports to exist work
		this.ports.add(new SwitchPort("GigabitEthernet0/1"));
		this.ports.add(new SwitchPort("GigabitEthernet0/2"));
		logger.fine("Creating new switch: %s with default ports: %s".formatted(name, ports));
	}

	@Override
	public DeviceId getId() {
		return id;
	}

	@Override
	public String getDeviceName() {
		return name;
	}

	@Override
	public List<? extends NetworkInterface> getInterfaces() {
		return ports;
	}

	/**
	 * Checks if this switch contains the specified port.
	 * @param port the port to check
	 * @return true if the port exists on this switch
	 */
	public boolean containsPort(SwitchPort port) {
		return this.ports.contains(port);
	}
}