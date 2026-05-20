package com.empresa.pos.ui;

import com.empresa.pos.dao.DatabaseManager;
import com.empresa.pos.sync.SyncWorker;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    private DatabaseManager databaseManager;
    private SyncWorker syncWorker;
    private Thread syncThread;

    @Override
    public void init() throws Exception {
        log.info("Inicializando POS Empresarial ERP...");
        databaseManager = new DatabaseManager();
        databaseManager.initializeDatabase();

        syncWorker = new SyncWorker();
        syncThread = new Thread(syncWorker, "sync-worker");
        syncThread.setDaemon(true);
        syncThread.start();
        log.info("SyncWorker iniciado en hilo demonio.");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/css/design-system.css").toExternalForm());

            primaryStage.setTitle("POS Empresarial ERP");
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> Platform.exit());
            primaryStage.show();
        } catch (Exception e) {
            log.error("Error fatal al iniciar la aplicación", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de inicio");
            alert.setHeaderText("No se pudo iniciar POS Empresarial ERP");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        log.info("Cerrando aplicación...");
        if (syncWorker != null) {
            syncWorker.stop();
        }
        if (syncThread != null) {
            syncThread.interrupt();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
