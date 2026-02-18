package org.uj.routingemulator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.cli.RouterCLI;
import org.uj.routingemulator.switching.Switch;
import org.uj.routingemulator.switching.SwitchPort;

import java.io.IOException;
import java.util.List;

/**
 * Main application class for the Network Routing Emulator.
 * Provides both GUI and CLI interfaces for network topology management and router configuration.
 */
public class Main extends Application {

	@Override
	public void start(Stage stage) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("network-topology-view.fxml"));
		Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
		stage.setTitle("Network Routing Emulator - VyOS");
		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args) {
		// Check if CLI mode is requested
		if (args.length > 0 && args[0].equals("--cli")) {
			runCLIMode();
		} else {
			// Launch GUI
			launch();
		}
	}

	/**
	 * Runs the application in CLI-only mode for testing purposes.
	 */
	private static void runCLIMode() {
		NetworkTopology topology = new NetworkTopology();

		Host h1 = new Host("PC1", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 1, 1), new SubnetMask(24)), new IPAddress(192, 168, 1, 254)));
		Host h2 = new Host("PC2", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 2, 1), new SubnetMask(24)), new IPAddress(192, 168, 2, 254)));
		Host h3 = new Host("PC3", new HostInterface("Ethernet0", new Subnet(new IPAddress(192, 168, 3, 1), new SubnetMask(24)), new IPAddress(192, 168, 3, 254)));
		topology.addHost(h1);
		topology.addHost(h2);
		topology.addHost(h3);
		Switch sw1 = new Switch("SW1", List.of(new SwitchPort("GigabitEthernet0/1"), new SwitchPort("GigabitEthernet0/2"), new SwitchPort("GigabitEthernet0/3"), new SwitchPort("GigabitEthernet0/4")));
		Switch sw2 = new Switch("SW2", List.of(new SwitchPort("GigabitEthernet0/1"), new SwitchPort("GigabitEthernet0/2"), new SwitchPort("GigabitEthernet0/3"), new SwitchPort("GigabitEthernet0/4")));
		Switch sw3 = new Switch("SW3", List.of(new SwitchPort("GigabitEthernet0/1"), new SwitchPort("GigabitEthernet0/2"), new SwitchPort("GigabitEthernet0/3"), new SwitchPort("GigabitEthernet0/4")));
		topology.addSwitch(sw1);
		topology.addSwitch(sw2);
		topology.addSwitch(sw3);
		Router r1 = new Router("R1", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r2 = new Router("R2", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		Router r3 = new Router("R3", List.of(new RouterInterface("eth0"), new RouterInterface("eth1"), new RouterInterface("eth2")));
		topology.addRouter(r1);
		topology.addRouter(r2);
		topology.addRouter(r3);
		topology.addConnection(new Connection(h1.getHostInterface(), sw1.getPorts().getFirst()));
		topology.addConnection(new Connection(h2.getHostInterface(), sw2.getPorts().getFirst()));
		topology.addConnection(new Connection(h3.getHostInterface(), sw3.getPorts().getFirst()));
		topology.addConnection(new Connection(sw1.getPorts().get(1), r1.getInterfaces().getFirst()));
		topology.addConnection(new Connection(sw2.getPorts().get(1), r2.getInterfaces().getFirst()));
		topology.addConnection(new Connection(sw3.getPorts().get(1), r3.getInterfaces().getFirst()));
		topology.addConnection(new Connection(r1.getInterfaces().get(1), r2.getInterfaces().get(1)));
		topology.addConnection(new Connection(r2.getInterfaces().get(2), r3.getInterfaces().get(1)));
		topology.addConnection(new Connection(r3.getInterfaces().get(2), r1.getInterfaces().get(2)));
		System.out.println(topology.visualize());
		topology.removeHost(h1);
		System.out.println(topology.visualize());
		RouterCLI cli = new RouterCLI(r1);
		cli.start();
	}
}

