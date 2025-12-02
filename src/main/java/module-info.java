module org.example.thesisuj {
	requires javafx.controls;
	requires javafx.fxml;
	requires static lombok;
	requires java.xml;


	opens org.example.thesisuj to javafx.fxml;
	exports org.example.thesisuj;
	exports org.example.thesisuj.host;
	opens org.example.thesisuj.host to javafx.fxml;
}