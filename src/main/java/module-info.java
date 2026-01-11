module org.example.thesisuj {
	requires javafx.controls;
	requires javafx.fxml;
	requires static lombok;
	requires java.xml;
	requires org.jline;


	opens org.uj.routingemulator to javafx.fxml;
	opens org.uj.routingemulator.gui to javafx.fxml;
	exports org.uj.routingemulator;
	exports org.uj.routingemulator.gui;
	exports org.uj.routingemulator.host;
	opens org.uj.routingemulator.host to javafx.fxml;
	exports org.uj.routingemulator.router;
	exports org.uj.routingemulator.router.cli;
	exports org.uj.routingemulator.router.exceptions;
	exports org.uj.routingemulator.common;
	exports org.uj.routingemulator.switching;
}