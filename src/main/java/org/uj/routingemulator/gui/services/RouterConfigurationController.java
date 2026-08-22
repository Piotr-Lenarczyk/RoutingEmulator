package org.uj.routingemulator.gui.services;

import org.uj.routingemulator.router.config.ConfigurationFactory;
import org.uj.routingemulator.router.config.ConfigurationGenerator;
import org.uj.routingemulator.router.config.ConfigurationParser;
import org.uj.routingemulator.router.model.Router;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RouterConfigurationController {

	public void loadConfigurationFromFile(Router router, Path path) throws IOException {
		String config = Files.readString(path);
		ConfigurationParser parser = ConfigurationFactory.getParser(config);
		parser.loadConfiguration(router, config);
	}

	public void saveConfigurationToFile(Router router, Path path, boolean isCommandFormat) throws IOException {
		ConfigurationGenerator generator = isCommandFormat ?
				ConfigurationFactory.getCommandGenerator() :
				ConfigurationFactory.getHierarchicalGenerator();
		String config = generator.generateConfiguration(router);
		Files.writeString(path, config);
	}
}