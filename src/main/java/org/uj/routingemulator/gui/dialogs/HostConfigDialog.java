package org.uj.routingemulator.gui.dialogs;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.uj.routingemulator.common.ping.PingStatistics;
import org.uj.routingemulator.gui.services.HostConfigurationService;
import org.uj.routingemulator.gui.services.PingApplicationService;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;

public class HostConfigDialog extends Dialog<Void> {
    private final Host host;
    private final HostConfigurationService hostConfigService;
    private final PingApplicationService pingService;

    private final TextField ipField = new TextField();
    private final TextField prefixField = new TextField();
    private final TextField gatewayField = new TextField();
    private final TextArea outputArea = new TextArea();

    public HostConfigDialog(Host host, HostConfigurationService hostConfigService, PingApplicationService pingService) {
        this.host = host;
        this.hostConfigService = hostConfigService;
        this.pingService = pingService;

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
        vbox.getChildren().addAll(grid, applyBtn, new Separator(), new Label("Ping target:"), pingTarget, pingBtn, new Separator(), outputArea);

        outputArea.setEditable(false);
        outputArea.setPrefRowCount(10);
        getDialogPane().setContent(vbox);

        HostInterface hi = host.getHostInterface();
        if (hi != null && hi.getInterfaceAddress() != null) {
            ipField.setText(hi.getInterfaceAddress().ipAddress().toString());
            prefixField.setText(String.valueOf(hi.getInterfaceAddress().subnetMask().shortMask()));
            if (hi.getDefaultGateway() != null) {
                gatewayField.setText(hi.getDefaultGateway().toString());
            }
        }
    }

    private void applyConfiguration() {
        try {
            hostConfigService.configureHost(
                    host,
                    ipField.getText().trim(),
                    prefixField.getText().trim(),
                    gatewayField.getText().trim()
            );
            outputArea.appendText("Configuration applied.\n");
        } catch (Exception ex) {
            outputArea.appendText("Failed to apply configuration: " + ex.getMessage() + "\n");
        }
    }

    private void doPing(String target) {
        try {
            PingStatistics stats = pingService.pingFromHost(host, target);
            outputArea.appendText(stats.toString() + "\n");
        } catch (Exception ex) {
            outputArea.appendText("Ping failed: " + ex.getMessage() + "\n");
        }
    }
}