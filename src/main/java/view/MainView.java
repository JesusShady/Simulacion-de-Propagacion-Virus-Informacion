package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import model.GraphModel;


public class MainView {
 


    //CONSTANTES DE ESTILOS

    // Fondos
    public static final String BG_PRIMARY    = "#0D1117";   // Fondo principal (casi negro)
    public static final String BG_SECONDARY  = "#161B22";   // Paneles laterales
    public static final String BG_TERTIARY   = "#1C2333";   // Cards/elementos elevados
    public static final String BG_HOVER      = "#252D3A";   // Hover sobre elementos
    public static final String BG_CANVAS     = "#0A0E14";   // Fondo del canvas (negro profundo)

    // Acentos
    public static final String ACCENT_CYAN     = "#00E5FF";   // Acento principal (cyan neón)
    public static final String ACCENT_MAGENTA  = "#D500F9";   // Acento secundario (magenta)
    public static final String ACCENT_GREEN    = "#00E676";   // Éxito / Recuperado
    public static final String ACCENT_RED      = "#FF1744";   // Peligro / Infectado
    public static final String ACCENT_YELLOW   = "#FFEA00";   // Advertencia
    public static final String ACCENT_ORANGE   = "#FF9100";   // Nivel medio

    // Texto
    public static final String TEXT_PRIMARY    = "#E6EDF3";   // Texto principal (blanco suave)
    public static final String TEXT_SECONDARY  = "#8B949E";   // Texto secundario (gris)
    public static final String TEXT_MUTED      = "#484F58";   // Texto deshabilitado

    // Bordes
    public static final String BORDER_COLOR    = "#30363D";   // Bordes sutiles
    public static final String BORDER_FOCUS    = "#58A6FF";   // Borde con foco

    // Dimensiones
    public static final double TOOLBAR_WIDTH    = 220.0;
    public static final double DASHBOARD_WIDTH  = 280.0;
    public static final double HEADER_HEIGHT    = 52.0;
    public static final double CONTROL_HEIGHT   = 80.0;
    public static final double BUTTON_HEIGHT    = 36.0;
    public static final double CORNER_RADIUS    = 8.0;



    //Componentes de layout

     /** Layout raíz de toda la aplicación */
    private final BorderPane root;

    /** Referencia al modelo (solo lectura / observación) */
    private final GraphModel model;

    // ── Sub-vistas ────────────────────────────────────────────────
    private GraphCanvasView     canvasView;
    private ControlPanelView    controlPanel;
    private StatsDashboardView  dashboard;

    // ── Header ────────────────────────────────────────────────────
    private HBox    headerBar;
    private Label   titleLabel;
    private Label   statusLabel;
    private Label   nodeCountLabel;
    private Label   edgeCountLabel;

    // ── Toolbar (Left) ────────────────────────────────────────────
    private VBox    toolbar;
    private Button  btnCreateNode;
    private Button  btnCreateEdge;
    private Button  btnTopology;
    private Button  btnQuarantine;
    private Button  btnPatientZero;
    private Button  btnSuperSpreader;
    private Button  btnFirewall;
    private Button  btnClearGraph;
    private Button  btnResetSim;


    //constructor

    public MainView(GraphModel model){
         this.model = model;
        this.root  = new BorderPane();

        // Construir cada sección
        buildHeader();
        buildToolbar();
        buildCanvas();
        buildControlPanel();
        buildDashboard();

        // Ensamblar en el BorderPane
        root.setTop(headerBar);
        root.setLeft(toolbar);
        root.setCenter(canvasView.getRoot());
        root.setBottom(controlPanel.getRoot());
        root.setRight(dashboard.getRoot());

        // Estilo global del root
        root.setStyle(String.format(
            "-fx-background-color: %s;", BG_PRIMARY
        ));

        // Bindings del header con el modelo
        setupHeaderBindings();
    }



    //===============================
    //Header bar
    //===============================

    private void buildControlPanel(){
        controlPanel = new ControlPanelView(model);
    }

    private void buildHeader() {
        headerBar = new HBox(12);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(0, 20, 0, 20));
        headerBar.setPrefHeight(HEADER_HEIGHT);
        headerBar.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: transparent transparent %s transparent; " +
            "-fx-border-width: 0 0 1 0;",
            BG_SECONDARY, BORDER_COLOR
        ));

        // ── Logo + Título ─────────────────────────────────────────
        Label logoIcon = new Label("☣");
        logoIcon.setFont(Font.font("Segoe UI Emoji", 22));
        logoIcon.setTextFill(Color.web(ACCENT_CYAN));
        logoIcon.setEffect(new Glow(0.8));

        titleLabel = new Label("BFS VIRUS PROPAGATION");
        titleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web(ACCENT_CYAN));
        titleLabel.setEffect(new Glow(0.3));

        Label subtitleLabel = new Label("SIMULATOR");
        subtitleLabel.setFont(Font.font("Consolas", FontWeight.LIGHT, 16));
        subtitleLabel.setTextFill(Color.web(TEXT_SECONDARY));

        HBox titleBox = new HBox(4, logoIcon, titleLabel, subtitleLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // ── Spacer ────────────────────────────────────────────────
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Status ────────────────────────────────────────────────
        statusLabel = new Label("● IDLE");
        statusLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        statusLabel.setTextFill(Color.web(TEXT_SECONDARY));
        statusLabel.setPadding(new Insets(4, 12, 4, 12));
        statusLabel.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: 12;",
            BG_TERTIARY
        ));

        // ── Contadores ────────────────────────────────────────────
        nodeCountLabel = createCounterLabel("⬡ Nodos: 0");
        edgeCountLabel = createCounterLabel("⟷ Aristas: 0");

        Separator sep1 = createVerticalSeparator();
        Separator sep2 = createVerticalSeparator();

        headerBar.getChildren().addAll(
            titleBox, spacer, statusLabel, sep1, nodeCountLabel, sep2, edgeCountLabel
        );
    }

    private Label createCounterLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Consolas", 12));
        label.setTextFill(Color.web(TEXT_SECONDARY));
        label.setPadding(new Insets(4, 8, 4, 8));
        return label;
    }

    private Separator createVerticalSeparator() {
        Separator sep = new Separator();
        sep.setOrientation(javafx.geometry.Orientation.VERTICAL);
        sep.setPadding(new Insets(8, 0, 8, 0));
        sep.setStyle(String.format("-fx-background-color: %s;", BORDER_COLOR));
        return sep;
    }

    /**
     * Conecta los labels del header con las propiedades del modelo.
     */
    private void setupHeaderBindings() {
        // Contador de nodos reactivo
        model.nodeCountProperty().addListener((obs, old, newVal) ->
            nodeCountLabel.setText("⬡ Nodos: " + newVal)
        );

        // Contador de aristas reactivo
        model.edgeCountProperty().addListener((obs, old, newVal) ->
            edgeCountLabel.setText("⟷ Aristas: " + newVal)
        );

        // Estado de la simulación
        model.getSimulationState().playbackStateProperty().addListener((obs, old, newVal) -> {
            switch (newVal) {
                case IDLE -> updateStatus("● IDLE", TEXT_SECONDARY);
                case RUNNING -> updateStatus("● RUNNING", ACCENT_GREEN);
                case PAUSED -> updateStatus("● PAUSED", ACCENT_YELLOW);
                case FINISHED -> updateStatus("● FINISHED", ACCENT_CYAN);
            }
        });
    }

    /**
     * Actualiza el label de status con color.
     */
    public void updateStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setTextFill(Color.web(color));
    }

    //===============================
    //Tool bar
    //===============================

     private void buildToolbar() {
        toolbar = new VBox(6);
        toolbar.setPrefWidth(TOOLBAR_WIDTH);
        toolbar.setPadding(new Insets(12));
        toolbar.setAlignment(Pos.TOP_CENTER);
        toolbar.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: transparent %s transparent transparent; " +
            "-fx-border-width: 0 1 0 0;",
            BG_SECONDARY, BORDER_COLOR
        ));

        // ── Sección: Creación ─────────────────────────────────────
        Label sectionCreate = createSectionLabel("CREACIÓN DE RED");

        btnCreateNode = createToolbarButton("⬡  Crear Nodo", ACCENT_CYAN,
            "Clic en el canvas para crear un nodo");

        btnCreateEdge = createToolbarButton("⟷  Crear Arista", ACCENT_CYAN,
            "Arrastra de un nodo a otro para conectarlos");

        btnTopology = createToolbarButton("🌐  Topologías", ACCENT_MAGENTA,
            "Generar redes automáticas (Estrella, Malla, etc.)");

        // ── Sección: Simulación ───────────────────────────────────
        Label sectionSim = createSectionLabel("SIMULACIÓN");

        btnPatientZero = createToolbarButton("☣  Paciente Cero", ACCENT_RED,
            "Seleccionar nodos de inicio de la infección");

        btnQuarantine = createToolbarButton("⊘  Cuarentena", ACCENT_YELLOW,
            "Marcar nodos como inmunes / aislados");

        btnFirewall = createToolbarButton("🔥  Cortafuegos", ACCENT_ORANGE,
            "Desactivar aristas para bloquear propagación");

        // ── Sección: Análisis ─────────────────────────────────────
        Label sectionAnalysis = createSectionLabel("ANÁLISIS");

        btnSuperSpreader = createToolbarButton("👑  Súper Propagador", ACCENT_MAGENTA,
            "Identificar el nodo más peligroso de la red");

        // ── Spacer ────────────────────────────────────────────────
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // ── Sección: Acciones ─────────────────────────────────────
        Label sectionActions = createSectionLabel("ACCIONES");

        btnResetSim = createToolbarButton("🔄  Reset Simulación", TEXT_SECONDARY,
            "Reiniciar simulación manteniendo el grafo");

        btnClearGraph = createToolbarButton("🗑  Limpiar Todo", ACCENT_RED,
            "Eliminar todos los nodos y aristas");
        // Estilo especial para el botón destructivo
        btnClearGraph.setStyle(btnClearGraph.getStyle() +
            String.format("-fx-border-color: %s; -fx-border-width: 1; -fx-border-radius: %f;",
                ACCENT_RED, CORNER_RADIUS));

        // ── Ensamblar toolbar ─────────────────────────────────────
        toolbar.getChildren().addAll(
            sectionCreate,
            btnCreateNode,
            btnCreateEdge,
            btnTopology,
            createToolbarSeparator(),
            sectionSim,
            btnPatientZero,
            btnQuarantine,
            btnFirewall,
            createToolbarSeparator(),
            sectionAnalysis,
            btnSuperSpreader,
            spacer,
            createToolbarSeparator(),
            sectionActions,
            btnResetSim,
            btnClearGraph
        );
    }

    /**
     * Crea un botón estilizado para el toolbar con efecto hover neón.
     */
    private Button createToolbarButton(String text, String accentColor, String tooltipText) {
        Button btn = new Button(text);
        btn.setPrefWidth(TOOLBAR_WIDTH - 24);
        btn.setPrefHeight(BUTTON_HEIGHT);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        btn.setTextFill(Color.web(TEXT_PRIMARY));
        btn.setCursor(javafx.scene.Cursor.HAND);

        // Estilo base
        String baseStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %f; " +
            "-fx-border-color: transparent; " +
            "-fx-border-radius: %f; " +
            "-fx-padding: 0 12;",
            BG_TERTIARY, CORNER_RADIUS, CORNER_RADIUS
        );
        btn.setStyle(baseStyle);

        // ── Efectos hover ─────────────────────────────────────────
        DropShadow glowShadow = new DropShadow();
        glowShadow.setColor(Color.web(accentColor, 0.4));
        glowShadow.setRadius(12);
        glowShadow.setSpread(0.1);

        btn.setOnMouseEntered(e -> {
            btn.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-background-radius: %f; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: %f; " +
                "-fx-padding: 0 12;",
                BG_HOVER, CORNER_RADIUS, accentColor, CORNER_RADIUS
            ));
            btn.setTextFill(Color.web(accentColor));
            btn.setEffect(glowShadow);
        });

        btn.setOnMouseExited(e -> {
            // Solo resetear si no está en modo activo
            if (!btn.getStyleClass().contains("active-mode")) {
                btn.setStyle(baseStyle);
                btn.setTextFill(Color.web(TEXT_PRIMARY));
                btn.setEffect(null);
            }
        });

        // ── Tooltip ───────────────────────────────────────────────
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setShowDelay(Duration.millis(400));
        tooltip.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: %s; " +
            "-fx-background-radius: 6; " +
            "-fx-font-family: 'Consolas'; " +
            "-fx-font-size: 11px; " +
            "-fx-padding: 8;",
            BG_TERTIARY, TEXT_PRIMARY
        ));
        btn.setTooltip(tooltip);

        return btn;
    }


    public void setButtonActive(Button btn, boolean active, String accentColor) {
        if (active) {
            btn.getStyleClass().add("active-mode");
            btn.setStyle(String.format(
                "-fx-background-color: %s22; " +  // Color con alfa baja
                "-fx-background-radius: %f; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: %f; " +
                "-fx-padding: 0 12;",
                accentColor, CORNER_RADIUS, accentColor, CORNER_RADIUS
            ));
            btn.setTextFill(Color.web(accentColor));
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web(accentColor, 0.5));
            glow.setRadius(15);
            glow.setSpread(0.15);
            btn.setEffect(glow);
        } else {
            btn.getStyleClass().remove("active-mode");
            btn.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-background-radius: %f; " +
                "-fx-border-color: transparent; " +
                "-fx-border-radius: %f; " +
                "-fx-padding: 0 12;",
                BG_TERTIARY, CORNER_RADIUS, CORNER_RADIUS
            ));
            btn.setTextFill(Color.web(TEXT_PRIMARY));
            btn.setEffect(null);
        }
    }

    /**
     * Desactiva todos los modos toggle de la toolbar.
     */
    public void clearAllModes() {
        setButtonActive(btnCreateNode,  false, ACCENT_CYAN);
        setButtonActive(btnCreateEdge,  false, ACCENT_CYAN);
        setButtonActive(btnPatientZero, false, ACCENT_RED);
        setButtonActive(btnQuarantine,  false, ACCENT_YELLOW);
        setButtonActive(btnFirewall,    false, ACCENT_ORANGE);
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        label.setTextFill(Color.web(TEXT_MUTED));
        label.setPadding(new Insets(12, 0, 4, 4));
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Separator createToolbarSeparator() {
        Separator sep = new Separator();
        sep.setPadding(new Insets(4, 0, 4, 0));
        sep.setStyle(String.format(
            "-fx-background-color: %s;", BORDER_COLOR
        ));
        return sep;
    }



    //Canvas central

    private void buildCanvas(){
        canvasView = new GraphCanvasView(model);
    }

    //Control panel

    private void builControlPanel(){
        controlPanel = new ControlPanelView(model);
    }

    //Dashboard derecho
    private void buildDashboard(){
        dashboard = new StatsDashboardView(model);
    }

    //getters

    // ── Layout ────────────────────────────────────────────────────
    public BorderPane getRoot()                 { return root; }

    // ── Sub-vistas ────────────────────────────────────────────────
    public GraphCanvasView    getCanvasView()    { return canvasView; }
    public ControlPanelView   getControlPanel()  { return controlPanel; }
    public StatsDashboardView getDashboard()     { return dashboard; }

    // ── Header ────────────────────────────────────────────────────
    public Label getStatusLabel()       { return statusLabel; }
    public Label getNodeCountLabel()    { return nodeCountLabel; }
    public Label getEdgeCountLabel()    { return edgeCountLabel; }

    // ── Toolbar Buttons ───────────────────────────────────────────
    public Button getBtnCreateNode()    { return btnCreateNode; }
    public Button getBtnCreateEdge()    { return btnCreateEdge; }
    public Button getBtnTopology()      { return btnTopology; }
    public Button getBtnQuarantine()    { return btnQuarantine; }
    public Button getBtnPatientZero()   { return btnPatientZero; }
    public Button getBtnSuperSpreader() { return btnSuperSpreader; }
    public Button getBtnFirewall()      { return btnFirewall; }
    public Button getBtnClearGraph()    { return btnClearGraph; }
    public Button getBtnResetSim()      { return btnResetSim; }

}
