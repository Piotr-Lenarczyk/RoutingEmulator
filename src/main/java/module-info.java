module org.example.thesisuj {
	requires javafx.controls;
	requires javafx.fxml;
	requires static lombok;
	requires org.jline;
	requires java.logging;


	opens org.uj.routingemulator to javafx.fxml;
	exports org.uj.routingemulator;
	exports org.uj.routingemulator.host;
	opens org.uj.routingemulator.host to javafx.fxml;
	exports org.uj.routingemulator.router.cli;
	exports org.uj.routingemulator.router.exceptions;
	exports org.uj.routingemulator.switching;
	exports org.uj.routingemulator.common.exceptions;
	exports org.uj.routingemulator.common.addressing;
	exports org.uj.routingemulator.common.packet;
	exports org.uj.routingemulator.common.topology;
	exports org.uj.routingemulator.common.forwarding;
	exports org.uj.routingemulator.common.ping;
	exports org.uj.routingemulator.router.model;
	exports org.uj.routingemulator.router.session;
	exports org.uj.routingemulator.gui.dialogs;
	opens org.uj.routingemulator.gui.dialogs to javafx.fxml;
	exports org.uj.routingemulator.gui.services;
	opens org.uj.routingemulator.gui.services to javafx.fxml;
	exports org.uj.routingemulator.gui.viewmodel;
	opens org.uj.routingemulator.gui.viewmodel to javafx.fxml;
	exports org.uj.routingemulator.common.topology.exceptions;
}