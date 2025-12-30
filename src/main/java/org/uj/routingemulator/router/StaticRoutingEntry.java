package org.uj.routingemulator.router;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;

/**
 * Represents a static routing entry in the routing table.
 * <p>
 * A static route defines how to reach a specific network destination.
 * Routes can be configured with either:
 * <ul>
 *   <li>Next-hop IP address - forward packets to this IP</li>
 *   <li>Exit interface - forward packets out this interface</li>
 * </ul>
 * <p>
 * Each route has an administrative distance (default 1) used for route selection
 * when multiple routes to the same destination exist.
 */
@Getter
@EqualsAndHashCode(exclude = "isDisabled")
public class StaticRoutingEntry {
	private final Subnet subnet;
	private final RouterInterface routerInterface;
	private final IPAddress nextHop;
	private final int administrativeDistance;
	private boolean isDisabled;

	/**
	 * Creates a next-hop based static route with default administrative distance (1).
	 *
	 * @param subnet destination network
	 * @param nextHop IP address of next hop router
	 */
	public StaticRoutingEntry(Subnet subnet, IPAddress nextHop) {
		this.subnet = subnet;
		this.nextHop = nextHop;
		this.isDisabled = false;
		this.routerInterface = null;
		this.administrativeDistance = 1;
	}

	/**
	 * Creates a next-hop based static route with specified administrative distance.
	 *
	 * @param subnet destination network
	 * @param nextHop IP address of next hop router
	 * @param administrativeDistance metric for route selection (1-255)
	 */
	public StaticRoutingEntry(Subnet subnet, IPAddress nextHop, int administrativeDistance) {
		this.subnet = subnet;
		this.nextHop = nextHop;
		this.isDisabled = false;
		this.routerInterface = null;
		validateAdministrativeDistance(administrativeDistance);
		this.administrativeDistance = administrativeDistance;
	}

	/**
	 * Creates an interface-based static route with default administrative distance (1).
	 *
	 * @param subnet destination network
	 * @param routerInterface exit interface for this route
	 */
	public StaticRoutingEntry(Subnet subnet, RouterInterface routerInterface) {
		this.subnet = subnet;
		this.routerInterface = routerInterface;
		this.isDisabled = false;
		this.nextHop = null;
		this.administrativeDistance = 1;
	}

	/**
	 * Creates an interface-based static route with specified administrative distance.
	 *
	 * @param subnet destination network
	 * @param routerInterface exit interface for this route
	 * @param administrativeDistance metric for route selection (1-255)
	 */
	public StaticRoutingEntry(Subnet subnet, RouterInterface routerInterface, int administrativeDistance) {
		this.subnet = subnet;
		this.routerInterface = routerInterface;
		this.isDisabled = false;
		this.nextHop = null;
		validateAdministrativeDistance(administrativeDistance);
		this.administrativeDistance = administrativeDistance;
	}

	/**
	 * Copy constructor for creating a deep copy of a StaticRoutingEntry.
	 * Note: RouterInterface is not deep-copied as it may be intentionally shared.
	 * Subnet and IPAddress are immutable so no copying is needed.
	 *
	 * @param other StaticRoutingEntry to copy
	 */
	public StaticRoutingEntry(StaticRoutingEntry other) {
		this.subnet = other.subnet; // Immutable
		this.nextHop = other.nextHop; // Immutable
		this.routerInterface = other.routerInterface; // Intentionally shared reference
		this.administrativeDistance = other.administrativeDistance;
		this.isDisabled = other.isDisabled;
	}

	/**
	 * Disables this routing entry.
	 * Disabled routes remain in the routing table but are not used for forwarding.
	 */
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
