package org.uj.routingemulator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.cli.RouterCLIParser;

import java.io.IOException;
import java.util.Scanner;


public class Main extends Application {
	@Override
	public void start(Stage stage) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("hello-view.fxml"));
		Scene scene = new Scene(fxmlLoader.load(), 320, 240);
		stage.setTitle("Hello!");
		stage.setScene(scene);
		stage.show();
	}

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