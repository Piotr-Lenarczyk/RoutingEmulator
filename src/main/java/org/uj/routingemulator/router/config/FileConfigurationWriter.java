package org.uj.routingemulator.router.config;

import org.uj.routingemulator.router.Router;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileConfigurationWriter implements ConfigurationWriter {
	@Override
	public void writeConfiguration(Router router, Path path, boolean isCommandFormat) throws IOException {
		ConfigurationGenerator generator = isCommandFormat ?
				ConfigurationFactory.getCommandGenerator() :
				ConfigurationFactory.getHierarchicalGenerator();
		String config = generator.generateConfiguration(router);
		Files.writeString(path, config);
	}
}