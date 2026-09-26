package org.uj.routingemulator.gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;

/**
 * Dialog for configuring a Host (IP address, gateway) and issuing ping commands.
 */
public class HostConfigDialog extends Dialog<Void> {

    private final Host host;
    private final NetworkTopology topology;

    private final TextField ipField = new TextField();
    private final TextField prefixField = new TextField();
    private final TextField gatewayField = new TextField();
    private final TextArea outputArea = new TextArea();

    public HostConfigDialog(Host host, NetworkTopology topology) {
        this.host = host;
        this.topology = topology;

        setTitle("Host Configuration - " + host.getHostname());
        setHeaderText("Configure IP and Ping from host");

        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("IP address:"), 0, 0);
        grid.add(ipField, 1, 0);
        grid.add(new Label("Prefix (/24):"), 0, 1);
        grid.add(prefixField, 1, 1);
        grid.add(new Label("Gateway:"), 0, 2);
        grid.add(gatewayField, 1, 2);

        Button applyBtn = new Button("Apply");
        Button pingBtn = new Button("Ping");
        TextField pingTarget = new TextField();
        pingTarget.setPromptText("destination IP (e.g., 192.168.1.1)");

        applyBtn.setOnAction(e -> applyConfiguration());
        pingBtn.setOnAction(e -> doPing(pingTarget.getText()));

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(
                grid,
                applyBtn,
                new Separator(),
                new Label("Ping target:"),
                pingTarget,
                pingBtn,
                new Separator(),
                outputArea
        );

        outputArea.setEditable(false);
        outputArea.setPrefRowCount(20);
        outputArea.setPrefColumnCount(80);
        // Ensure standard terminal font for proper alignment
        outputArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

        getDialogPane().setContent(vbox);

        // Initialize fields from current host interface
        HostInterface hi = host.getHostInterface();
        if (hi != null && hi.getSubnet() != null) {
            ipField.setText(hi.getSubnet().networkAddress().toString());
            prefixField.setText(String.valueOf(hi.getSubnet().subnetMask().shortMask()));
            if (hi.getDefaultGateway() != null) {
                gatewayField.setText(hi.getDefaultGateway().toString());
            }
        }
    }

    private void applyConfiguration() {
        try {
            String ipText = ipField.getText().trim();
            int prefix = Integer.parseInt(prefixField.getText().trim());

            IPAddress ip = IPAddress.fromString(ipText);
            SubnetMask mask = new SubnetMask(prefix);
            Subnet subnet = new Subnet(ip, mask);

            HostInterface hi = host.getHostInterface();
            if (hi == null) {
                hi = new HostInterface();
                host.setHostInterface(hi);
            }
            hi.setSubnet(subnet);

            String gw = gatewayField.getText().trim();
            if (!gw.isEmpty()) {
                hi.setDefaultGateway(IPAddress.fromString(gw));
            }

            outputArea.appendText("Configuration applied.\n");
        } catch (Exception ex) {
            outputArea.appendText("Failed to apply configuration: " + ex.getMessage() + "\n");
        }
    }

    private void doPing(String target) {
        try {
            IPAddress dstIp = IPAddress.fromString(target.trim());
            PingStatistics stats = host.ping(dstIp.toString(), topology);

            // Determine source IP for the formatter
            IPAddress srcIp = null;
            if (host.getHostInterface() != null && host.getHostInterface().getSubnet() != null) {
                srcIp = host.getHostInterface().getSubnet().networkAddress();
            }
            if (srcIp == null) {
                srcIp = new IPAddress(0, 0, 0, 0);
            }

            // Use the authentic VyOS formatter instead of standard toString()
            String formattedOutput = PingFormatter.format(dstIp, srcIp, 64, stats);
            outputArea.appendText(formattedOutput + "\n");
        } catch (Exception ex) {
            outputArea.appendText("Ping failed: " + ex.getMessage() + "\n");
        }
    }
}