package org.uj.routingemulator;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {
	@FXML
	private Label welcomeText;

	/**
	 * Handles the hello button click event.
	 * Updates the welcome text label with a greeting message.
	 */
	@FXML
	protected void onHelloButtonClick() {
		welcomeText.setText("Welcome to JavaFX Application!");
	}
}