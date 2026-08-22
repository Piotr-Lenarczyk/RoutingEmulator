package org.uj.routingemulator.common.topology;

import java.util.UUID;

public record ConnectionId(String id) {
	public static ConnectionId generate() {
		return new ConnectionId(UUID.randomUUID().toString());
	}
}