package org.uj.routingemulator.common.topology;

import java.util.List;

public interface Device {
	DeviceId getId();

	String getDeviceName();

	List<? extends NetworkInterface> getInterfaces();
}