package org.uj.routingemulator.common;

import lombok.Data;

@Data
public class Packet {
	private final IPAddress source;
	private final IPAddress destination;
	private final PacketType type;
	private int ttl;

	public boolean decrementTTL() {
		return --ttl > 0;
	}

	public enum PacketType {
		ICMP_ECHO_REQUEST,
		ICMP_ECHO_REPLY,
		ICMP_DESTINATION_UNREACHABLE
	}
}
