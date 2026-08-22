package org.uj.routingemulator.common.topology;

import java.util.UUID;

public record DeviceId(String id) {
	public static DeviceId generate() {
		return new DeviceId(UUID.randomUUID().toString());
	}
}