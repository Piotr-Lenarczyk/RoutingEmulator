package org.uj.routingemulator.gui;

import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.config.ConfigurationFactory;
import org.uj.routingemulator.router.config.ConfigurationGenerator;
import org.uj.routingemulator.router.config.ConfigurationParseException;
import org.uj.routingemulator.router.config.ConfigurationParser;
import org.uj.routingemulator.switching.Switch;
import org.uj.routingemulator.switching.SwitchPort;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Controller for the network topology GUI.
 * Manages the visual representation of the network and user interactions.
 */
public class NetworkTopologyController {

	private static final String COMMAND_CONFIG_FILE_EXTENSION = "*.conf";
	private static final String HIERARCHICAL_CONFIG_FILE_EXTENSION = "*.cfg";
	private static final String TEXT_FILE_EXTENSION = "*.txt";
	private static final String COMBO_BOX = ".combo-box";

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
	@FXML
	private Button loadConfigButton;
	@FXML
	private Button saveConfigButton;

	private NetworkTopology topology;
	private Map<Object, DeviceNode> deviceNodes; // Device (Router/Switch/Host) -> Visual Node
	private Map<Connection, ConnectionVisual> connectionLines; // Connection -> Visual Elements
	private DeviceNode selectedNode;
	private DeviceNode connectionStartNode;

	// Tracks if the informational prompt was shown
	private boolean firstConnectionPromptShown = false;

	/**
	 * Initializes the controller.
	 * Sets up the network topology and event handlers.
	 */
	@FXML
	public void initialize() {
		topology = new NetworkTopology();
		// Use IdentityHashMap to prevent lookup failures when object hashCodes change dynamically
		deviceNodes = new IdentityHashMap<>();
		connectionLines = new IdentityHashMap<>();
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
		loadConfigButton.setOnAction(e -> loadRouterConfiguration());
		saveConfigButton.setOnAction(e -> saveRouterConfiguration());

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

	private static boolean checkInterfaceState(RouterInterface iface, boolean allInterfacesUp) {
		if (iface.isDisabled()) {
			allInterfacesUp = false;
		}
		return allInterfacesUp;
	}

	/**
	 * Adds a new host to the topology.
	 */
	private void addHost() {
		final String HOST_CONFIGURATION = "Host Configuration";
		TextInputDialog dialog = new TextInputDialog("PC" + (topology.getHosts().size() + 1));
		dialog.setTitle("Add Host");
		dialog.setHeaderText("Add a new host");
		dialog.setContentText("Host name:");

		Optional<String> result = dialog.showAndWait();
		result.ifPresent(name -> {
			// Ask for IP address
			@SuppressWarnings("java:S1313") // Suggested placeholder value
			TextInputDialog ipDialog = new TextInputDialog("192.168.1.1");
			ipDialog.setTitle(HOST_CONFIGURATION);
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
					maskDialog.setTitle(HOST_CONFIGURATION);
					maskDialog.setHeaderText("Configure subnet mask");
					maskDialog.setContentText("Subnet mask (CIDR):");

					Optional<String> maskResult = maskDialog.showAndWait();
					maskResult.ifPresent(maskStr -> parseSubnetMask(name, maskStr, ip));
				} catch (Exception ex) {
					showError("Invalid IP address: " + ex.getMessage());
				}
			});
		});
	}

	private void parseSubnetMask(String name, String maskStr, IPAddress ip) {
		try {
			int maskLength = Integer.parseInt(maskStr);
			SubnetMask mask = new SubnetMask(maskLength);

			// Ask for default gateway
			@SuppressWarnings("java:S1313") // Suggested placeholder value
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
			removeDeviceConnections(device);

			// Remove device from topology
			if (device instanceof Router router) {
				topology.removeRouter(router);
			} else if (device instanceof Switch sw) {
				topology.removeSwitch(sw);
			} else if (device instanceof Host host) {
				topology.removeHost(host);
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
	 * @param device the device to check
	 * @param connection the connection to check
	 * @return true if the device is part of the connection
	 */
	private boolean isDeviceInConnection(Object device, Connection connection) {
		if (device instanceof Router router) {
			return router.getInterfaces().stream().anyMatch(iface ->
					iface.equals(connection.interfaceA()) || iface.equals(connection.interfaceB()));
		} else if (device instanceof Switch sw) {
			return sw.getPorts().stream().anyMatch(port ->
					port.equals(connection.interfaceA()) || port.equals(connection.interfaceB()));
		} else if (device instanceof Host host) {
			return host.getHostInterface().equals(connection.interfaceA()) ||
					host.getHostInterface().equals(connection.interfaceB());
		}
		return false;
	}

	/**
	 * Starts connection mode, allowing the user to create a connection between two devices.
	 * Shows an informational prompt only the very first time it is used.
	 */
	private void startConnectionMode() {
		if (selectedNode == null) {
			showError("Please select the first device for the connection");
			return;
		}
		connectionStartNode = selectedNode;

		if (!firstConnectionPromptShown) {
			showInfo("Now select the second device to complete the connection");
			firstConnectionPromptShown = true;
		}
	}

	private void removeDeviceConnections(Object device) {
		List<Connection> connectionsToRemove = new ArrayList<>();
		for (Connection conn : topology.getConnections()) {
			if (isDeviceInConnection(device, conn)) {
				connectionsToRemove.add(conn);
			}
		}

		for (Connection conn : connectionsToRemove) {
			ConnectionVisual visual = connectionLines.remove(conn);
			if (visual != null) {
				canvasPane.getChildren().removeAll(visual.line(), visual.labelA(), visual.labelB());
			}
		}
	}

	/**
	 * Adds a visual node for a device to the canvas.
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
		circle.setMouseTransparent(true);

		Text text = new Text(label);
		text.setStyle("-fx-font-weight: bold;");
		text.setMouseTransparent(true);

		StackPane stackPane = new StackPane();
		stackPane.getChildren().addAll(circle, text);
		stackPane.setMouseTransparent(true);

		VBox container = new VBox(5);
		container.setAlignment(Pos.CENTER);

		Text nameText = new Text(name);
		nameText.setMouseTransparent(true);

		container.getChildren().addAll(stackPane, nameText);
		container.setManaged(false);
		container.setPickOnBounds(true);
		container.setLayoutX(x - 30);
		container.setLayoutY(y - 30);

		DeviceNode deviceNode = new DeviceNode(device, container, circle);
		deviceNodes.put(device, deviceNode);

		// Make node draggable
		final Delta dragDelta = new Delta();
		container.setOnMousePressed(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				Point2D mousePosition = canvasPane.sceneToLocal(e.getSceneX(), e.getSceneY());
				dragDelta.x = container.getLayoutX() - mousePosition.getX();
				dragDelta.y = container.getLayoutY() - mousePosition.getY();
				e.consume();
			}
		});

		container.setOnMouseDragged(e -> {
			if (e.isPrimaryButtonDown()) {
				updateDraggedDevice(device, container, e, dragDelta);
				e.consume();
			}
		});

		container.setOnMouseReleased(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				updateDraggedDevice(device, container, e, dragDelta);
			}
		});

		container.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				handleNodeClick(deviceNode, e);
				e.consume();
			}
		});

		canvasPane.getChildren().add(container);
	}

	private void updateDraggedDevice(Object device, VBox container, MouseEvent event, Delta dragDelta) {
		Point2D mousePosition = canvasPane.sceneToLocal(event.getSceneX(), event.getSceneY());
		container.setLayoutX(mousePosition.getX() + dragDelta.x);
		container.setLayoutY(mousePosition.getY() + dragDelta.y);
		updateAllConnectionLines();
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
		javafx.util.StringConverter<Connection> connectionConverter = new StringConverter<>() {
			@Override
			public String toString(Connection conn) {
				if (conn == null) return "null";
				return formatInterfaceDisplay(conn.interfaceA()) + " <--> " + formatInterfaceDisplay(conn.interfaceB());
			}

			@Override
			public Connection fromString(String string) {
				return null; // Not needed for ChoiceDialog
			}
		};

		// Create a dialog to select which connection to remove
		ChoiceDialog<Connection> dialog = new ChoiceDialog<>(relatedConnections.getFirst(), relatedConnections);
		dialog.setTitle("Remove Connection");
		dialog.setHeaderText("Select connection to remove");
		dialog.setContentText("Connection:");

		// Set the converter for the ComboBox inside the ChoiceDialog
		@SuppressWarnings("unchecked")
		ComboBox<Connection> comboBox = (ComboBox<Connection>) dialog.getDialogPane().lookup(COMBO_BOX);
		if (comboBox != null) {
			comboBox.setConverter(connectionConverter);
		}

		Optional<Connection> result = dialog.showAndWait();
		result.ifPresent(conn -> {
			topology.removeConnection(conn);
			ConnectionVisual visual = connectionLines.remove(conn);
			if (visual != null) {
				canvasPane.getChildren().removeAll(visual.line(), visual.labelA(), visual.labelB());
			}
			updateAllConnectionLines(); // Re-relax the remaining labels
		});
	}

	/**
	 * Handles click on a device node.
	 * @param node the clicked node
	 * @param event the mouse event that triggered the click
	 */
	private void handleNodeClick(DeviceNode node, MouseEvent event) {
		if (connectionStartNode != null && connectionStartNode != node) {
			// Complete connection
			createConnection(connectionStartNode, node);
			connectionStartNode = null;
		} else if (node.device instanceof Router router) {
			// Strict double-click detection for router CLI
			if (event.getClickCount() == 2) {
				openRouterCLI(router);
			}
			selectedNode = node;
			updateSelection();
		} else if (node.device instanceof Host host) {
			// Strict double-click detection for host configuration
			if (event.getClickCount() == 2) {
				openHostDialog(host);
			}
			selectedNode = node;
			updateSelection();
		} else {
			selectedNode = node;
			updateSelection();
		}
	}

	/**
	 * Gets available (unconnected) interfaces for a device.
	 * @param device the device
	 * @return list of available interfaces
	 */
	private List<NetworkInterface> getAvailableInterfaces(Object device) {
		List<NetworkInterface> allInterfaces = new ArrayList<>();
		if (device instanceof Router router) {
			allInterfaces.addAll(router.getInterfaces());
		} else if (device instanceof Switch sw) {
			allInterfaces.addAll(sw.getPorts());
		} else if (device instanceof Host host) {
			allInterfaces.add(host.getHostInterface());
		}

		// Filter out already connected interfaces
		List<NetworkInterface> availableInterfaces = new ArrayList<>();
		for (NetworkInterface iface : allInterfaces) {
			boolean isConnected = topology.getConnections().stream().anyMatch(conn ->
					conn.interfaceA().equals(iface) || conn.interfaceB().equals(iface));
			if (!isConnected) {
				availableInterfaces.add(iface);
			}
		}
		return availableInterfaces;
	}

	/**
	 * Triggers a visual update for all connections and intelligently resolves any label clipping
	 */
	private void updateAllConnectionLines() {
		// 1. Reset all labels to their optimal (but potentially clipping) layout starting positions
		for (Map.Entry<Connection, ConnectionVisual> entry : connectionLines.entrySet()) {
			Connection conn = entry.getKey();
			ConnectionVisual visual = entry.getValue();

			Object deviceA = findDevice(conn.interfaceA());
			Object deviceB = findDevice(conn.interfaceB());

			if (deviceA != null && deviceB != null) {
				DeviceNode nodeA = deviceNodes.get(deviceA);
				DeviceNode nodeB = deviceNodes.get(deviceB);
				if (nodeA != null && nodeB != null) {
					updateConnectionVisual(visual, nodeA, nodeB);
				}
			}
		}

		// 2. Perform global force-directed collision resolution on all labels
		resolveAllLabelOverlaps();
	}

	/**
	 * Updates a connection visual group between two nodes.
	 * @param visual the visual group containing line and labels
	 * @param nodeA the first node
	 * @param nodeB the second node
	 */
	private void updateConnectionVisual(ConnectionVisual visual, DeviceNode nodeA, DeviceNode nodeB) {
		Point2D start = canvasPane.sceneToLocal(nodeA.circle().localToScene(0, 0));
		Point2D end = canvasPane.sceneToLocal(nodeB.circle().localToScene(0, 0));

		visual.line().setStartX(start.getX());
		visual.line().setStartY(start.getY());
		visual.line().setEndX(end.getX());
		visual.line().setEndY(end.getY());

		double dx = end.getX() - start.getX();
		double dy = end.getY() - start.getY();
		double distance = Math.hypot(dx, dy);

		// Hide labels if nodes are dragged too close together to avoid clutter
		if (distance < 50) {
			visual.labelA().setVisible(false);
			visual.labelB().setVisible(false);
			return;
		} else {
			visual.labelA().setVisible(true);
			visual.labelB().setVisible(true);
		}

		// Calculate the unit vector (direction) of the line
		double unitX = dx / distance;
		double unitY = dy / distance;

		// Circle radius is 25. Anchor the label 38 pixels from the center (13px padding).
		double offsetFromCenter = 38.0;

		// Prevent labels from crossing the midpoint on very short lines
		double actualOffsetA = Math.min(offsetFromCenter, distance / 2.5);
		double actualOffsetB = Math.min(offsetFromCenter, distance / 2.5);

		// Base anchor points for the text
		double anchorAX = start.getX() + unitX * actualOffsetA;
		double anchorAY = start.getY() + unitY * actualOffsetA;

		double anchorBX = end.getX() - unitX * actualOffsetB;
		double anchorBY = end.getY() - unitY * actualOffsetB;

		// Get actual text dimensions to prevent clipping into the circles
		double widthA = visual.labelA().getLayoutBounds().getWidth();
		double widthB = visual.labelB().getLayoutBounds().getWidth();

		// Smoothly shift the anchor point based on the line's angle.
		double shiftAX = -widthA * (0.5 - 0.5 * unitX);
		double shiftBX = -widthB * (0.5 + 0.5 * unitX);

		// Calculate a perpendicular vector (-y, x) to lift the text off the line
		double perpX = -unitY * 12;
		double perpY = unitX * 12;

		// Apply coordinates (adding +4 to Y to account for JavaFX Text baseline rendering)
		visual.labelA().setX(anchorAX + shiftAX + perpX);
		visual.labelA().setY(anchorAY + perpY + 4);

		visual.labelB().setX(anchorBX + shiftBX + perpX);
		visual.labelB().setY(anchorBY + perpY + 4);
	}

	/**
	 * Resolves layout overlaps using iterative force-directed relaxation.
	 * Prevents text nodes from clipping into each other or the router nodes.
	 */
	private void resolveAllLabelOverlaps() {
		List<Text> allLabels = new ArrayList<>();
		Map<Text, Point2D> labelToNodeMap = new HashMap<>();

		// 1. Gather all visible labels and associate them with their owning router's exact center coordinates
		for (Map.Entry<Connection, ConnectionVisual> entry : connectionLines.entrySet()) {
			Connection conn = entry.getKey();
			ConnectionVisual visual = entry.getValue();

			Object deviceA = findDevice(conn.interfaceA());
			if (deviceA != null && visual.labelA().isVisible()) {
				DeviceNode nodeA = deviceNodes.get(deviceA);
				if (nodeA != null) {
					allLabels.add(visual.labelA());
					labelToNodeMap.put(visual.labelA(), canvasPane.sceneToLocal(nodeA.circle().localToScene(0, 0)));
				}
			}

			Object deviceB = findDevice(conn.interfaceB());
			if (deviceB != null && visual.labelB().isVisible()) {
				DeviceNode nodeB = deviceNodes.get(deviceB);
				if (nodeB != null) {
					allLabels.add(visual.labelB());
					labelToNodeMap.put(visual.labelB(), canvasPane.sceneToLocal(nodeB.circle().localToScene(0, 0)));
				}
			}
		}

		// 2. Iterative Relaxation Loop (Execute physics ticks instantly)
		for (int i = 0; i < 20; i++) {
			boolean moved = false;

			for (int j = 0; j < allLabels.size(); j++) {
				Text t1 = allLabels.get(j);
				javafx.geometry.Bounds b1 = getBounds(t1);

				// Pass A: Check intersection with its owning node (keep it outside radius 35)
				Point2D nodeCenter = labelToNodeMap.get(t1);
				if (nodeCenter != null) {
					double c1x = b1.getMinX() + b1.getWidth() / 2;
					double c1y = b1.getMinY() + b1.getHeight() / 2;
					double distToNode = Math.hypot(c1x - nodeCenter.getX(), c1y - nodeCenter.getY());

					if (distToNode < 35) { // Minimum safe distance from center
						moved = true;
						double dx = c1x - nodeCenter.getX();
						double dy = c1y - nodeCenter.getY();
						if (distToNode < 0.1) {
							dx = 1;
							dy = 1;
							distToNode = Math.sqrt(2);
						} // Prevent div by 0

						// Push away from the node center
						t1.setX(t1.getX() + (dx / distToNode) * 3);
						t1.setY(t1.getY() + (dy / distToNode) * 3);
						b1 = getBounds(t1); // refresh bounds
					}
				}

				// Pass B: Check intersection with OTHER labels
				for (int k = j + 1; k < allLabels.size(); k++) {
					Text t2 = allLabels.get(k);
					javafx.geometry.Bounds b2 = getBounds(t2);

					if (b1.intersects(b2)) {
						moved = true;
						double c1x = b1.getMinX() + b1.getWidth() / 2;
						double c1y = b1.getMinY() + b1.getHeight() / 2;
						double c2x = b2.getMinX() + b2.getWidth() / 2;
						double c2y = b2.getMinY() + b2.getHeight() / 2;

						double dx = c1x - c2x;
						double dy = c1y - c2y;
						double dist = Math.hypot(dx, dy);

						// Add slight random perturbation if they are perfectly stacked identically
						if (dist < 0.1) {
							dx = Math.random() - 0.5;
							dy = Math.random() - 0.5;
							dist = Math.hypot(dx, dy);
						}

						// Push them apart dynamically
						double pushX = (dx / dist) * 2;
						double pushY = (dy / dist) * 2;

						t1.setX(t1.getX() + pushX);
						t1.setY(t1.getY() + pushY);

						t2.setX(t2.getX() - pushX);
						t2.setY(t2.getY() - pushY);

						b1 = getBounds(t1); // update bounds after moving
					}
				}
			}
			if (!moved) break; // Break early if nothing overlaps anymore
		}
	}

	/**
	 * Generates a padded bounding box representing the active collision area of a text node.
	 */
	private javafx.geometry.Bounds getBounds(Text t) {
		double w = t.getLayoutBounds().getWidth();
		double h = t.getLayoutBounds().getHeight();
		// Pad by 2 pixels to give the text elements breathing room
		return new javafx.geometry.BoundingBox(t.getX() - 2, t.getY() - h - 2, w + 4, h + 4);
	}

	/**
	 * Finds the device that owns a network interface.
	 * @param iface the interface
	 * @return the device owning the interface, or null if not found
	 */
	private Object findDevice(NetworkInterface iface) {
		// Check routers
		for (Router router : topology.getRouters()) {
			if (iface instanceof RouterInterface && router.getInterfaces().contains(iface)) {
				return router;
			}
		}
		// Check switches
		for (Switch sw : topology.getSwitches()) {
			if (iface instanceof SwitchPort && sw.getPorts().contains(iface)) {
				return sw;
			}
		}
		// Check hosts
		for (Host host : topology.getHosts()) {
			if (host.getHostInterface().equals(iface)) {
				return host;
			}
		}
		return null;
	}

	/**
	 * Gets the display name of a device.
	 * @param device the device
	 * @return the device name
	 */
	private String getDeviceName(Object device) {
		if (device instanceof Router router) {
			return router.getName();
		} else if (device instanceof Switch sw) {
			return sw.getName();
		} else if (device instanceof Host host) {
			return host.getHostname();
		}
		return "Unknown";
	}

	/**
	 * Formats a network interface for display with user-friendly information.
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
			display.append(" (").append(iface.getSubnet().networkAddress());
			display.append("/").append(iface.getSubnet().subnetMask().shortMask()).append(")");
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
	 * @param router the router to open CLI for
	 */
	private void openRouterCLI(Router router) {
		SimpleCLIDialog cliDialog = new SimpleCLIDialog(router, topology);
		cliDialog.showAndWait();
	}

	/**
	 * Opens the configuration dialog for a host.
	 * @param host the host to configure
	 */
	private void openHostDialog(Host host) {
		HostConfigDialog dialog = new HostConfigDialog(host, topology);
		dialog.showAndWait();
	}

	/**
	 * Shows an error message.
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
	 * Creates a connection between two device nodes.
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
		StringConverter<NetworkInterface> interfaceConverter = new StringConverter<>() {
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
		ChoiceDialog<NetworkInterface> startDialog = new ChoiceDialog<>(startInterfaces.getFirst(), startInterfaces);
		startDialog.setTitle("Select Interface");
		startDialog.setHeaderText("Select interface on " + getDeviceName(startNode.device));
		startDialog.setContentText("Interface:");

		// Set the converter for the ComboBox inside the ChoiceDialog
		@SuppressWarnings("unchecked")
		ComboBox<NetworkInterface> startComboBox = (ComboBox<NetworkInterface>) startDialog.getDialogPane().lookup(COMBO_BOX);
		if (startComboBox != null) {
			startComboBox.setConverter(interfaceConverter);
		}

		Optional<NetworkInterface> startResult = startDialog.showAndWait();
		if (startResult.isEmpty()) {
			return;
		}

		ChoiceDialog<NetworkInterface> endDialog = new ChoiceDialog<>(endInterfaces.getFirst(), endInterfaces);
		endDialog.setTitle("Select Interface");
		endDialog.setHeaderText("Select interface on " + getDeviceName(endNode.device));
		endDialog.setContentText("Interface:");

		// Set the converter for the ComboBox inside the ChoiceDialog
		@SuppressWarnings("unchecked")
		ComboBox<NetworkInterface> endComboBox = (ComboBox<NetworkInterface>) endDialog.getDialogPane().lookup(COMBO_BOX);
		if (endComboBox != null) {
			endComboBox.setConverter(interfaceConverter);
		}

		Optional<NetworkInterface> endResult = endDialog.showAndWait();
		if (endResult.isEmpty()) {
			return;
		}

		try {
			Connection connection = new Connection(startResult.get(), endResult.get());
			topology.addConnection(connection);

			Line line = new Line();
			line.setStrokeWidth(3);
			line.setStroke(Color.DARKGRAY);
			line.setMouseTransparent(true);

			Text labelA = new Text(startResult.get().getInterfaceName());
			labelA.setStyle("-fx-font-size: 11px; -fx-fill: #333333; -fx-font-weight: bold;");
			labelA.setMouseTransparent(true);

			Text labelB = new Text(endResult.get().getInterfaceName());
			labelB.setStyle("-fx-font-size: 11px; -fx-fill: #333333; -fx-font-weight: bold;");
			labelB.setMouseTransparent(true);

			ConnectionVisual visual = new ConnectionVisual(line, labelA, labelB);
			connectionLines.put(connection, visual);

			// Add elements to the canvas. Adding them behind the circles so nodes stay clickable
			canvasPane.getChildren().addFirst(labelB);
			canvasPane.getChildren().addFirst(labelA);
			canvasPane.getChildren().addFirst(line);

			// Re-render and resolve overlaps
			updateAllConnectionLines();

		} catch (Exception ex) {
			showError("Failed to create connection: " + ex.getMessage());
		}
	}

	/**
	 * Loads router configuration from a file.
	 * Automatically detects the format (command-based or hierarchical).
	 */
	private void loadRouterConfiguration() {
		if (selectedNode == null || !(selectedNode.device instanceof Router router)) {
			showError("Please select a router first");
			return;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Load Router Configuration");
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("All Config Files", COMMAND_CONFIG_FILE_EXTENSION, HIERARCHICAL_CONFIG_FILE_EXTENSION, TEXT_FILE_EXTENSION),
				new FileChooser.ExtensionFilter("Command Format", COMMAND_CONFIG_FILE_EXTENSION),
				new FileChooser.ExtensionFilter("Hierarchical Format", HIERARCHICAL_CONFIG_FILE_EXTENSION),
				new FileChooser.ExtensionFilter("Text Files", TEXT_FILE_EXTENSION),
				new FileChooser.ExtensionFilter("All Files", "*.*")
		);

		Stage stage = (Stage) canvasPane.getScene().getWindow();
		File file = fileChooser.showOpenDialog(stage);
		if (file != null) {
			try {
				String config = Files.readString(file.toPath());
				// Automatically detect format
				ConfigurationParser parser = ConfigurationFactory.getParser(config);
				parser.loadConfiguration(router, config);

				// Update visual representation if interface states changed
				updateInterfaceStates(router);
				showInfo("Configuration loaded successfully from " + file.getName());
			} catch (ConfigurationParseException e) {
				showError("Configuration error: " + e.getMessage());
			} catch (Exception e) {
				showError("Failed to load configuration: " + e.getMessage());
			}
		}
	}

	/**
	 * Saves router configuration to a file.
	 * Allows user to choose the format (command-based or hierarchical).
	 */
	private void saveRouterConfiguration() {
		if (selectedNode == null || !(selectedNode.device instanceof Router router)) {
			showError("Please select a router first");
			return;
		}

		// Ask user for format
		Alert formatAlert = new Alert(Alert.AlertType.CONFIRMATION);
		formatAlert.setTitle("Choose Configuration Format");
		formatAlert.setHeaderText("Select the configuration format");
		formatAlert.setContentText("Choose the format for the configuration file:");

		ButtonType commandFormatButton = new ButtonType("Command Format (.conf)");
		ButtonType hierarchicalFormatButton = new ButtonType("Hierarchical Format (.cfg)");
		ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

		formatAlert.getButtonTypes().setAll(commandFormatButton, hierarchicalFormatButton, cancelButton);

		Optional<ButtonType> formatResult = formatAlert.showAndWait();
		if (formatResult.isEmpty() || formatResult.get() == cancelButton) {
			return;
		}

		boolean isCommandFormat = formatResult.get() == commandFormatButton;
		ConfigurationGenerator generator = isCommandFormat ? ConfigurationFactory.getCommandGenerator() : ConfigurationFactory.getHierarchicalGenerator();

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Router Configuration");
		String extension = isCommandFormat ? COMMAND_CONFIG_FILE_EXTENSION : HIERARCHICAL_CONFIG_FILE_EXTENSION;
		String description = isCommandFormat ? "Command Format" : "Hierarchical Format";
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter(description, extension),
				new FileChooser.ExtensionFilter("Text Files", TEXT_FILE_EXTENSION),
				new FileChooser.ExtensionFilter("All Files", "*.*")
		);

		// Suggest filename
		fileChooser.setInitialFileName(router.getName() + (isCommandFormat ? ".conf" : ".cfg"));

		Stage stage = (Stage) canvasPane.getScene().getWindow();
		File file = fileChooser.showSaveDialog(stage);
		if (file != null) {
			try {
				String config = generator.generateConfiguration(router);
				Files.writeString(file.toPath(), config);
				showInfo("Configuration saved successfully to " + file.getName());
			} catch (Exception e) {
				showError("Failed to save configuration: " + e.getMessage());
			}
		}
	}

	/**
	 * Updates visual representation of interface states after configuration load.
	 * @param router the router whose interfaces to update
	 */
	private void updateInterfaceStates(Router router) {
		// Update connection line colors based on interface states
		for (Map.Entry<Connection, ConnectionVisual> entry : connectionLines.entrySet()) {
			Connection conn = entry.getKey();
			ConnectionVisual visual = entry.getValue();

			// Check if this connection involves the router
			boolean hasRouterInterface = false;
			boolean allInterfacesUp = true;

			for (RouterInterface iface : router.getInterfaces()) {
				if (iface.equals(conn.interfaceA()) || iface.equals(conn.interfaceB())) {
					hasRouterInterface = true;
					allInterfacesUp = checkInterfaceState(iface, allInterfacesUp);
				}
			}

			// Update line color if it involves this router
			if (hasRouterInterface) {
				visual.line().setStroke(allInterfacesUp ? Color.DARKGRAY : Color.RED);
			}
		}
	}

	/**
	 * Internal class representing a visual device node.
	 */
	private record DeviceNode(Object device, VBox stackPane, Circle circle) {
	}

	/**
	 * Internal class representing a visual connection.
	 */
	private record ConnectionVisual(Line line, Text labelA, Text labelB) {
	}

	/**
	 * Helper class for drag delta calculation.
	 */
	private static class Delta {
		double x;
		double y;
	}
}