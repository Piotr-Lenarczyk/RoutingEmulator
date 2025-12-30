package org.uj.routingemulator.host;

import lombok.Data;

/**
 * Represents an end-host device in the network.
 * <p>
 * Hosts are simplified network endpoints with a single network interface.
 * They can send and receive traffic but do not forward packets.
 */
@Data
public class Host {
	private String hostname;
	private HostInterface hostInterface;

	/**
	 * Creates a new host with specified hostname and network interface.
	 *
	 * @param hostname the name of the host
	 * @param hostInterface the network interface configuration
	 */
	public Host(String hostname, HostInterface hostInterface) {
		this.hostname = hostname;
		this.hostInterface = hostInterface;
	}
}
