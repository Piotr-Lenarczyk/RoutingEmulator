package org.uj.routingemulator;

import javafx.application.Application;

/**
 * Class-path entry point for the packaged application.
 *
 * <p>The Java launcher treats a main class that extends {@link Application}
 * specially and requires JavaFX to be supplied as named modules. Keeping this
 * bootstrap class separate allows the shaded distribution JAR to be launched
 * with {@code java -jar}.</p>
 */
public final class Launcher {

	private Launcher() {
	}

	public static void main(String[] args) {
		Application.launch(Main.class, args);
	}
}