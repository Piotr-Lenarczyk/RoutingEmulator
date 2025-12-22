package org.uj.routingemulator.router;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.uj.routingemulator.common.MacAddress;
import org.uj.routingemulator.common.NetworkInterface;
import org.uj.routingemulator.common.Subnet;

@Setter
@Getter
@EqualsAndHashCode
@ToString
public class RouterInterface implements NetworkInterface {
	private String interfaceName;
	private Subnet subnet;
	private MacAddress macAddress;
	private String description;
	private String vrf = "default";
	private int mtu;
	private InterfaceStatus status;

	public RouterInterface(String interfaceName) {
		this.interfaceName = interfaceName;
		this.subnet = null;
		this.macAddress = new MacAddress();
		this.description = null;
		if (interfaceName.startsWith("eth")) {
			this.mtu = 1500;
		} else if (interfaceName.startsWith("lo")) {
			this.mtu = 65536;
		}
		this.status = InterfaceStatus.fromChars('u', 'u');
	}

	public RouterInterface(String interfaceName, Subnet subnet, MacAddress macAddress, int mtu, InterfaceStatus status) {
		this.interfaceName = interfaceName;
		this.subnet = subnet;
		this.macAddress = macAddress;
		this.mtu = mtu;
		this.status = status;
	}

	public RouterInterface(String interfaceName, Subnet subnet, MacAddress macAddress, String vrf, int mtu, InterfaceStatus status) {
		this.interfaceName = interfaceName;
		this.subnet = subnet;
		this.macAddress = macAddress;
		this.vrf = vrf;
		this.mtu = mtu;
		this.status = status;
	}

	/**
	 * Copy constructor for creating a deep copy of RouterInterface.
	 * @param other The RouterInterface to copy
	 */
	public RouterInterface(RouterInterface other) {
		this.interfaceName = other.interfaceName;
		this.subnet = other.subnet; // Subnet is immutable
		this.macAddress = other.macAddress; // MacAddress is immutable
		this.description = other.description;
		this.vrf = other.vrf;
		this.mtu = other.mtu;
		this.status = other.status; // InterfaceStatus is immutable
	}

	/**
	 * Administratively disables the interface.
	 *
	 * @throws RuntimeException if the interface is already disabled
	 */
	public void disable() {
		if (this.status.equals(new InterfaceStatus(AdminState.ADMIN_DOWN, LinkState.DOWN))) {
			throw new RuntimeException("Configuration path: [interfaces ethernet %s disable] already exists".formatted(this.interfaceName));
		}
		this.status = new InterfaceStatus(AdminState.ADMIN_DOWN, LinkState.DOWN);
	}

	/**
	 * Administratively enables the interface.
	 *
	 * @throws RuntimeException if the interface is already enabled
	 */
	public void enable() {
		if (this.status.equals(new InterfaceStatus(AdminState.UP, LinkState.UP))) {
			throw new RuntimeException("Nothing to delete (the specified node does not exist)");
		}
		this.status = new InterfaceStatus(AdminState.UP, LinkState.UP);
	}
}
