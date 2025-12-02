package org.uj.routingemulator.router;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;

@Getter
@EqualsAndHashCode(exclude = "isDisabled")
public class StaticRoutingEntry {
	private final Subnet subnet;
	private final RouterInterface routerInterface;
	private final IPAddress nextHop;
	private final int administrativeDistance;
	private boolean isDisabled;

	// IPv4 Next-Hop Routes
	public StaticRoutingEntry(Subnet subnet, IPAddress nextHop) {
		this.subnet = subnet;
		this.nextHop = nextHop;
		this.isDisabled = false;
		this.routerInterface = null;
		this.administrativeDistance = 1;
	}

	public StaticRoutingEntry(Subnet subnet, IPAddress nextHop, int administrativeDistance) {
		this.subnet = subnet;
		this.nextHop = nextHop;
		this.isDisabled = false;
		this.routerInterface = null;
		validateAdministrativeDistance(administrativeDistance);
		this.administrativeDistance = administrativeDistance;
	}

	// IPv4 Interface Routes
	public StaticRoutingEntry(Subnet subnet, RouterInterface routerInterface) {
		this.subnet = subnet;
		this.routerInterface = routerInterface;
		this.isDisabled = false;
		this.nextHop = null;
		this.administrativeDistance = 1;
	}

	public StaticRoutingEntry(Subnet subnet, RouterInterface routerInterface, int administrativeDistance) {
		this.subnet = subnet;
		this.routerInterface = routerInterface;
		this.isDisabled = false;
		this.nextHop = null;
		validateAdministrativeDistance(administrativeDistance);
		this.administrativeDistance = administrativeDistance;
	}

	public void disable() {
		this.isDisabled = true;
	}

	/**
	 * Re-enables a previously disabled routing entry.
	 */
	public void enable() {
		this.isDisabled = false;
	}

	/**
	 * Validates that administrative distance is within the valid range (1-255).
	 *
	 * @param administrativeDistance Administrative distance to validate
	 * @throws RuntimeException if the value is outside the valid range
	 */
	private void validateAdministrativeDistance(int administrativeDistance) {
		if (administrativeDistance < 1 || administrativeDistance > 255) {
			throw new RuntimeException("Administrative distance must be between 1 and 255. Provided: " + administrativeDistance);
		}
	}
}
