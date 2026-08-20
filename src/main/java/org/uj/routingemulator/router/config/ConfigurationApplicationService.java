package org.uj.routingemulator.router.config;

import org.uj.routingemulator.router.Router;

import java.io.IOException;
import java.nio.file.Path;

public class ConfigurationApplicationService {
	private final ConfigurationLoader loader;
	private final ConfigurationWriter writer;

	public ConfigurationApplicationService(ConfigurationLoader loader, ConfigurationWriter writer) {
		this.loader = loader;
		this.writer = writer;
	}

	public void loadConfiguration(Router router, Path path) throws IOException {
		loader.loadConfiguration(router, path);
	}

	public void saveConfiguration(Router router, Path path, boolean isCommandFormat) throws IOException {
		writer.writeConfiguration(router, path, isCommandFormat);
	}
}