package org.uj.routingemulator.switching;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Switch {
	private String name;
	private List<SwitchPort> ports;

	public Switch(String name, List<SwitchPort> ports) {
		this.name = name;
		this.ports = ports;
	}

	public Switch(String name) {
		this.name = name;
		this.ports = new ArrayList<>();
	}

	public void addPort(SwitchPort port) {
		this.ports.add(port);
	}

	public void removePort(SwitchPort port) {
		this.ports.remove(port);
	}

	public boolean containsPort(SwitchPort port) {
		return this.ports.contains(port);
	}
}
