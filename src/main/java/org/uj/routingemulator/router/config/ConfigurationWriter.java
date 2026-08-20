package org.uj.routingemulator.router.config;

import org.uj.routingemulator.router.Router;

import java.io.IOException;
import java.nio.file.Path;

public interface ConfigurationWriter {
	void writeConfiguration(Router router, Path path, boolean isCommandFormat) throws IOException;
}