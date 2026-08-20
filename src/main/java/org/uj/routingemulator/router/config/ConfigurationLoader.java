package org.uj.routingemulator.router.config;

import org.uj.routingemulator.router.Router;

import java.io.IOException;
import java.nio.file.Path;

public interface ConfigurationLoader {
	void loadConfiguration(Router router, Path path) throws IOException;
}