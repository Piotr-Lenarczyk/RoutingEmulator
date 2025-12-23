package org.uj.routingemulator.gui;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import org.uj.routingemulator.common.Connection;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.NetworkInterface;
import org.uj.routingemulator.common.NetworkTopology;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.switching.Switch;
import org.uj.routingemulator.switching.SwitchPort;

import java.util.*;

/**
 * Controller for the network topology GUI.
 * Manages the visual representation of the network and user interactions.
 */
public class NetworkTopologyController {

	@FXML
	private Pane canvasPane;

	@FXML
	private ListView<String> deviceListView;

	@FXML
	private Button addRouterButton;

	@FXML
	private Button addSwitchButton;

	@FXML
	private Button addHostButton;

	@FXML
	private Button removeDeviceButton;

	@FXML
	private Button addConnectionButton;

	@FXML
	private Button removeConnectionButton;

	private NetworkTopology topology;
	private Map<Object, DeviceNode> deviceNodes; // Device (Router/Switch/Host) -> Visual Node
	private Map<Connection, Line> connectionLines; // Connection -> Visual Line
	private DeviceNode selectedNode;
	private DeviceNode connectionStartNode;

	/**
	 * Initializes the controller.
	 * Sets up the network topology and event handlers.
	 */
	@FXML
	public void initialize() {
		topology = new NetworkTopology();
		deviceNodes = new HashMap<>();
		connectionLines = new HashMap<>();

		updateDeviceList();
		setupEventHandlers();
	}

	/**
	 * Sets up event handlers for buttons and canvas interactions.
	 */
	private void setupEventHandlers() {
		addRouterButton.setOnAction(e -> addRouter());
		addSwitchButton.setOnAction(e -> addSwitch());
		addHostButton.setOnAction(e -> addHost());
		removeDeviceButton.setOnAction(e -> removeSelectedDevice());
		addConnectionButton.setOnAction(e -> startConnectionMode());
		removeConnectionButton.setOnAction(e -> removeConnection());

		canvasPane.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY && connectionStartNode == null) {
				selectedNode = null;
				updateSelection();
			}
		});
	}

	/**
	 * Adds a new router to the topology.
	 */
	private void addRouter() {
		TextInputDialog dialog = new TextInputDialog("R" + (topology.getRouters().size() + 1));
		dialog.setTitle("Add Router");
		dialog.setHeaderText("Add a new router");
		dialog.setContentText("Router name:");

		Optional<String> result = dialog.showAndWait();
		result.ifPresent(name -> {
			// Ask for number of interfaces
			TextInputDialog interfaceDialog = new TextInputDialog("3");
			interfaceDialog.setTitle("Router Interfaces");
			interfaceDialog.setHeaderText("Configure router interfaces");
			interfaceDialog.setContentText("Number of interfaces:");

			Optional<String> interfaceResult = interfaceDialog.showAndWait();
			interfaceResult.ifPresent(numStr -> {
				try {
					int numInterfaces = Integer.parseInt(numStr);
					if (numInterfaces < 1 || numInterfaces > 10) {
						showError("Number of interfaces must be between 1 and 10");
						return;
					}

					List<RouterInterface> interfaces = new ArrayList<>();
					for (int i = 0; i < numInterfaces; i++) {
						interfaces.add(new RouterInterface("eth" + i));
					}

					Router router = new Router(name, interfaces);
					topology.addRouter(router);

					// Place router at random position
					double x = 100 + Math.random() * (canvasPane.getWidth() - 200);
					double y = 100 + Math.random() * (canvasPane.getHeight() - 200);
					addDeviceNode(router, x, y, Color.LIGHTBLUE, "R");

					updateDeviceList();
				} catch (NumberFormatException ex) {
					showError("Invalid number of interfaces");
				}
			});
		});
	}

	/**
	 * Adds a new switch to the topology.
	 */
	private void addSwitch() {
		TextInputDialog dialog = new TextInputDialog("SW" + (topology.getSwitches().size() + 1));
		dialog.setTitle("Add Switch");
		dialog.setHeaderText("Add a new switch");
		dialog.setContentText("Switch name:");

		Optional<String> result = dialog.showAndWait();
		result.ifPresent(name -> {
			// Ask for number of ports
			TextInputDialog portDialog = new TextInputDialog("4");
			portDialog.setTitle("Switch Ports");
			portDialog.setHeaderText("Configure switch ports");
			portDialog.setContentText("Number of ports:");

			Optional<String> portResult = portDialog.showAndWait();
			portResult.ifPresent(numStr -> {
				try {
					int numPorts = Integer.parseInt(numStr);
					if (numPorts < 1 || numPorts > 48) {
						showError("Number of ports must be between 1 and 48");
						return;
					}

					List<SwitchPort> ports = new ArrayList<>();
					for (int i = 0; i < numPorts; i++) {
						ports.add(new SwitchPort("GigabitEthernet0/" + (i + 1)));
					}

					Switch sw = new Switch(name, ports);
					topology.addSwitch(sw);

					// Place switch at random position
					double x = 100 + Math.random() * (canvasPane.getWidth() - 200);
					double y = 100 + Math.random() * (canvasPane.getHeight() - 200);
					addDeviceNode(sw, x, y, Color.LIGHTGREEN, "SW");

					updateDeviceList();
				} catch (NumberFormatException ex) {
					showError("Invalid number of ports");
				}
			});
		});
	}

	/**
	 * Adds a new host to the topology.
	 */
	private void addHost() {
		TextInputDialog dialog = new TextInputDialog("PC" + (topology.getHosts().size() + 1));
		dialog.setTitle("Add Host");
		dialog.setHeaderText("Add a new host");
		dialog.setContentText("Host name:");

		Optional<String> result = dialog.showAndWait();
		result.ifPresent(name -> {
			// Ask for IP address
			TextInputDialog ipDialog = new TextInputDialog("192.168.1.1");
			ipDialog.setTitle("Host Configuration");
			ipDialog.setHeaderText("Configure host IP address");
			ipDialog.setContentText("IP Address:");

			Optional<String> ipResult = ipDialog.showAndWait();
			ipResult.ifPresent(ipStr -> {
				try {
					String[] parts = ipStr.split("\\.");
					if (parts.length != 4) {
						showError("Invalid IP address format");
						return;
					}

					IPAddress ip = new IPAddress(
						Integer.parseInt(parts[0]),
						Integer.parseInt(parts[1]),
						Integer.parseInt(parts[2]),
						Integer.parseInt(parts[3])
					);

					// Ask for subnet mask
					TextInputDialog maskDialog = new TextInputDialog("24");
					maskDialog.setTitle("Host Configuration");
					maskDialog.setHeaderText("Configure subnet mask");
					maskDialog.setContentText("Subnet mask (CIDR):");

					Optional<String> maskResult = maskDialog.showAndWait();
					maskResult.ifPresent(maskStr -> {
						try {
							int maskLength = Integer.parseInt(maskStr);
							SubnetMask mask = new SubnetMask(maskLength);

							// Ask for default gateway
							TextInputDialog gwDialog = new TextInputDialog("192.168.1.254");
							gwDialog.setTitle("Host Configuration");
							gwDialog.setHeaderText("Configure default gateway");
							gwDialog.setContentText("Default gateway:");

							Optional<String> gwResult = gwDialog.showAndWait();
							gwResult.ifPresent(gwStr -> {
								try {
									String[] gwParts = gwStr.split("\\.");
									if (gwParts.length != 4) {
										showError("Invalid gateway IP address format");
										return;
									}

									IPAddress gateway = new IPAddress(
										Integer.parseInt(gwParts[0]),
										Integer.parseInt(gwParts[1]),
										Integer.parseInt(gwParts[2]),
										Integer.parseInt(gwParts[3])
									);

									HostInterface hostInterface = new HostInterface(
										"Ethernet0",
										new Subnet(ip, mask),
										gateway
									);

									Host host = new Host(name, hostInterface);
									topology.addHost(host);

									// Place host at random position
									double x = 100 + Math.random() * (canvasPane.getWidth() - 200);
									double y = 100 + Math.random() * (canvasPane.getHeight() - 200);
									addDeviceNode(host, x, y, Color.LIGHTYELLOW, "H");

									updateDeviceList();
								} catch (Exception ex) {
									showError("Invalid gateway IP address: " + ex.getMessage());
								}
							});
						} catch (Exception ex) {
							showError("Invalid subnet mask: " + ex.getMessage());
						}
					});
				} catch (Exception ex) {
					showError("Invalid IP address: " + ex.getMessage());
				}
			});
		});
	}

	/**
	 * Removes the currently selected device from the topology.
	 */
	private void removeSelectedDevice() {
		if (selectedNode == null) {
			showError("No device selected");
			return;
		}

		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
		confirmation.setTitle("Remove Device");
		confirmation.setHeaderText("Are you sure you want to remove this device?");
		confirmation.setContentText("All connections to this device will also be removed.");

		Optional<ButtonType> result = confirmation.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			Object device = selectedNode.device;

			// Remove all connections related to this device
			List<Connection> connectionsToRemove = new ArrayList<>();
			for (Connection conn : topology.getConnections()) {
				if (isDeviceInConnection(device, conn)) {
					connectionsToRemove.add(conn);
				}
			}

			for (Connection conn : connectionsToRemove) {
				Line line = connectionLines.remove(conn);
				if (line != null) {
					canvasPane.getChildren().remove(line);
				}
			}

			// Remove device from topology
			if (device instanceof Router) {
				topology.removeRouter((Router) device);
			} else if (device instanceof Switch) {
				topology.removeSwitch((Switch) device);
			} else if (device instanceof Host) {
				topology.removeHost((Host) device);
			}

			// Remove visual node
			canvasPane.getChildren().remove(selectedNode.stackPane);
			deviceNodes.remove(device);
			selectedNode = null;

			updateDeviceList();
		}
	}

	/**
	 * Checks if a device is part of a connection.
	 *
	 * @param device the device to check
	 * @param connection the connection to check
	 * @return true if the device is part of the connection
	 */
	private boolean isDeviceInConnection(Object device, Connection connection) {
		if (device instanceof Router) {
			Router router = (Router) device;
			return router.getInterfaces().stream().anyMatch(iface ->
				iface.equals(connection.getInterfaceA()) || iface.equals(connection.getInterfaceB()));
		} else if (device instanceof Switch) {
			Switch sw = (Switch) device;
			return sw.getPorts().stream().anyMatch(port ->
				port.equals(connection.getInterfaceA()) || port.equals(connection.getInterfaceB()));
		} else if (device instanceof Host) {
			Host host = (Host) device;
			return host.getHostInterface().equals(connection.getInterfaceA()) ||
				host.getHostInterface().equals(connection.getInterfaceB());
		}
		return false;
	}

	/**
	 * Starts connection mode, allowing the user to create a connection between two devices.
	 */
	private void startConnectionMode() {
		if (selectedNode == null) {
			showError("Please select the first device for the connection");
			return;
		}

		connectionStartNode = selectedNode;
		showInfo("Now select the second device to complete the connection");
	}

	/**
	 * Removes a connection between two selected devices.
	 */
	private void removeConnection() {
		if (selectedNode == null) {
			showError("Please select a device to remove its connections");
			return;
		}

		// Find all connections related to this device
		List<Connection> relatedConnections = new ArrayList<>();
		Object device = selectedNode.device;

		for (Connection conn : topology.getConnections()) {
			if (isDeviceInConnection(device, conn)) {
				relatedConnections.add(conn);
			}
		}

		if (relatedConnections.isEmpty()) {
			showError("No connections found for this device");
			return;
		}

		// Create custom StringConverter for displaying connections
		javafx.util.StringConverter<Connection> connectionConverter = new javafx.util.StringConverter<Connection>() {
			@Override
			public String toString(Connection conn) {
				if (conn == null) return "null";
				return formatInterfaceDisplay(conn.getInterfaceA()) + " <--> " + formatInterfaceDisplay(conn.getInterfaceB());
			}

			@Override
			public Connection fromString(String string) {
				return null; // Not needed for ChoiceDialog
			}
		};

		// Create a dialog to select which connection to remove
		ChoiceDialog<Connection> dialog = new ChoiceDialog<>(relatedConnections.get(0), relatedConnections);
		dialog.setTitle("Remove Connection");
		dialog.setHeaderText("Select connection to remove");
		dialog.setContentText("Connection:");

		// Set the converter for the ComboBox inside the ChoiceDialog
		@SuppressWarnings("unchecked")
		ComboBox<Connection> comboBox = (ComboBox<Connection>) dialog.getDialogPane().lookup(".combo-box");
		if (comboBox != null) {
			comboBox.setConverter(connectionConverter);
		}

		Optional<Connection> result = dialog.showAndWait();
		result.ifPresent(conn -> {
			topology.removeConnection(conn);
			Line line = connectionLines.remove(conn);
			if (line != null) {
				canvasPane.getChildren().remove(line);
			}
		});
	}

	/**
	 * Adds a visual node for a device to the canvas.
	 *
	 * @param device the device to add
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param color the color of the node
	 * @param label the label prefix for the device
	 */
	private void addDeviceNode(Object device, double x, double y, Color color, String label) {
		String name = getDeviceName(device);

		Circle circle = new Circle(25, color);
		circle.setStroke(Color.BLACK);
		circle.setStrokeWidth(2);

		Text text = new Text(label);
		text.setStyle("-fx-font-weight: bold;");

		VBox vbox = new VBox(5);
		vbox.setAlignment(Pos.CENTER);
		vbox.getChildren().addAll(circle, new Text(name));

		StackPane stackPane = new StackPane();
		stackPane.getChildren().addAll(circle, text);
		stackPane.setLayoutX(x);
		stackPane.setLayoutY(y);

		VBox container = new VBox(5);
		container.setAlignment(Pos.CENTER);
		container.getChildren().addAll(stackPane, new Text(name));
		container.setLayoutX(x - 30);
		container.setLayoutY(y - 30);

		DeviceNode deviceNode = new DeviceNode(device, container, circle);
		deviceNodes.put(device, deviceNode);

		// Make node draggable
		final Delta dragDelta = new Delta();
		container.setOnMousePressed(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				dragDelta.x = container.getLayoutX() - e.getSceneX();
				dragDelta.y = container.getLayoutY() - e.getSceneY();
				e.consume();
			}
		});

		container.setOnMouseDragged(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				container.setLayoutX(e.getSceneX() + dragDelta.x);
				container.setLayoutY(e.getSceneY() + dragDelta.y);
				updateConnectionLines(device);
				e.consume();
			}
		});

		container.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				handleNodeClick(deviceNode);
				e.consume();
			}
		});

		canvasPane.getChildren().add(container);
	}

	/**
	 * Handles click on a device node.
	 *
	 * @param node the clicked node
	 */
	private void handleNodeClick(DeviceNode node) {
		if (connectionStartNode != null && connectionStartNode != node) {
			// Complete connection
			createConnection(connectionStartNode, node);
			connectionStartNode = null;
		} else if (node.device instanceof Router) {
			// Double-click detection for router CLI
			if (selectedNode == node) {
				openRouterCLI((Router) node.device);
			}
		}

		selectedNode = node;
		updateSelection();
	}

	/**
	 * Creates a connection between two device nodes.
	 *
	 * @param startNode the start node
	 * @param endNode the end node
	 */
	private void createConnection(DeviceNode startNode, DeviceNode endNode) {
		// Get available interfaces for both devices
		List<NetworkInterface> startInterfaces = getAvailableInterfaces(startNode.device);
		List<NetworkInterface> endInterfaces = getAvailableInterfaces(endNode.device);

		if (startInterfaces.isEmpty()) {
			showError("No available interfaces on " + getDeviceName(startNode.device));
			return;
		}

		if (endInterfaces.isEmpty()) {
			showError("No available interfaces on " + getDeviceName(endNode.device));
			return;
		}

		// Create custom StringConverter for displaying interfaces
		javafx.util.StringConverter<NetworkInterface> interfaceConverter = new javafx.util.StringConverter<NetworkInterface>() {
			@Override
			public String toString(NetworkInterface iface) {
				return formatInterfaceDisplay(iface);
			}

			@Override
			public NetworkInterface fromString(String string) {
				return null; // Not needed for ChoiceDialog
			}
		};

		// Let user select interfaces
		ChoiceDialog<NetworkInterface> startDialog = new ChoiceDialog<>(startInterfaces.get(0), startInterfaces);
		startDialog.setTitle("Select Interface");
		startDialog.setHeaderText("Select interface on " + getDeviceName(startNode.device));
		startDialog.setContentText("Interface:");
		// Set the converter for the ComboBox inside the ChoiceDialog
		@SuppressWarnings("unchecked")
		ComboBox<NetworkInterface> startComboBox = (ComboBox<NetworkInterface>) startDialog.getDialogPane().lookup(".combo-box");
		if (startComboBox != null) {
			startComboBox.setConverter(interfaceConverter);
		}

		Optional<NetworkInterface> startResult = startDialog.showAndWait();
		if (!startResult.isPresent()) {
			return;
		}

		ChoiceDialog<NetworkInterface> endDialog = new ChoiceDialog<>(endInterfaces.get(0), endInterfaces);
		endDialog.setTitle("Select Interface");
		endDialog.setHeaderText("Select interface on " + getDeviceName(endNode.device));
		endDialog.setContentText("Interface:");
		// Set the converter for the ComboBox inside the ChoiceDialog
		@SuppressWarnings("unchecked")
		ComboBox<NetworkInterface> endComboBox = (ComboBox<NetworkInterface>) endDialog.getDialogPane().lookup(".combo-box");
		if (endComboBox != null) {
			endComboBox.setConverter(interfaceConverter);
		}

		Optional<NetworkInterface> endResult = endDialog.showAndWait();
		if (!endResult.isPresent()) {
			return;
		}

		try {
			Connection connection = new Connection(startResult.get(), endResult.get());
			topology.addConnection(connection);

			// Create visual line
			Line line = new Line();
			line.setStrokeWidth(3);
			line.setStroke(Color.DARKGRAY);
			updateConnectionLine(line, startNode, endNode);

			canvasPane.getChildren().add(0, line); // Add to back
			connectionLines.put(connection, line);

		} catch (Exception ex) {
			showError("Failed to create connection: " + ex.getMessage());
		}
	}

	/**
	 * Gets available (unconnected) interfaces for a device.
	 *
	 * @param device the device
	 * @return list of available interfaces
	 */
	private List<NetworkInterface> getAvailableInterfaces(Object device) {
		List<NetworkInterface> allInterfaces = new ArrayList<>();

		if (device instanceof Router) {
			allInterfaces.addAll(((Router) device).getInterfaces());
		} else if (device instanceof Switch) {
			allInterfaces.addAll(((Switch) device).getPorts());
		} else if (device instanceof Host) {
			allInterfaces.add(((Host) device).getHostInterface());
		}

		// Filter out already connected interfaces
		List<NetworkInterface> availableInterfaces = new ArrayList<>();
		for (NetworkInterface iface : allInterfaces) {
			boolean isConnected = topology.getConnections().stream().anyMatch(conn ->
				conn.getInterfaceA().equals(iface) || conn.getInterfaceB().equals(iface));
			if (!isConnected) {
				availableInterfaces.add(iface);
			}
		}

		return availableInterfaces;
	}

	/**
	 * Updates all connection lines related to a device.
	 *
	 * @param device the device that was moved
	 */
	private void updateConnectionLines(Object device) {
		for (Map.Entry<Connection, Line> entry : connectionLines.entrySet()) {
			Connection conn = entry.getKey();
			Line line = entry.getValue();

			Object deviceA = findDevice(conn.getInterfaceA());
			Object deviceB = findDevice(conn.getInterfaceB());

			if (device.equals(deviceA) || device.equals(deviceB)) {
				DeviceNode nodeA = deviceNodes.get(deviceA);
				DeviceNode nodeB = deviceNodes.get(deviceB);
				if (nodeA != null && nodeB != null) {
					updateConnectionLine(line, nodeA, nodeB);
				}
			}
		}
	}

	/**
	 * Updates a connection line between two nodes.
	 *
	 * @param line the line to update
	 * @param nodeA the first node
	 * @param nodeB the second node
	 */
	private void updateConnectionLine(Line line, DeviceNode nodeA, DeviceNode nodeB) {
		double startX = nodeA.stackPane.getLayoutX() + 30;
		double startY = nodeA.stackPane.getLayoutY() + 30;
		double endX = nodeB.stackPane.getLayoutX() + 30;
		double endY = nodeB.stackPane.getLayoutY() + 30;

		line.setStartX(startX);
		line.setStartY(startY);
		line.setEndX(endX);
		line.setEndY(endY);
	}

	/**
	 * Finds the device that owns a network interface.
	 *
	 * @param iface the interface
	 * @return the device owning the interface, or null if not found
	 */
	private Object findDevice(NetworkInterface iface) {
		for (Router router : topology.getRouters()) {
			if (router.getInterfaces().contains(iface)) {
				return router;
			}
		}

		for (Switch sw : topology.getSwitches()) {
			if (sw.getPorts().contains(iface)) {
				return sw;
			}
		}

		for (Host host : topology.getHosts()) {
			if (host.getHostInterface().equals(iface)) {
				return host;
			}
		}

		return null;
	}

	/**
	 * Gets the display name of a device.
	 *
	 * @param device the device
	 * @return the device name
	 */
	private String getDeviceName(Object device) {
		if (device instanceof Router) {
			return ((Router) device).getName();
		} else if (device instanceof Switch) {
			return ((Switch) device).getName();
		} else if (device instanceof Host) {
			return ((Host) device).getHostname();
		}
		return "Unknown";
	}

	/**
	 * Formats a network interface for display with user-friendly information.
	 *
	 * @param iface the interface to format
	 * @return formatted string representation
	 */
	private String formatInterfaceDisplay(NetworkInterface iface) {
		if (iface == null) {
			return "null";
		}

		StringBuilder display = new StringBuilder();
		display.append(iface.getInterfaceName());

		if (iface.getSubnet() != null) {
			display.append(" (").append(iface.getSubnet().getNetworkAddress());
			display.append("/").append(iface.getSubnet().getSubnetMask().getShortMask()).append(")");
		} else {
			display.append(" (unconfigured)");
		}

		return display.toString();
	}

	/**
	 * Updates the visual selection state of nodes.
	 */
	private void updateSelection() {
		for (DeviceNode node : deviceNodes.values()) {
			node.circle.setStrokeWidth(2);
			node.circle.setStroke(Color.BLACK);
		}

		if (selectedNode != null) {
			selectedNode.circle.setStrokeWidth(4);
			selectedNode.circle.setStroke(Color.BLUE);
		}
	}

	/**
	 * Updates the device list view.
	 */
	private void updateDeviceList() {
		deviceListView.getItems().clear();

		deviceListView.getItems().add("=== Routers ===");
		for (Router router : topology.getRouters()) {
			deviceListView.getItems().add("  " + router.getName());
		}

		deviceListView.getItems().add("");
		deviceListView.getItems().add("=== Switches ===");
		for (Switch sw : topology.getSwitches()) {
			deviceListView.getItems().add("  " + sw.getName());
		}

		deviceListView.getItems().add("");
		deviceListView.getItems().add("=== Hosts ===");
		for (Host host : topology.getHosts()) {
			deviceListView.getItems().add("  " + host.getHostname());
		}
	}

	/**
	 * Opens the CLI dialog for a router.
	 *
	 * @param router the router to open CLI for
	 */
	private void openRouterCLI(Router router) {
		CLIDialog cliDialog = new CLIDialog(router);
		cliDialog.showAndWait();
	}

	/**
	 * Shows an error message.
	 *
	 * @param message the error message
	 */
	private void showError(String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	/**
	 * Shows an information message.
	 *
	 * @param message the information message
	 */
	private void showInfo(String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Information");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	/**
	 * Internal class representing a visual device node.
	 */
	private static class DeviceNode {
		Object device;
		VBox stackPane;
		Circle circle;

		DeviceNode(Object device, VBox stackPane, Circle circle) {
			this.device = device;
			this.stackPane = stackPane;
			this.circle = circle;
		}
	}

	/**
	 * Helper class for drag delta calculation.
	 */
	private static class Delta {
		double x, y;
	}
}

