package org.uj.routingemulator.host;

import lombok.Data;

@Data
public class Host {
	private String hostname;
	private HostInterface hostInterface;

	public Host(String hostname, HostInterface hostInterface) {
		this.hostname = hostname;
		this.hostInterface = hostInterface;
	}
}
