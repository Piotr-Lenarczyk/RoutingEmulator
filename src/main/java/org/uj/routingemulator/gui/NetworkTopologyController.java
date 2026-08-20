package org.uj.routingemulator.gui;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
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
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.config.ConfigurationApplicationService;
import org.uj.routingemulator.router.config.ConfigurationParseException;
import org.uj.routingemulator.router.config.FileConfigurationLoader;
import org.uj.routingemulator.router.config.FileConfigurationWriter;
import org.uj.routingemulator.switching.Switch;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
	private TopologyApplicationService applicationService;
	private TopologyQueryService queryService;
	private ConfigurationApplicationService configurationApplicationService;
	private PingApplicationService pingApplicationService;
	private HostConfigurationService hostConfigurationService;
	private RouterCLIService routerCLIService;

	private Map<DeviceId, DeviceNode> deviceNodes;
	private Map<ConnectionId, Line> connectionLines;

	private DeviceNode selectedNode;
	private DeviceNode connectionStartNode;

	@FXML
	public void initialize() {
		this.topology = new NetworkTopology();
		this.applicationService = new TopologyApplicationService(topology);
		this.queryService = new TopologyQueryService(topology);
		this.configurationApplicationService = new ConfigurationApplicationService(
				new FileConfigurationLoader(),
				new FileConfigurationWriter()
		);
		this.pingApplicationService = new PingApplicationService(topology);
		this.hostConfigurationService = new HostConfigurationService();
		this.routerCLIService = new RouterCLIService(topology);

		this.deviceNodes = new HashMap<>();
		this.connectionLines = new HashMap<>();

		updateDeviceList();
		setupEventHandlers();
	}

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

	private void addRouter() {
		TopologyViewModel vm = queryService.getTopologyViewModel();
		TextInputDialog dialog = new TextInputDialog("R" + (vm.routers().size() + 1));
		dialog.setTitle("Add Router");
		dialog.setHeaderText("Add a new router");
		dialog.setContentText("Router name:");

		dialog.showAndWait().ifPresent(name -> {
			TextInputDialog interfaceDialog = new TextInputDialog("3");
			interfaceDialog.setTitle("Router Interfaces");
			interfaceDialog.setHeaderText("Configure router interfaces");
			interfaceDialog.setContentText("Number of interfaces:");

			interfaceDialog.showAndWait().ifPresent(numStr -> {
				try {
					int numInterfaces = Integer.parseInt(numStr);
					if (numInterfaces < 1 || numInterfaces > 10) {
						showError("Number of interfaces must be between 1 and 10");
						return;
					}
					Router router = applicationService.addRouter(name, numInterfaces);
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

	private void addSwitch() {
		TopologyViewModel vm = queryService.getTopologyViewModel();
		TextInputDialog dialog = new TextInputDialog("SW" + (vm.switches().size() + 1));
		dialog.setTitle("Add Switch");
		dialog.setHeaderText("Add a new switch");
		dialog.setContentText("Switch name:");

		dialog.showAndWait().ifPresent(name -> {
			TextInputDialog portDialog = new TextInputDialog("4");
			portDialog.setTitle("Switch Ports");
			portDialog.setHeaderText("Configure switch ports");
			portDialog.setContentText("Number of ports:");

			portDialog.showAndWait().ifPresent(numStr -> {
				try {
					int numPorts = Integer.parseInt(numStr);
					if (numPorts < 1 || numPorts > 48) {
						showError("Number of ports must be between 1 and 48");
						return;
					}
					Switch sw = applicationService.addSwitch(name, numPorts);
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

	private void addHost() {
		TopologyViewModel vm = queryService.getTopologyViewModel();
		TextInputDialog dialog = new TextInputDialog("PC" + (vm.hosts().size() + 1));
		dialog.setTitle("Add Host");
		dialog.setHeaderText("Add a new host");
		dialog.setContentText("Host name:");

		dialog.showAndWait().ifPresent(name -> {
			TextInputDialog ipDialog = new TextInputDialog("192.168.1.1");
			ipDialog.setTitle("Host Configuration");
			ipDialog.setHeaderText("Configure host IP address");
			ipDialog.setContentText("IP Address:");

			ipDialog.showAndWait().ifPresent(ipStr -> {
				TextInputDialog maskDialog = new TextInputDialog("24");
				maskDialog.setTitle("Host Configuration");
				maskDialog.setHeaderText("Configure subnet mask");
				maskDialog.setContentText("Subnet mask (CIDR):");

				maskDialog.showAndWait().ifPresent(maskStr -> {
					TextInputDialog gwDialog = new TextInputDialog("192.168.1.254");
					gwDialog.setTitle("Host Configuration");
					gwDialog.setHeaderText("Configure default gateway");
					gwDialog.setContentText("Default gateway:");

					gwDialog.showAndWait().ifPresent(gwStr -> {
						try {
							Host host = applicationService.addHost(name, ipStr, maskStr, gwStr);
							double x = 100 + Math.random() * (canvasPane.getWidth() - 200);
							double y = 100 + Math.random() * (canvasPane.getHeight() - 200);
							addDeviceNode(host, x, y, Color.LIGHTYELLOW, "H");
							updateDeviceList();
						} catch (Exception ex) {
							showError("Invalid configuration: " + ex.getMessage());
						}
					});
				});
			});
		});
	}

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
			DeviceId deviceId = selectedNode.deviceId();
			Device device = queryService.getDevice(deviceId);
			List<Connection> connectionsToRemove = queryService.getDeviceConnections(device);

			for (Connection conn : connectionsToRemove) {
				Line line = connectionLines.remove(conn.id());
				if (line != null) {
					canvasPane.getChildren().remove(line);
				}
			}

			applicationService.removeDevice(deviceId);
			canvasPane.getChildren().remove(selectedNode.stackPane());
			deviceNodes.remove(deviceId);
			selectedNode = null;
			updateDeviceList();
		}
	}

	private void startConnectionMode() {
		if (selectedNode == null) {
			showError("Please select the first device for the connection");
			return;
		}
		connectionStartNode = selectedNode;
		showInfo("Now select the second device to complete the connection");
	}

	private void addDeviceNode(Device device, double x, double y, Color color, String label) {
		String name = device.getDeviceName();
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

		DeviceNode deviceNode = new DeviceNode(device.getId(), container, circle);
		deviceNodes.put(device.getId(), deviceNode);

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
				updateConnectionLines(device.getId());
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

	private void removeConnection() {
		if (selectedNode == null) {
			showError("Please select a device to remove its connections");
			return;
		}

		DeviceId deviceId = selectedNode.deviceId();
		Device device = queryService.getDevice(deviceId);
		List<Connection> relatedConnections = queryService.getDeviceConnections(device);

		if (relatedConnections.isEmpty()) {
			showError("No connections found for this device");
			return;
		}

		StringConverter<Connection> connectionConverter = new StringConverter<>() {
			@Override
			public String toString(Connection conn) {
				if (conn == null) return "null";
				return formatInterfaceDisplay(conn.interfaceA()) + " <--> " + formatInterfaceDisplay(conn.interfaceB());
			}
			@Override
			public Connection fromString(String string) {
				return null;
			}
		};

		ChoiceDialog<Connection> dialog = new ChoiceDialog<>(relatedConnections.getFirst(), relatedConnections);
		dialog.setTitle("Remove Connection");
		dialog.setHeaderText("Select connection to remove");
		dialog.setContentText("Connection:");

		@SuppressWarnings("unchecked")
		ComboBox<Connection> comboBox = (ComboBox<Connection>) dialog.getDialogPane().lookup(COMBO_BOX);
		if (comboBox != null) {
			comboBox.setConverter(connectionConverter);
		}

		dialog.showAndWait().ifPresent(conn -> {
			applicationService.removeConnection(conn);
			Line line = connectionLines.remove(conn.id());
			if (line != null) {
				canvasPane.getChildren().remove(line);
			}
		});
	}

	private void handleNodeClick(DeviceNode node) {
		if (connectionStartNode != null && connectionStartNode != node) {
			createConnection(connectionStartNode, node);
			connectionStartNode = null;
		} else {
			Device device = queryService.getDevice(node.deviceId());
			if (device instanceof Router router) {
				if (selectedNode == node) {
					openRouterCLI(router);
				} else {
					selectedNode = node;
					updateSelection();
				}
			} else if (device instanceof Host host && selectedNode == node) {
				openHostDialog(host);
			} else {
				selectedNode = node;
				updateSelection();
			}
		}
	}

	private void updateConnectionLines(DeviceId deviceId) {
		for (Map.Entry<ConnectionId, Line> entry : connectionLines.entrySet()) {
			ConnectionId connId = entry.getKey();
			Line line = entry.getValue();

			Connection conn = queryService.getConnection(connId);
			if (conn == null) continue;

			Device deviceA = queryService.findDevice(conn.interfaceA());
			Device deviceB = queryService.findDevice(conn.interfaceB());

			if (deviceA != null && deviceB != null &&
					(deviceId.equals(deviceA.getId()) || deviceId.equals(deviceB.getId()))) {
				DeviceNode nodeA = deviceNodes.get(deviceA.getId());
				DeviceNode nodeB = deviceNodes.get(deviceB.getId());

				if (nodeA != null && nodeB != null) {
					updateConnectionLine(line, nodeA, nodeB);
				}
			}
		}
	}

	private void updateConnectionLine(Line line, DeviceNode nodeA, DeviceNode nodeB) {
		double startX = nodeA.stackPane().getLayoutX() + 30;
		double startY = nodeA.stackPane().getLayoutY() + 30;
		double endX = nodeB.stackPane().getLayoutX() + 30;
		double endY = nodeB.stackPane().getLayoutY() + 30;

		line.setStartX(startX);
		line.setStartY(startY);
		line.setEndX(endX);
		line.setEndY(endY);
	}

	private String formatInterfaceDisplay(NetworkInterface iface) {
		if (iface == null) return "null";
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

	private void updateSelection() {
		for (DeviceNode node : deviceNodes.values()) {
			node.circle().setStrokeWidth(2);
			node.circle().setStroke(Color.BLACK);
		}

		if (selectedNode != null) {
			selectedNode.circle().setStrokeWidth(4);
			selectedNode.circle().setStroke(Color.BLUE);
		}
	}

	private void updateDeviceList() {
		TopologyViewModel vm = queryService.getTopologyViewModel();
		deviceListView.getItems().clear();

		deviceListView.getItems().add("=== Routers ===");
		for (Router router : vm.routers()) {
			deviceListView.getItems().add("  " + router.getName());
		}
		deviceListView.getItems().add("");

		deviceListView.getItems().add("=== Switches ===");
		for (Switch sw : vm.switches()) {
			deviceListView.getItems().add("  " + sw.getName());
		}
		deviceListView.getItems().add("");

		deviceListView.getItems().add("=== Hosts ===");
		for (Host host : vm.hosts()) {
			deviceListView.getItems().add("  " + host.getHostname());
		}
	}

	private void openRouterCLI(Router router) {
		SimpleCLIDialog cliDialog = new SimpleCLIDialog(router, routerCLIService);
		cliDialog.showAndWait();
	}

	private void openHostDialog(Host host) {
		HostConfigDialog dialog = new HostConfigDialog(host, hostConfigurationService, pingApplicationService);
		dialog.showAndWait();
	}

	private void showError(String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	private void showInfo(String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Information");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	private void createConnection(DeviceNode startNode, DeviceNode endNode) {
		Device startDevice = queryService.getDevice(startNode.deviceId());
		Device endDevice = queryService.getDevice(endNode.deviceId());

		List<NetworkInterface> startInterfaces = queryService.getAvailableInterfaces(startDevice);
		List<NetworkInterface> endInterfaces = queryService.getAvailableInterfaces(endDevice);

		if (startInterfaces.isEmpty()) {
			showError("No available interfaces on " + startDevice.getDeviceName());
			return;
		}
		if (endInterfaces.isEmpty()) {
			showError("No available interfaces on " + endDevice.getDeviceName());
			return;
		}

		StringConverter<NetworkInterface> interfaceConverter = new StringConverter<>() {
			@Override
			public String toString(NetworkInterface iface) {
				return formatInterfaceDisplay(iface);
			}
			@Override
			public NetworkInterface fromString(String string) {
				return null;
			}
		};

		ChoiceDialog<NetworkInterface> startDialog = new ChoiceDialog<>(startInterfaces.getFirst(), startInterfaces);
		startDialog.setTitle("Select Interface");
		startDialog.setHeaderText("Select interface on " + startDevice.getDeviceName());
		startDialog.setContentText("Interface:");

		@SuppressWarnings("unchecked")
		ComboBox<NetworkInterface> startComboBox = (ComboBox<NetworkInterface>) startDialog.getDialogPane().lookup(COMBO_BOX);
		if (startComboBox != null) {
			startComboBox.setConverter(interfaceConverter);
		}

		Optional<NetworkInterface> startResult = startDialog.showAndWait();
		if (startResult.isEmpty()) return;

		ChoiceDialog<NetworkInterface> endDialog = new ChoiceDialog<>(endInterfaces.getFirst(), endInterfaces);
		endDialog.setTitle("Select Interface");
		endDialog.setHeaderText("Select interface on " + endDevice.getDeviceName());
		endDialog.setContentText("Interface:");

		@SuppressWarnings("unchecked")
		ComboBox<NetworkInterface> endComboBox = (ComboBox<NetworkInterface>) endDialog.getDialogPane().lookup(COMBO_BOX);
		if (endComboBox != null) {
			endComboBox.setConverter(interfaceConverter);
		}

		Optional<NetworkInterface> endResult = endDialog.showAndWait();
		if (endResult.isEmpty()) return;

		try {
			Connection connection = applicationService.addConnection(startResult.get(), endResult.get());
			Line line = new Line();
			line.setStrokeWidth(3);
			line.setStroke(Color.DARKGRAY);

			updateConnectionLine(line, startNode, endNode);
			canvasPane.getChildren().addFirst(line);
			connectionLines.put(connection.id(), line);
		} catch (Exception ex) {
			showError("Failed to create connection: " + ex.getMessage());
		}
	}

	private void loadRouterConfiguration() {
		if (selectedNode == null) {
			showError("Please select a router first");
			return;
		}
		Device device = queryService.getDevice(selectedNode.deviceId());
		if (!(device instanceof Router router)) {
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
				configurationApplicationService.loadConfiguration(router, file.toPath());
				updateInterfaceStates(router);
				showInfo("Configuration loaded successfully from " + file.getName());
			} catch (ConfigurationParseException e) {
				showError("Configuration error: " + e.getMessage());
			} catch (Exception e) {
				showError("Failed to load configuration: " + e.getMessage());
			}
		}
	}

	private void saveRouterConfiguration() {
		if (selectedNode == null) {
			showError("Please select a router first");
			return;
		}
		Device device = queryService.getDevice(selectedNode.deviceId());
		if (!(device instanceof Router router)) {
			showError("Please select a router first");
			return;
		}

		Alert formatAlert = new Alert(Alert.AlertType.CONFIRMATION);
		formatAlert.setTitle("Choose Configuration Format");
		formatAlert.setHeaderText("Select the configuration format");
		formatAlert.setContentText("Choose the format for the configuration file:");

		ButtonType commandFormatButton = new ButtonType("Command Format (.conf)");
		ButtonType hierarchicalFormatButton = new ButtonType("Hierarchical Format (.cfg)");
		ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

		formatAlert.getButtonTypes().setAll(commandFormatButton, hierarchicalFormatButton, cancelButton);

		Optional<ButtonType> formatResult = formatAlert.showAndWait();
		if (formatResult.isEmpty() || formatResult.get() == cancelButton) return;

		boolean isCommandFormat = formatResult.get() == commandFormatButton;

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Router Configuration");

		String extension = isCommandFormat ? COMMAND_CONFIG_FILE_EXTENSION : HIERARCHICAL_CONFIG_FILE_EXTENSION;
		String description = isCommandFormat ? "Command Format" : "Hierarchical Format";

		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter(description, extension),
				new FileChooser.ExtensionFilter("Text Files", TEXT_FILE_EXTENSION),
				new FileChooser.ExtensionFilter("All Files", "*.*")
		);

		fileChooser.setInitialFileName(router.getName() + (isCommandFormat ? ".conf" : ".cfg"));

		Stage stage = (Stage) canvasPane.getScene().getWindow();
		File file = fileChooser.showSaveDialog(stage);

		if (file != null) {
			try {
				configurationApplicationService.saveConfiguration(router, file.toPath(), isCommandFormat);
				showInfo("Configuration saved successfully to " + file.getName());
			} catch (Exception e) {
				showError("Failed to save configuration: " + e.getMessage());
			}
		}
	}

	private void updateInterfaceStates(Router router) {
		for (Map.Entry<ConnectionId, Line> entry : connectionLines.entrySet()) {
			ConnectionId connId = entry.getKey();
			Line line = entry.getValue();

			Connection conn = queryService.getConnection(connId);
			if (conn == null) continue;

			boolean hasRouterInterface = false;
			boolean allInterfacesUp = true;

			for (RouterInterface iface : router.getInterfaces()) {
				if (iface.equals(conn.interfaceA()) || iface.equals(conn.interfaceB())) {
					hasRouterInterface = true;
					if (iface.isDisabled()) {
						allInterfacesUp = false;
					}
				}
			}

			if (hasRouterInterface) {
				line.setStroke(allInterfacesUp ? Color.DARKGRAY : Color.RED);
			}
		}
	}

	private record DeviceNode(DeviceId deviceId, VBox stackPane, Circle circle) {
	}

	private static class Delta {
		double x;
		double y;
	}
}