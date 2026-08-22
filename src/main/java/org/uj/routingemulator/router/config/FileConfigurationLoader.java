package org.uj.routingemulator.router.config;

import org.uj.routingemulator.router.model.Router;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileConfigurationLoader implements ConfigurationLoader {
	@Override
	public void loadConfiguration(Router router, Path path) throws IOException {
		String config = Files.readString(path);
		ConfigurationParser parser = ConfigurationFactory.getParser(config);
		parser.loadConfiguration(router, config);
	}
}