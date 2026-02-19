package controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.GraphModel;
import model.NodeData;
import model.SimulationState;
import model.SimulationState.IterationSnapshot;
import model.TopologyGenerator.TopologyResult;
import model.TopologyGenerator.TopologyType;
import view.MainView;
import view.TopologyDialogView;

import java.util.Optional;

public class MainController {

    // ═══════════════════════════════════════════════════════════════
    //  REFERENCIAS MVC
    // ═════════════��═════════════════════════════════════════════════

    private final GraphModel model;
    private final MainView   view;

    // ═══════════════════════════════════════════════════════════════
    //  SUB-CONTROLADORES
    // ═══════════════════════════════════════════════════════════════

    private GraphController      graphController;
    private SimulationController simulationController;
    private ExportController     exportController;

    // ═══════════════════════════════════════════════════════════════
    //  LOOP DE REPRODUCCIÓN AUTOMÁTICA
    // ═══════════════════════════════════════════════════════════════

    /** Timeline de JavaFX para la reproducción automática */
    private Timeline playbackTimeline;

    /** ¿El sistema está en proceso de shutdown? */
    private boolean shuttingDown = false;

    // ═══════════════════════════════════════════════════════════════
    //  ESTADO DE MODOS DE INTERACCIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Modos mutuamente excluyentes de interacción con el canvas.
     */
    private enum InteractionMode {
        NONE,           // Sin modo especial (selección por defecto)
        CREATE_NODE,    // Click en canvas = crear nodo
        CREATE_EDGE,    // Drag entre nodos = crear arista
        PATIENT_ZERO,   // Click en nodo = marcar como paciente cero
        QUARANTINE,     // Click en nodo = poner en cuarentena
        FIREWALL        // Click en arista = cortafuegos
    }

    private InteractionMode currentMode = InteractionMode.NONE;

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══���════════════════════════════════════════════════════════════

    public MainController(GraphModel model, MainView view) {
        this.model = model;
        this.view  = view;
    }

    // ═══════════════════════════════════════════════════════════════
    //  INICIALIZACIÓN — Cablear todo
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inicializa todos los event handlers y sub-controladores.
     * Llamado una sola vez desde Main.start().
     */
    public void initialize() {
        System.out.println("[MainController] Inicializando controladores...");

        // Crear sub-controladores
        graphController      = new GraphController(model, view);
        simulationController = new SimulationController(model, view);
        exportController     = new ExportController(model, view);

        // Inicializar sub-controladores
        graphController.setMainController(this);
        graphController.initialize();

        // Cablear eventos de la toolbar
        wireToolbarEvents();

        // Cablear controles de reproducción
        wirePlaybackControls();

        // Cablear el timeline slider
        wireTimelineSlider();

        // Configurar el loop de reproducción
        setupPlaybackLoop();

        System.out.println("[MainController] ✓ Todos los controladores inicializados.");
    }

    // ═══════════════════════════════════════════════════════════════
    //  TOOLBAR — Eventos de los botones laterales
    // ═══════════════════════════════════════════════════════════════

    private void wireToolbarEvents() {

        // ── Crear Nodo ────────────────────────────────────────────
        view.getBtnCreateNode().setOnAction(e -> {
            toggleMode(InteractionMode.CREATE_NODE);
        });

        // ── Crear Arista ──────────────────────────────────────────
        view.getBtnCreateEdge().setOnAction(e -> {
            toggleMode(InteractionMode.CREATE_EDGE);
        });

        // ── Topologías ────────────────────────────────────────────
        view.getBtnTopology().setOnAction(e -> {
            showTopologyDialog();
        });

        // ── Paciente Cero ─────────────────────────────────────────
        view.getBtnPatientZero().setOnAction(e -> {
            toggleMode(InteractionMode.PATIENT_ZERO);
        });

        // ── Cuarentena ────────────────────────────────────────────
        view.getBtnQuarantine().setOnAction(e -> {
            toggleMode(InteractionMode.QUARANTINE);
        });

        // ── Cortafuegos ───────────────────────────────────────────
        view.getBtnFirewall().setOnAction(e -> {
            toggleMode(InteractionMode.FIREWALL);
        });

        // ── Súper Propagador ──────────────────────────────────────
        view.getBtnSuperSpreader().setOnAction(e -> {
            identifySuperSpreader();
        });

        // ── Reset Simulación ──────────────────────────────────────
        view.getBtnResetSim().setOnAction(e -> {
            resetSimulation();
        });

        // ── Limpiar Todo ──────────────────────────────────────────
        view.getBtnClearGraph().setOnAction(e -> {
            confirmAndClearGraph();
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  GESTIÓN DE MODOS DE INTERACCIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Alterna un modo de interacción (toggle on/off).
     * Los modos son mutuamente excluyentes.
     *
     * @param mode el modo a activar/desactivar
     */
    private void toggleMode(InteractionMode mode) {
        if (currentMode == mode) {
            // Desactivar modo actual
            deactivateCurrentMode();
        } else {
            // Desactivar modo anterior y activar el nuevo
            deactivateCurrentMode();
            activateMode(mode);
        }
    }

    /**
     * Activa un modo de interacción.
     */
    private void activateMode(InteractionMode mode) {
        currentMode = mode;

        switch (mode) {
            case CREATE_NODE -> {
                model.setEdgeCreationMode(false);
                model.setPatientZeroMode(false);
                model.setQuarantineMode(false);
                view.setButtonActive(view.getBtnCreateNode(), true, MainView.ACCENT_CYAN);
            }
            case CREATE_EDGE -> {
                model.setEdgeCreationMode(true);
                model.setPatientZeroMode(false);
                model.setQuarantineMode(false);
                view.setButtonActive(view.getBtnCreateEdge(), true, MainView.ACCENT_CYAN);
            }
            case PATIENT_ZERO -> {
                model.setEdgeCreationMode(false);
                model.setPatientZeroMode(true);
                model.setQuarantineMode(false);
                view.setButtonActive(view.getBtnPatientZero(), true, MainView.ACCENT_RED);
            }
            case QUARANTINE -> {
                model.setEdgeCreationMode(false);
                model.setPatientZeroMode(false);
                model.setQuarantineMode(true);
                view.setButtonActive(view.getBtnQuarantine(), true, MainView.ACCENT_YELLOW);
            }
            case FIREWALL -> {
                model.setEdgeCreationMode(false);
                model.setPatientZeroMode(false);
                model.setQuarantineMode(false);
                view.setButtonActive(view.getBtnFirewall(), true, MainView.ACCENT_ORANGE);
            }
            default -> {}
        }

        System.out.println("[MainController] Modo activado: " + mode);
    }

    /**
     * Desactiva el modo de interacción actual.
     */
    private void deactivateCurrentMode() {
        currentMode = InteractionMode.NONE;
        model.setEdgeCreationMode(false);
        model.setPatientZeroMode(false);
        model.setQuarantineMode(false);
        view.clearAllModes();

        System.out.println("[MainController] Modo desactivado → NONE");
    }

    /**
     * @return el modo de interacción actual
     */
    public InteractionMode getCurrentMode() {
        return currentMode;
    }

    /**
     * @return true si estamos en modo de crear nodos
     */
    public boolean isCreateNodeMode() {
        return currentMode == InteractionMode.CREATE_NODE;
    }

    /**
     * @return true si estamos en modo cortafuegos
     */
    public boolean isFirewallMode() {
        return currentMode == InteractionMode.FIREWALL;
    }

    // ═══════════════════════════════════════════════════════════════
    //  TOPOLOGÍAS — Diálogo y generación
    // ══════════════════���════════════════════════════════════════════

    /**
     * Muestra el diálogo de selección de topología y genera la red.
     */
    private void showTopologyDialog() {
        // Obtener el Stage principal
        Stage primaryStage = (Stage) view.getRoot().getScene().getWindow();

        // Crear y mostrar el diálogo
        TopologyDialogView dialog = new TopologyDialogView(primaryStage);
        boolean confirmed = dialog.showAndWait();

        if (confirmed) {
            TopologyType type = dialog.getSelectedType();
            int nodeCount     = dialog.getSelectedNodeCount();

            System.out.println("[MainController] Generando topología: " + type
                + " con " + nodeCount + " nodos");

            // Generar la topología en el modelo
            double canvasW = view.getCanvasView().getCanvasWidth();
            double canvasH = view.getCanvasView().getCanvasHeight();

            TopologyResult result = model.generateTopology(type, nodeCount, canvasW, canvasH);

            // Limpiar dashboard
            view.getDashboard().clearAll();

            // Forzar re-render del canvas
            view.getCanvasView().forceRender();

            // Notificar
            view.updateStatus("● RED GENERADA", MainView.ACCENT_GREEN);

            System.out.println("[MainController] ✓ Topología generada: "
                + result.nodes().size() + " nodos, "
                + result.edges().size() + " aristas");

            // Si hay un hub sugerido, informar
            if (result.suggestedHubId() != null) {
                System.out.println("[MainController] Hub sugerido (súper propagador): "
                    + result.suggestedHubId());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ANÁLISIS — Súper Propagador
    // ═══════════════════════════════════════════════════════════════

    /**
     * Identifica y resalta el nodo súper propagador.
     */
    private void identifySuperSpreader() {
        if (model.isEmpty()) {
            showAlert("Sin datos",
                "No hay nodos en la red. Crea nodos o genera una topología primero.",
                Alert.AlertType.INFORMATION);
            return;
        }

        NodeData superSpreader = model.findSuperSpreader();

        if (superSpreader != null) {
            // Seleccionar el nodo
            model.selectNode(superSpreader.getId());

            // Forzar re-render
            view.getCanvasView().forceRender();

            showAlert("👑 Súper Propagador Identificado",
                String.format(
                    "Nodo: %s (%s)\n" +
                    "Conexiones: %d\n" +
                    "Grado de centralidad: Máximo\n\n" +
                    "Este nodo es el más peligroso de la red.\n" +
                    "Si se infecta, puede propagar el virus al\n" +
                    "mayor número de vecinos directos.",
                    superSpreader.getId(),
                    superSpreader.getLabel(),
                    superSpreader.getDegree()
                ),
                Alert.AlertType.INFORMATION
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONTROLES DE REPRODUCCIÓN
    // ═══════════════════════════════════════════════════════════════

    private void wirePlaybackControls() {
        var controlPanel = view.getControlPanel();

        // ── Botón SIMULAR ─────────────────────────────────────────
        controlPanel.getBtnStartSim().setOnAction(e -> {
            startSimulation();
        });

        // ── Play / Pausa ──────────────────────────────────────────
        controlPanel.getBtnPlayPause().setOnAction(e -> {
            togglePlayPause();
        });

        // ── Paso Adelante ─────────────────────────────────────────
        controlPanel.getBtnStepForward().setOnAction(e -> {
            stepForward();
        });

        // ── Paso Atrás ────────────────────────────────────────────
        controlPanel.getBtnStepBack().setOnAction(e -> {
            stepBackward();
        });

        // ── Ir al Inicio ──────────────────────────────────────────
        controlPanel.getBtnGoToStart().setOnAction(e -> {
            goToStart();
        });

        // ── Ir al Final ───────────────────────────────────────────
        controlPanel.getBtnGoToEnd().setOnAction(e -> {
            goToEnd();
        });

        // ── Exportación ───────────────────────────────────────────
        controlPanel.getBtnExportCSV().setOnAction(e -> {
            exportController.exportCSV();
        });

        controlPanel.getBtnExportTXT().setOnAction(e -> {
            exportController.exportTXT();
        });
    }

    /**
     * Cablea el slider de timeline para navegar entre iteraciones.
     */
    private void wireTimelineSlider() {
        var slider = view.getControlPanel().getSliderTimeline();

        slider.valueProperty().addListener((obs, old, newVal) -> {
            SimulationState simState = model.getSimulationState();

            // Solo actuar si es un cambio del usuario (no programático)
            if (slider.isValueChanging()) {
                int targetIteration = newVal.intValue();
                int currentIteration = simState.getCurrentIterationIndex();

                if (targetIteration != currentIteration) {
                    // Pausar si está corriendo
                    if (simState.isRunning()) {
                        pausePlayback();
                    }

                    // Navegar a la iteración
                    IterationSnapshot snapshot = model.goToIterationAndApply(targetIteration);
                    if (snapshot != null) {
                        updateViewFromSnapshot(snapshot);
                    }
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  SIMULACIÓN — Inicio y Control
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inicia la pre-computación de la simulación BFS.
     */
    private void startSimulation() {
        SimulationState simState = model.getSimulationState();

        // Validar que hay pacientes cero
        if (simState.getPatientZeroIds().isEmpty()) {
            showAlert("Sin Paciente Cero",
                "Selecciona al menos un nodo como Paciente Cero antes de simular.\n\n" +
                "Usa el botón '☣ Paciente Cero' y haz clic en los nodos de origen.",
                Alert.AlertType.WARNING);
            return;
        }

        // Validar que hay nodos
        if (model.isEmpty()) {
            showAlert("Red Vacía",
                "No hay nodos en la red. Crea nodos o genera una topología primero.",
                Alert.AlertType.WARNING);
            return;
        }

        System.out.println("[MainController] Iniciando simulación BFS...");

        // Si ya hay una simulación previa, resetear
        if (simState.isPrecomputed()) {
            model.resetSimulation();
        }

        // Desactivar modos de interacción
        deactivateCurrentMode();

        // Pre-computar
        boolean success = model.precomputeSimulation();

        if (success) {
            view.updateStatus("● READY", MainView.ACCENT_GREEN);

            // Reconstruir la gráfica del dashboard con todos los datos
            view.getDashboard().rebuildChart(simState.getStatsTimeline());

            // Aplicar el primer snapshot
            IterationSnapshot first = simState.getCurrentSnapshot();
            if (first != null) {
                updateViewFromSnapshot(first);
            }

            // Iniciar reproducción automática
            startPlayback();

            System.out.println("[MainController] ✓ Simulación lista. Reproduciendo...");
        } else {
            showAlert("Error de Simulación",
                "No se pudo ejecutar la simulación. Verifica la configuración.",
                Alert.AlertType.ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  LOOP DE REPRODUCCIÓN AUTOMÁTICA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Configura el Timeline de JavaFX para la reproducción automática.
     * Se actualiza dinámicamente según la velocidad del slider.
     */
    private void setupPlaybackLoop() {
        playbackTimeline = new Timeline();
        playbackTimeline.setCycleCount(Animation.INDEFINITE);

        // KeyFrame inicial (se reconfigura en cada play)
        updatePlaybackSpeed();

        // Escuchar cambios de velocidad en tiempo real
        model.getSimulationState().playbackSpeedProperty().addListener((obs, old, newVal) -> {
            if (model.getSimulationState().isRunning()) {
                // Reconfigurar velocidad en caliente
                updatePlaybackSpeed();
            }
        });
    }

    /**
     * Actualiza la velocidad del playback timeline.
     */
    private void updatePlaybackSpeed() {
        double speedMs = model.getSimulationState().getPlaybackSpeed();

        playbackTimeline.stop();
        playbackTimeline.getKeyFrames().clear();
        playbackTimeline.getKeyFrames().add(
            new KeyFrame(Duration.millis(speedMs), e -> {
                onPlaybackTick();
            })
        );

        // Reiniciar si estaba corriendo
        if (model.getSimulationState().isRunning()) {
            playbackTimeline.play();
        }
    }

    /**
     * Callback de cada tick del playback.
     * Avanza una iteración y actualiza la vista.
     */
    private void onPlaybackTick() {
        SimulationState simState = model.getSimulationState();

        if (!simState.isRunning() || shuttingDown) {
            pausePlayback();
            return;
        }

        IterationSnapshot snapshot = model.stepForwardAndApply();

        if (snapshot != null) {
            updateViewFromSnapshot(snapshot);
        } else {
            // No hay más iteraciones → simulación terminada
            pausePlayback();
            simState.finish();
            view.updateStatus("● FINISHED", MainView.ACCENT_CYAN);
            System.out.println("[MainController] Reproducción completada.");
        }
    }

    /**
     * Inicia la reproducción automática.
     */
    private void startPlayback() {
        SimulationState simState = model.getSimulationState();
        simState.play();
        updatePlaybackSpeed();
        playbackTimeline.play();
        view.getControlPanel().updatePlayPauseIcon(true);
        view.updateStatus("● RUNNING", MainView.ACCENT_GREEN);
    }

    /**
     * Pausa la reproducción automática.
     */
    private void pausePlayback() {
        model.getSimulationState().pause();
        playbackTimeline.stop();
        view.getControlPanel().updatePlayPauseIcon(false);
        view.updateStatus("● PAUSED", MainView.ACCENT_YELLOW);
    }

    /**
     * Alterna entre play y pausa.
     */
    private void togglePlayPause() {
        SimulationState simState = model.getSimulationState();

        if (simState.isRunning()) {
            pausePlayback();
        } else if (simState.isPaused() || simState.isFinished()) {
            if (simState.isAtEnd()) {
                // Si está al final, volver al inicio
                goToStart();
            }
            startPlayback();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  NAVEGACIÓN PASO A PASO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Avanza un paso en la simulación.
     */
    private void stepForward() {
        SimulationState simState = model.getSimulationState();

        // Pausar si está corriendo
        if (simState.isRunning()) {
            pausePlayback();
        }

        IterationSnapshot snapshot = model.stepForwardAndApply();
        if (snapshot != null) {
            updateViewFromSnapshot(snapshot);
        }
    }

    /**
     * Retrocede un paso en la simulación.
     */
    private void stepBackward() {
        SimulationState simState = model.getSimulationState();

        // Pausar si está corriendo
        if (simState.isRunning()) {
            pausePlayback();
        }

        IterationSnapshot snapshot = model.stepBackwardAndApply();
        if (snapshot != null) {
            updateViewFromSnapshot(snapshot);
        }
    }

    /**
     * Va al inicio de la simulación.
     */
    private void goToStart() {
        SimulationState simState = model.getSimulationState();

        if (simState.isRunning()) {
            pausePlayback();
        }

        IterationSnapshot snapshot = simState.goToStart();
        if (snapshot != null) {
            model.applySnapshot(snapshot);
            updateViewFromSnapshot(snapshot);
        }
    }

    /**
     * Va al final de la simulación.
     */
    private void goToEnd() {
        SimulationState simState = model.getSimulationState();

        if (simState.isRunning()) {
            pausePlayback();
        }

        IterationSnapshot snapshot = simState.goToEnd();
        if (snapshot != null) {
            model.applySnapshot(snapshot);
            updateViewFromSnapshot(snapshot);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTUALIZACIÓN DE LA VISTA DESDE UN SNAPSHOT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualiza todos los componentes visuales desde un snapshot.
     * Punto central de sincronización Vista ↔ Modelo.
     *
     * @param snapshot el snapshot de la iteración actual
     */
    private void updateViewFromSnapshot(IterationSnapshot snapshot) {
        if (snapshot == null) return;

        // Actualizar estadísticas del dashboard
        view.getDashboard().updateStats(snapshot.stats());

        // Actualizar cola BFS
        view.getDashboard().updateBFSQueue(snapshot.bfsQueueState());

        // Actualizar info del nodo seleccionado (si hay uno)
        NodeData selected = model.getSelectedNode();
        if (selected != null) {
            view.getDashboard().updateNodeInfo(selected);
        }

        // Forzar re-render del canvas
        view.getCanvasView().forceRender();
    }

    // ═══════════════════════════════════════════════════════════════
    //  RESET Y LIMPIEZA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Resetea la simulación manteniendo el grafo.
     */
    private void resetSimulation() {
        // Detener reproducción
        if (playbackTimeline != null) {
            playbackTimeline.stop();
        }

        model.resetSimulation();
        view.getDashboard().clearAll();
        view.getControlPanel().updatePlayPauseIcon(false);
        view.updateStatus("● IDLE", MainView.TEXT_SECONDARY);
        deactivateCurrentMode();
        view.getCanvasView().forceRender();

        System.out.println("[MainController] ✓ Simulación reseteada.");
    }

    /**
     * Confirma con el usuario y limpia todo el grafo.
     */
    private void confirmAndClearGraph() {
        if (model.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Limpieza");
        confirm.setHeaderText("¿Eliminar todos los nodos y aristas?");
        confirm.setContentText("Esta acción no se puede deshacer.");
        styleAlert(confirm);

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Detener todo
            if (playbackTimeline != null) {
                playbackTimeline.stop();
            }

            model.clearGraph();
            view.getDashboard().clearAll();
            view.getControlPanel().updatePlayPauseIcon(false);
            view.updateStatus("● IDLE", MainView.TEXT_SECONDARY);
            deactivateCurrentMode();
            view.getCanvasView().forceRender();

            System.out.println("[MainController] ✓ Grafo limpiado completamente.");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ALERTAS Y DIÁLOGOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Muestra una alerta estilizada.
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        styleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Aplica estilo dark a las alertas de JavaFX.
     */
    private void styleAlert(Alert alert) {
        var dialogPane = alert.getDialogPane();
        dialogPane.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-font-family: 'Consolas';",
            MainView.BG_SECONDARY
        ));
        dialogPane.lookup(".content").setStyle(String.format(
            "-fx-text-fill: %s; " +
            "-fx-font-family: 'Consolas'; " +
            "-fx-font-size: 12px;",
            MainView.TEXT_PRIMARY
        ));

        // Intentar estilizar los botones
        try {
            dialogPane.getButtonTypes().forEach(bt -> {
                var button = (javafx.scene.control.Button) dialogPane.lookupButton(bt);
                button.setStyle(String.format(
                    "-fx-background-color: %s; " +
                    "-fx-text-fill: %s; " +
                    "-fx-font-family: 'Consolas'; " +
                    "-fx-background-radius: 6; " +
                    "-fx-border-color: %s; " +
                    "-fx-border-radius: 6; " +
                    "-fx-cursor: hand;",
                    MainView.BG_TERTIARY, MainView.TEXT_PRIMARY, MainView.BORDER_COLOR
                ));
            });
        } catch (Exception e) {
            // Silenciar si el styling de botones falla
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SHUTDOWN — Limpieza al cerrar la aplicación
    // ═══════════════════════════════════════════════════════════════

    /**
     * Limpia todos los recursos antes de cerrar.
     * Llamado desde Main.stop() y Main.setOnCloseRequest().
     */
    public void shutdown() {
        shuttingDown = true;
        System.out.println("[MainController] Shutdown iniciado...");

        // Detener reproducción
        if (playbackTimeline != null) {
            playbackTimeline.stop();
            playbackTimeline = null;
        }

        // Detener render loop del canvas
        if (view.getCanvasView() != null) {
            view.getCanvasView().stopRendering();
        }

        System.out.println("[MainController] ✓ Shutdown completado.");
    }
}