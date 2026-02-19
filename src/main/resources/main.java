package main;

import controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import model.GraphModel;
import view.MainView;

public class Main extends Application {


    //Configuracion de la ventana
    private static final String APP_TITLE = "Simulador de propagacion de virus/informacion";
    private static final double WINDOW_WIDTH = 1280;
    private static final double WINDOW_HEIGHT = 800;
    private static final double MIN_WINDOW_WIDTH = 1024;
    private static final double MIN_WINDOW_HEIGHT = 680;

    //componentes del MVC
    private GraphModel model;
    private MainView view;
    private MainController controller;


    @Override

    public void start(Stage primaryStage) {

        //Configuracion inicial del GraphStream
        System.setProperty("org.graphstream.ui","javafx");

        //Iniciar Modelo
        model = new GraphModel();

        //Iniciar Vista
        view = new MainView(model);

        //Iniciar Controlador
        controller = new MainController(model, view);
        controller.initialize();

        Scene scene = new Scene(view.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);

        //Cargar hoja de estilos CSS
        loadStylesheet(scene);

        //Configurar la ventana principal
        configureStage(primaryStage, scene);

        primaryStage.show();

        //debug de inicio
        System.out.println("==========================================");
        System.out.println("  " + APP_TITLE);
        System.out.println("  Ventana: " + WINDOW_WIDTH + " x " + WINDOW_HEIGHT);
        System.out.println("  GraphStream renderer: JavaFX");
        System.out.println("==========================================");
    }

    private void configureStage(Stage stage, Scene scene) {
        stage.setTitle(APP_TITLE);
        stage.setScene(scene);
        stage.setMinWidth(MIN_WINDOW_WIDTH);
        stage.setMinHeight(MIN_WINDOW_HEIGHT);

        stage.setOnCloseRequest(event -> {
            System.out.println("[Main] -> cerrando aplicacion");
            if (controller != null) {
                controller.shutdown();
            }
            Platform.exit();
            System.exit(0);
        });

        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/icon.png")));
        } catch (Exception e) {
            System.out.println("ICONO NO ENCONTRADO");
        }
    }

    private void loadStylesheet(Scene scene) {
        try {
            String css = getClass().getResource("/styles/app.css").toExternalForm();
            scene.getStylesheets().add(css);
            System.out.println("[Main] Hoja de estilos cargada correctamente.");
        } catch (Exception e) {
            System.out.println("[Main] No se encontró /styles/app.css, usando estilos por defecto.");
        }
    }

    /**
     * Método de limpieza llamado por JavaFX al cerrar la aplicación.
     * Se usa como respaldo en caso de que setOnCloseRequest no se dispare.
     */
    @Override
    public void stop() throws Exception {
        System.out.println("[Main] stop() invocado por JavaFX.");
        if (controller != null) {
            controller.shutdown();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}