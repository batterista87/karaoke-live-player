package it.andrea.karaokeliveplayer;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // Prima avvio la finestra...
        stage.setTitle("Karaoke Live Player");

        // La massimizzo SUBITO (vale anche prima di mostrare la scena)
        stage.setMaximized(true);

        // ... e poi lascio che KaraokePlayer costruisca tutto il layout
        new KaraokePlayer().start(stage);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}