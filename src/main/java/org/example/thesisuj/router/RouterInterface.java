package org.example.thesisuj.router;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.thesisuj.common.MacAddress;
import org.example.thesisuj.common.Subnet;

@Setter
@Getter
@EqualsAndHashCode
@ToString
public class RouterInterface {
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
	 * Administratively disables the interface.
	 *
	 * @throws RuntimeException if the interface is already disabled
	 */
	public void disable() {
		if (this.status.equals(new InterfaceStatus(AdminState.ADMIN_DOWN, LinkState.DOWN))) {
			throw new RuntimeException("Configuration path: [interfaces ethernet %s disable] already exists".formatted(this.interfaceName));
		}
		this.status = new InterfaceStatus(AdminState.ADMIN_DOWN, LinkState.DOWN);
		System.out.println("[edit]");
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
		System.out.println("[edit]");
	}
}
