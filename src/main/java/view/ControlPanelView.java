package view;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
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
import model.SimulationState;


public class ControlPanelView {
      // ═══════════════════════════════════════════════════════════════
    //  CONSTANTES
    // ═══════════════════════════════════════════════════════════════

    private static final double PANEL_HEIGHT      = 90.0;
    private static final double TRANSPORT_BTN_SIZE = 36.0;
    private static final double PLAY_BTN_SIZE      = 44.0;
    private static final String ICON_FONT         = "Segoe UI Emoji";

    // ═══════════════════════════════════════════════════════════════
    //  COMPONENTES
    // ═══════════════════════════════════════════════════════════════

    private final HBox root;
    private final GraphModel model;

    // ── Botones de transporte ─────────────────────────────────────
    private Button btnGoToStart;     // ⏮
    private Button btnStepBack;      // ⏪
    private Button btnPlayPause;     // ▶ / ⏸
    private Button btnStepForward;   // ⏩
    private Button btnGoToEnd;       // ⏭

    // ── Botón de inicio de simulación ─────────────────────────────
    private Button btnStartSim;      // ▶ SIMULAR

    // ── Indicador de iteración ────────────────────────────────────
    private Label  lblIteration;
    private Slider sliderTimeline;

    // ── Slider de velocidad ───────────────────────────────────────
    private Slider sliderSpeed;
    private Label  lblSpeed;

    // ── Slider de tasa de contagio ────────────────────────────────
    private Slider sliderContagion;
    private Label  lblContagion;

    // ── Spinner de tiempo de recuperación ─────────────────────────
    private Spinner<Integer> spinnerRecovery;
    private Label lblRecovery;

    // ── Exportación ───────────────────────────────────────────────
    private Button btnExportCSV;
    private Button btnExportTXT;

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public ControlPanelView(GraphModel model) {
        this.model = model;

        root = new HBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10, 20, 10, 20));
        root.setPrefHeight(PANEL_HEIGHT);
        root.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s transparent transparent transparent; " +
            "-fx-border-width: 1 0 0 0;",
            MainView.BG_SECONDARY, MainView.BORDER_COLOR
        ));

        buildTransportControls();
        buildTimelineSection();
        buildSpeedSection();
        buildContagionSection();
        buildRecoverySection();
        buildExportSection();

        assemblePanel();
        setupBindings();
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN: Controles de Transporte
    // ═══════════════════════════════════════════════════════════════

    private void buildTransportControls() {
        btnGoToStart   = createTransportButton("⏮", "Ir al inicio",
                             MainView.TEXT_SECONDARY);
        btnStepBack    = createTransportButton("⏪", "Paso anterior",
                             MainView.TEXT_SECONDARY);
        btnPlayPause   = createPlayButton();
        btnStepForward = createTransportButton("⏩", "Siguiente paso",
                             MainView.TEXT_SECONDARY);
        btnGoToEnd     = createTransportButton("⏭", "Ir al final",
                             MainView.TEXT_SECONDARY);

        btnStartSim = createStartSimButton();
    }

    /**
     * Crea un botón de transporte (⏮⏪⏩⏭) con estilo dark neón.
     */
    private Button createTransportButton(String icon, String tooltipText, String color) {
        Button btn = new Button(icon);
        btn.setPrefSize(TRANSPORT_BTN_SIZE, TRANSPORT_BTN_SIZE);
        btn.setMinSize(TRANSPORT_BTN_SIZE, TRANSPORT_BTN_SIZE);
        btn.setFont(Font.font(ICON_FONT, 14));
        btn.setTextFill(Color.web(color));
        btn.setCursor(javafx.scene.Cursor.HAND);

        String baseStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: 20; " +
            "-fx-border-color: %s; " +
            "-fx-border-radius: 20; " +
            "-fx-border-width: 1;",
            MainView.BG_TERTIARY, MainView.BORDER_COLOR
        );
        btn.setStyle(baseStyle);

        // Hover
        btn.setOnMouseEntered(e -> {
            btn.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-background-radius: 20; " +
                "-fx-border-color: %s; " +
                "-fx-border-radius: 20; " +
                "-fx-border-width: 1;",
                MainView.BG_HOVER, MainView.ACCENT_CYAN
            ));
            btn.setTextFill(Color.web(MainView.ACCENT_CYAN));
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(baseStyle);
            btn.setTextFill(Color.web(color));
        });

        // Tooltip
        Tooltip tooltip = createTooltip(tooltipText);
        btn.setTooltip(tooltip);

        return btn;
    }

    /**
     * Crea el botón Play/Pausa (más grande, con glow especial).
     */
    private Button createPlayButton() {
        Button btn = new Button("▶");
        btn.setPrefSize(PLAY_BTN_SIZE, PLAY_BTN_SIZE);
        btn.setMinSize(PLAY_BTN_SIZE, PLAY_BTN_SIZE);
        btn.setFont(Font.font(ICON_FONT, 18));
        btn.setTextFill(Color.web(MainView.ACCENT_GREEN));
        btn.setCursor(javafx.scene.Cursor.HAND);

        applyPlayButtonStyle(btn, false);

        // Glow permanente sutil
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(MainView.ACCENT_GREEN, 0.4));
        glow.setRadius(15);
        glow.setSpread(0.1);
        btn.setEffect(glow);

        // Hover
        btn.setOnMouseEntered(e -> {
            glow.setRadius(22);
            glow.setSpread(0.2);
            glow.setColor(Color.web(MainView.ACCENT_GREEN, 0.6));
        });

        btn.setOnMouseExited(e -> {
            glow.setRadius(15);
            glow.setSpread(0.1);
            glow.setColor(Color.web(MainView.ACCENT_GREEN, 0.4));
        });

        Tooltip tooltip = createTooltip("Play / Pausa");
        btn.setTooltip(tooltip);

        return btn;
    }

    /**
     * Aplica el estilo visual del botón play según si está en modo Play o Pausa.
     */
    private void applyPlayButtonStyle(Button btn, boolean isPlaying) {
        String accentColor = isPlaying ? MainView.ACCENT_YELLOW : MainView.ACCENT_GREEN;

        btn.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: 22; " +
            "-fx-border-color: %s; " +
            "-fx-border-radius: 22; " +
            "-fx-border-width: 2;",
            MainView.BG_TERTIARY, accentColor
        ));
        btn.setTextFill(Color.web(accentColor));

        if (btn.getEffect() instanceof DropShadow ds) {
            ds.setColor(Color.web(accentColor, 0.4));
        }
    }

    /**
     * Actualiza el ícono del botón Play/Pausa.
     */
    public void updatePlayPauseIcon(boolean isPlaying) {
        btnPlayPause.setText(isPlaying ? "⏸" : "▶");
        applyPlayButtonStyle(btnPlayPause, isPlaying);
    }

    /**
     * Crea el botón "SIMULAR" para iniciar la pre-computación.
     */
    private Button createStartSimButton() {
        Button btn = new Button("▶  SIMULAR");
        btn.setPrefHeight(PLAY_BTN_SIZE);
        btn.setPrefWidth(120);
        btn.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        btn.setTextFill(Color.web(MainView.BG_PRIMARY));
        btn.setCursor(javafx.scene.Cursor.HAND);

        String baseStyle = String.format(
            "-fx-background-color: linear-gradient(to right, %s, %s); " +
            "-fx-background-radius: %f; " +
            "-fx-border-color: transparent; " +
            "-fx-border-radius: %f;",
            MainView.ACCENT_GREEN, MainView.ACCENT_CYAN,
            MainView.CORNER_RADIUS, MainView.CORNER_RADIUS
        );
        btn.setStyle(baseStyle);

        // Glow
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(MainView.ACCENT_GREEN, 0.5));
        glow.setRadius(18);
        glow.setSpread(0.15);
        btn.setEffect(glow);

        // Hover
        btn.setOnMouseEntered(e -> {
            btn.setStyle(String.format(
                "-fx-background-color: linear-gradient(to right, %s, %s); " +
                "-fx-background-radius: %f; " +
                "-fx-border-color: white; " +
                "-fx-border-radius: %f; " +
                "-fx-border-width: 1;",
                MainView.ACCENT_CYAN, MainView.ACCENT_GREEN,
                MainView.CORNER_RADIUS, MainView.CORNER_RADIUS
            ));
            glow.setRadius(25);
            glow.setSpread(0.25);
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(baseStyle);
            glow.setRadius(18);
            glow.setSpread(0.15);
        });

        Tooltip tooltip = createTooltip("Pre-computar y ejecutar la simulación BFS");
        btn.setTooltip(tooltip);

        return btn;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN: Timeline (Iteración actual)
    // ═══════════════════════════════════════════════════════════════

    private void buildTimelineSection() {
        lblIteration = new Label("Iteración: 0 / 0");
        lblIteration.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        lblIteration.setTextFill(Color.web(MainView.TEXT_PRIMARY));

        sliderTimeline = new Slider(0, 1, 0);
        sliderTimeline.setPrefWidth(140);
        sliderTimeline.setBlockIncrement(1);
        sliderTimeline.setMajorTickUnit(1);
        sliderTimeline.setMinorTickCount(0);
        sliderTimeline.setSnapToTicks(true);
        applySliderStyle(sliderTimeline, MainView.ACCENT_CYAN);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN: Velocidad
    // ═══════════════════════════════════════════════════════════════

    private void buildSpeedSection() {
        lblSpeed = new Label("Velocidad: 0.8s");
        lblSpeed.setFont(Font.font("Consolas", 10));
        lblSpeed.setTextFill(Color.web(MainView.TEXT_SECONDARY));

        sliderSpeed = new Slider(
            SimulationState.SPEED_MIN,
            SimulationState.SPEED_MAX,
            SimulationState.SPEED_DEFAULT
        );
        sliderSpeed.setPrefWidth(110);
        sliderSpeed.setBlockIncrement(100);
        applySliderStyle(sliderSpeed, MainView.ACCENT_MAGENTA);

        // Invertir: slider a la izquierda = rápido (valor bajo de ms)
        // Label legible
        sliderSpeed.valueProperty().addListener((obs, old, newVal) -> {
            double seconds = newVal.doubleValue() / 1000.0;
            lblSpeed.setText(String.format("Velocidad: %.1fs", seconds));
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN: Tasa de Contagio
    // ═══════════════════════════════════════════════════════════════

    private void buildContagionSection() {
        lblContagion = new Label("Contagio: 100%");
        lblContagion.setFont(Font.font("Consolas", 10));
        lblContagion.setTextFill(Color.web(MainView.TEXT_SECONDARY));

        sliderContagion = new Slider(0.0, 1.0, 1.0);
        sliderContagion.setPrefWidth(110);
        sliderContagion.setBlockIncrement(0.05);
        applySliderStyle(sliderContagion, MainView.ACCENT_RED);

        sliderContagion.valueProperty().addListener((obs, old, newVal) -> {
            int pct = (int) (newVal.doubleValue() * 100);
            lblContagion.setText("Contagio: " + pct + "%");

            // Cambiar color del label según nivel
            if (pct >= 80) {
                lblContagion.setTextFill(Color.web(MainView.ACCENT_RED));
            } else if (pct >= 50) {
                lblContagion.setTextFill(Color.web(MainView.ACCENT_ORANGE));
            } else if (pct >= 30) {
                lblContagion.setTextFill(Color.web(MainView.ACCENT_YELLOW));
            } else {
                lblContagion.setTextFill(Color.web(MainView.ACCENT_GREEN));
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN: Tiempo de Recuperación
    // ═══════════════════════════════════════════════════════════════

    private void buildRecoverySection() {
        lblRecovery = new Label("Recuperación:");
        lblRecovery.setFont(Font.font("Consolas", 10));
        lblRecovery.setTextFill(Color.web(MainView.TEXT_SECONDARY));

        spinnerRecovery = new Spinner<>();
        SpinnerValueFactory<Integer> factory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5);
        spinnerRecovery.setValueFactory(factory);
        spinnerRecovery.setPrefWidth(70);
        spinnerRecovery.setPrefHeight(28);
        spinnerRecovery.setEditable(true);

        spinnerRecovery.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-font-family: 'Consolas'; " +
            "-fx-font-size: 11px; " +
            "-fx-text-fill: %s;",
            MainView.BG_TERTIARY, MainView.BORDER_COLOR, MainView.TEXT_PRIMARY
        ));

        Label lblIter = new Label("iter");
        lblIter.setFont(Font.font("Consolas", 10));
        lblIter.setTextFill(Color.web(MainView.TEXT_MUTED));
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN: Exportación
    // ═══════════════════════════════════════════════════════════════

    private void buildExportSection() {
        btnExportCSV = createExportButton("📁 CSV", "Exportar resultados a CSV");
        btnExportTXT = createExportButton("📄 TXT", "Exportar reporte a TXT");
    }

    private Button createExportButton(String text, String tooltipText) {
        Button btn = new Button(text);
        btn.setPrefHeight(30);
        btn.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
        btn.setTextFill(Color.web(MainView.TEXT_SECONDARY));
        btn.setCursor(javafx.scene.Cursor.HAND);

        String baseStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: %f; " +
            "-fx-border-color: %s; " +
            "-fx-border-radius: %f; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 4 12;",
            MainView.BG_TERTIARY, MainView.CORNER_RADIUS,
            MainView.BORDER_COLOR, MainView.CORNER_RADIUS
        );
        btn.setStyle(baseStyle);

        btn.setOnMouseEntered(e -> {
            btn.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-background-radius: %f; " +
                "-fx-border-color: %s; " +
                "-fx-border-radius: %f; " +
                "-fx-border-width: 1; " +
                "-fx-padding: 4 12;",
                MainView.BG_HOVER, MainView.CORNER_RADIUS,
                MainView.ACCENT_CYAN, MainView.CORNER_RADIUS
            ));
            btn.setTextFill(Color.web(MainView.ACCENT_CYAN));
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(baseStyle);
            btn.setTextFill(Color.web(MainView.TEXT_SECONDARY));
        });

        btn.setTooltip(createTooltip(tooltipText));
        return btn;
    }

    // ══════════════════===════════════════════════════════════════════
    //  ENSAMBLAJE DEL PANEL
    // ═══════════════════════════════════════════════════════════════

    private void assemblePanel() {
        // ── Grupo 1: Botón SIMULAR ────────────────────────────────
        VBox simGroup = new VBox(2, btnStartSim);
        simGroup.setAlignment(Pos.CENTER);

        // ── Grupo 2: Controles de transporte ──────────────────────
        HBox transportBox = new HBox(4,
            btnGoToStart, btnStepBack, btnPlayPause, btnStepForward, btnGoToEnd
        );
        transportBox.setAlignment(Pos.CENTER);

        // ── Grupo 3: Timeline ─────────────────────────────────────
        VBox timelineGroup = new VBox(4, lblIteration, sliderTimeline);
        timelineGroup.setAlignment(Pos.CENTER);
        timelineGroup.setPadding(new Insets(0, 4, 0, 4));

        // ── Grupo 4: Velocidad ────────────────────────────────────
        VBox speedGroup = new VBox(4, lblSpeed, sliderSpeed);
        speedGroup.setAlignment(Pos.CENTER);

        // ── Grupo 5: Contagio ─────────────────────────────────────
        VBox contagionGroup = new VBox(4, lblContagion, sliderContagion);
        contagionGroup.setAlignment(Pos.CENTER);

        // ── Grupo 6: Recuperación ─────────────────────────────────
        HBox recoveryInner = new HBox(4, spinnerRecovery, createMutedLabel("iter"));
        recoveryInner.setAlignment(Pos.CENTER_LEFT);
        VBox recoveryGroup = new VBox(4, lblRecovery, recoveryInner);
        recoveryGroup.setAlignment(Pos.CENTER);

        // ── Grupo 7: Exportación ──────────────────────────────────
        VBox exportGroup = new VBox(4, btnExportCSV, btnExportTXT);
        exportGroup.setAlignment(Pos.CENTER);

        // ── Separadores ───────────────────────────────────────────
        Separator sep1 = createPanelSeparator();
        Separator sep2 = createPanelSeparator();
        Separator sep3 = createPanelSeparator();
        Separator sep4 = createPanelSeparator();
        Separator sep5 = createPanelSeparator();
        Separator sep6 = createPanelSeparator();

        // ── Spacer ────────────────────────────────────────────────
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Ensamblar todo ────────────────────────────────────────
        root.getChildren().addAll(
            simGroup,
            sep1,
            transportBox,
            sep2,
            timelineGroup,
            sep3,
            speedGroup,
            sep4,
            contagionGroup,
            sep5,
            recoveryGroup,
            spacer,
            sep6,
            exportGroup
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  BINDINGS CON EL MODELO
    // ═══════════════════════════════════════════════════════════════

    private void setupBindings() {
        SimulationState simState = model.getSimulationState();

        // ── Timeline slider ↔ iteración actual ────────────────────
        simState.totalIterationsProperty().addListener((obs, old, newVal) -> {
            int total = newVal.intValue();
            sliderTimeline.setMax(Math.max(total - 1, 0));
        });

        simState.currentIterationIndexProperty().addListener((obs, old, newVal) -> {
            int current = newVal.intValue();
            int total   = simState.getTotalIterations();

            sliderTimeline.setValue(current);
            lblIteration.setText(String.format("Iteración: %d / %d",
                Math.max(current, 0), total));
        });

        // ── Speed slider → modelo ────────────────────────────────
        sliderSpeed.valueProperty().addListener((obs, old, newVal) ->
            simState.setPlaybackSpeed(newVal.doubleValue())
        );

        // ── Contagion slider → modelo ─────────────────────────────
        sliderContagion.valueProperty().addListener((obs, old, newVal) ->
            simState.setContagionRate(newVal.doubleValue())
        );

        // ── Recovery spinner → modelo ─────────────────────────────
        spinnerRecovery.valueProperty().addListener((obs, old, newVal) ->
            simState.setRecoveryTime(newVal)
        );

        // ── Estado de simulación → habilitar/deshabilitar botones ─
        simState.playbackStateProperty().addListener((obs, old, newVal) -> {
            boolean isIdle     = newVal == SimulationState.PlaybackState.IDLE;
            boolean isRunning  = newVal == SimulationState.PlaybackState.RUNNING;
            boolean isFinished = newVal == SimulationState.PlaybackState.FINISHED;

            // Botones de transporte solo activos cuando hay simulación
            btnGoToStart.setDisable(isIdle);
            btnStepBack.setDisable(isIdle);
            btnPlayPause.setDisable(isIdle);
            btnStepForward.setDisable(isIdle);
            btnGoToEnd.setDisable(isIdle);

            // Timeline solo activo con simulación
            sliderTimeline.setDisable(isIdle);

            // Play/Pause ícono
            updatePlayPauseIcon(isRunning);

            // Parámetros solo editables antes de simular
            sliderContagion.setDisable(!isIdle);
            spinnerRecovery.setDisable(!isIdle);

            // Exportación solo cuando hay datos
            btnExportCSV.setDisable(isIdle);
            btnExportTXT.setDisable(isIdle);

            // Botón simular visible solo en IDLE
            btnStartSim.setDisable(!isIdle);
            btnStartSim.setOpacity(isIdle ? 1.0 : 0.4);
        });

        // ── Precomputación → actualizar controles ─────────────────
        simState.precomputedProperty().addListener((obs, old, newVal) -> {
            if (newVal) {
                btnStartSim.setText("🔄 RE-SIMULAR");
                btnStartSim.setDisable(false);
                btnStartSim.setOpacity(1.0);
            }
        });

        // Estado inicial
        btnGoToStart.setDisable(true);
        btnStepBack.setDisable(true);
        btnPlayPause.setDisable(true);
        btnStepForward.setDisable(true);
        btnGoToEnd.setDisable(true);
        sliderTimeline.setDisable(true);
        btnExportCSV.setDisable(true);
        btnExportTXT.setDisable(true);
    }

    /**
     * Actualiza el label de iteración manualmente.
     */
    public void updateIterationLabel(int current, int total) {
        lblIteration.setText(String.format("Iteración: %d / %d", current, total));
    }

    // ═══════════════════════════════════════════════════════════════
    //  ESTILOS — Slider personalizado neón
    // ═══════════════════════════════════════════════════════════════

    /**
     * Aplica estilo dark neón a un slider de JavaFX.
     * Personaliza: track, thumb y colores.
     */
    private void applySliderStyle(Slider slider, String accentColor) {
        slider.setStyle(String.format(
            "-fx-control-inner-background: %s; " +
            "-fx-accent: %s;",
            MainView.BG_TERTIARY, accentColor
        ));

        // El styling profundo del slider se hace en el CSS externo,
        // pero configuramos colores base aquí para funcionalidad sin CSS.
        slider.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                slider.lookup(".track").setStyle(String.format(
                    "-fx-background-color: %s; " +
                    "-fx-background-radius: 4; " +
                    "-fx-pref-height: 6;",
                    MainView.BG_TERTIARY
                ));

                slider.lookup(".thumb").setStyle(String.format(
                    "-fx-background-color: %s; " +
                    "-fx-background-radius: 8; " +
                    "-fx-pref-width: 16; " +
                    "-fx-pref-height: 16; " +
                    "-fx-effect: dropshadow(gaussian, %s, 8, 0.3, 0, 0);",
                    accentColor, accentColor
                ));

                // Track relleno (filled portion)
                var coloredTrack = slider.lookup(".colored-track");
                if (coloredTrack != null) {
                    coloredTrack.setStyle(String.format(
                        "-fx-background-color: %s; " +
                        "-fx-background-radius: 4;",
                        accentColor
                    ));
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILIDADES DE ESTILO
    // ═══════════════════════════════════════════════════════════════

    private Separator createPanelSeparator() {
        Separator sep = new Separator(Orientation.VERTICAL);
        sep.setPadding(new Insets(8, 0, 8, 0));
        sep.setStyle(String.format(
            "-fx-background-color: %s;", MainView.BORDER_COLOR
        ));
        return sep;
    }

    private Label createMutedLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Consolas", 10));
        label.setTextFill(Color.web(MainView.TEXT_MUTED));
        return label;
    }

    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(400));
        tooltip.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: %s; " +
            "-fx-background-radius: 6; " +
            "-fx-font-family: 'Consolas'; " +
            "-fx-font-size: 11px; " +
            "-fx-padding: 8;",
            MainView.BG_TERTIARY, MainView.TEXT_PRIMARY
        ));
        return tooltip;
    }

    // ═══════════════════════════════════════════════════════════════
    //  GETTERS — Para el Controller
    // ═===═════════════════════════════════════════════════════════════

    public HBox   getRoot()              { return root; }

    // Transporte
    public Button getBtnGoToStart()      { return btnGoToStart; }
    public Button getBtnStepBack()       { return btnStepBack; }
    public Button getBtnPlayPause()      { return btnPlayPause; }
    public Button getBtnStepForward()    { return btnStepForward; }
    public Button getBtnGoToEnd()        { return btnGoToEnd; }
    public Button getBtnStartSim()       { return btnStartSim; }

    // Timeline
    public Slider getSliderTimeline()    { return sliderTimeline; }
    public Label  getLblIteration()      { return lblIteration; }

    // Parámetros
    public Slider          getSliderSpeed()       { return sliderSpeed; }
    public Slider          getSliderContagion()   { return sliderContagion; }
    public Spinner<Integer> getSpinnerRecovery()  { return spinnerRecovery; }

    // Exportación
    public Button getBtnExportCSV()      { return btnExportCSV; }
    public Button getBtnExportTXT()      { return btnExportTXT; }
}
