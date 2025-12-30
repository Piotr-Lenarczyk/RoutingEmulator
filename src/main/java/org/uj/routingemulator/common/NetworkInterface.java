package org.uj.routingemulator.common;

/**
 * Base interface for all network interfaces in the system.
 * <p>
 * Network interfaces represent physical or logical network ports on devices
 * (routers, switches, hosts). All interfaces have a name, subnet configuration,
 * and MAC address.
 * <p>
 * Implementations include:
 * <ul>
 *   <li>{@link org.uj.routingemulator.router.RouterInterface} - Router network interface</li>
 *   <li>{@link org.uj.routingemulator.switching.SwitchPort} - Switch port</li>
 *   <li>{@link org.uj.routingemulator.host.HostInterface} - Host network interface</li>
 * </ul>
 */
public interface NetworkInterface {
	/**
	 * Gets the name of this interface.
	 *
	 * @return interface name (e.g., "eth0", "GigabitEthernet0/1")
	 */
	String getInterfaceName();

	/**
	 * Sets the name of this interface.
	 *
	 * @param interfaceName the new interface name
	 */
	void setInterfaceName(String interfaceName);

	/**
	 * Gets the subnet configuration of this interface.
	 *
	 * @return subnet configuration, or null if not configured
	 */
	Subnet getSubnet();

	/**
	 * Sets the subnet configuration of this interface.
	 *
	 * @param subnet the subnet configuration
	 */
	void setSubnet(Subnet subnet);

	/**
	 * Gets the MAC address of this interface.
	 *
	 * @return MAC address
	 */
	MacAddress getMacAddress();

	/**
	 * Sets the MAC address of this interface.
	 *
	 * @param macAddress the new MAC address
	 */
	void setMacAddress(MacAddress macAddress);
}
