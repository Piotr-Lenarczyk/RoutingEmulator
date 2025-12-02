package org.example.thesisuj;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.thesisuj.router.Router;
import org.example.thesisuj.router.RouterMode;
import org.example.thesisuj.router.cli.RouterCLIParser;

import java.io.IOException;
import java.util.Scanner;

/**
 * Main application class that provides both JavaFX GUI and CLI interface for router configuration.
 * The CLI provides a VyOS-style command-line interface for managing router configuration.
 */
public class Main extends Application {
	@Override
	public void start(Stage stage) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("hello-view.fxml"));
		Scene scene = new Scene(fxmlLoader.load(), 320, 240);
		stage.setTitle("Hello!");
		stage.setScene(scene);
		stage.show();
	}

	/**
	 * Main entry point for the application.
	 * Launches the JavaFX GUI and then starts the router CLI in interactive mode.
	 *
	 * @param args Command-line arguments (not used)
	 */
	public static void main(String[] args) {
		launch();
		RouterCLIParser parser = new RouterCLIParser();
		Scanner scanner = new Scanner(System.in);
		Router router = new Router("R1");
		while (true) {
			if (router.getMode() == RouterMode.OPERATIONAL) {
				System.out.print("vyos@vyos$ ");
			} else if (router.getMode() == RouterMode.CONFIGURATION) {
				System.out.print("vyos@vyos# ");
			}
			String input = scanner.nextLine();
			parser.executeCommand(input, router);
		}

	}
}