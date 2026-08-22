package org.uj.routingemulator.host;

import lombok.Data;
import org.uj.routingemulator.common.topology.Device;
import org.uj.routingemulator.common.topology.DeviceId;
import org.uj.routingemulator.common.topology.NetworkInterface;

import java.util.List;
import java.util.logging.Logger;

/** Hosts are simplified network endpoints with a single network interface.
 * They can send and receive traffic but do not forward packets. */
@Data
public class Host implements Device {
	private static final Logger logger = Logger.getLogger(Host.class.getName());

	private final DeviceId id = DeviceId.generate();
	private String hostname;
	private HostInterface hostInterface;

	/**
	 * Creates a new host with specified hostname and network interface.
	 * @param hostname the name of the host
	 * @param hostInterface the network interface configuration
	 */
	public Host(String hostname, HostInterface hostInterface) {
		this.hostname = hostname;
		this.hostInterface = hostInterface;
		logger.fine("Creating new host %s with interface %s".formatted(hostname, hostInterface));
	}

	@Override
	public DeviceId getId() {
		return id;
	}

	@Override
	public String getDeviceName() {
		return hostname;
	}

	@Override
	public List<? extends NetworkInterface> getInterfaces() {
		return hostInterface != null ? List.of(hostInterface) : List.of();
	}
}