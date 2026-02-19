package controller;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import model.GraphModel;
import model.NodeData;
import model.SimulationState;
import model.SimulationState.IterationSnapshot;
import model.SimulationState.IterationStats;
import model.SimulationState.PropagationEvent;
import model.enums.NodeStatus;
import view.MainView;
import view.StatsDashboardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SimulationController {

    // ═══════════════════════════════════════════���═══════════════════
    //  REFERENCIAS
    // ═══════════════════════════════════════════════════════════════

    private final GraphModel model;
    private final MainView   view;

    // ═══════════════════════════════════════════════════════════════
    //  HISTORIAL DE SIMULACIONES (para comparación)
    // ═══════════════════════════════════════════════════════════════

    /** Resumen de la última simulación ejecutada */
    private SimulationSummary lastSummary;

    // ═══════════════════════════════════════════════════════════════
    //  RECORD — Resumen de simulación
    // ═══════════════════════════════════════════════════════════════

    /**
     * Resumen inmutable de una simulación completada.
     * Se usa para comparaciones y análisis post-simulación.
     */
    public record SimulationSummary(
        int     totalIterations,
        int     totalNodesInfected,
        int     totalNodesRecovered,
        int     totalNodesQuarantined,
        int     totalNodes,
        double  contagionRate,
        int     recoveryTime,
        int     peakInfectionIteration,
        int     peakInfectionCount,
        String  topSpreaderNodeId,
        int     topSpreaderCount,
        double  finalInfectionPercentage,
        List<String> patientZeroIds,
        List<String> quarantinedNodeIds
    ) {
        /**
         * Genera un resumen legible en texto.
         */
        public String toDisplayString() {
            StringBuilder sb = new StringBuilder();
            sb.append("══════ RESUMEN DE SIMULACIÓN ══════\n\n");

            sb.append(String.format("  Total de iteraciones:      %d\n", totalIterations));
            sb.append(String.format("  Nodos totales:             %d\n", totalNodes));
            sb.append(String.format("  Tasa de contagio:          %.0f%%\n", contagionRate * 100));
            sb.append(String.format("  Tiempo de recuperación:    %d iter\n", recoveryTime));
            sb.append(String.format("  Pacientes cero:            %s\n", patientZeroIds));
            sb.append(String.format("  Nodos en cuarentena:       %d\n", quarantinedNodeIds.size()));
            sb.append("\n");

            sb.append("──── Resultados ────\n");
            sb.append(String.format("  Nodos infectados (total):  %d (%.1f%%)\n",
                totalNodesInfected, finalInfectionPercentage));
            sb.append(String.format("  Nodos recuperados:         %d\n", totalNodesRecovered));
            sb.append(String.format("  Pico de infección:         Iter %d (%d infectados)\n",
                peakInfectionIteration, peakInfectionCount));

            if (topSpreaderNodeId != null) {
                sb.append(String.format("  Mayor propagador:          %s (%d infecciones)\n",
                    topSpreaderNodeId, topSpreaderCount));
            }

            sb.append("\n══════════════════════════════════\n");
            return sb.toString();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public SimulationController(GraphModel model, MainView view) {
        this.model       = model;
        this.view        = view;
        this.lastSummary = null;
    }

    // ════════════════════════════════════════════════���══════════════
    //  VALIDACIONES PRE-SIMULACIÓN
    // ═══════════════════════════════════════════════════��═══════════

    /**
     * Ejecuta todas las validaciones antes de iniciar la simulación.
     * Retorna una lista de problemas encontrados (vacía si todo está OK).
     *
     * @return lista de mensajes de error/advertencia
     */
    public List<String> validatePreSimulation() {
        List<String> issues = new ArrayList<>();
        SimulationState simState = model.getSimulationState();

        // ── Verificar que hay nodos ───────────────────────────────
        if (model.isEmpty()) {
            issues.add("La red está vacía. Crea nodos o genera una topología.");
            return issues; // No tiene sentido seguir validando
        }

        // ── Verificar pacientes cero ──────────────────────────────
        if (simState.getPatientZeroIds().isEmpty()) {
            issues.add("No hay pacientes cero seleccionados. "
                + "Usa '☣ Paciente Cero' para seleccionar al menos uno.");
        }

        // ── Verificar que los pacientes cero existen ──────────────
        for (String pzId : simState.getPatientZeroIds()) {
            if (model.getNode(pzId) == null) {
                issues.add("Paciente cero '" + pzId + "' no existe en la red.");
            }
        }

        // ── Verificar nodos aislados ──────────────────────────────
        long isolatedCount = model.getAllNodes().stream()
            .filter(n -> n.getDegree() == 0)
            .count();

        if (isolatedCount > 0) {
            issues.add(isolatedCount + " nodo(s) aislado(s) sin conexiones. "
                + "No serán alcanzados por el BFS.");
        }

        // ── Verificar que no todos estén en cuarentena ────────────
        long quarantineCount = simState.getQuarantinedNodeIds().size();
        long totalNodes = model.getNodeCount();

        if (quarantineCount >= totalNodes - simState.getPatientZeroIds().size()) {
            issues.add("Demasiados nodos en cuarentena. "
                + "El virus no podrá propagarse a ningún nodo.");
        }

        // ── Verificar tasa de contagio ──────────────────────────��─
        if (simState.getContagionRate() <= 0) {
            issues.add("La tasa de contagio es 0%. "
                + "El virus no se propagará a ningún nodo.");
        }

        // ── Advertencia de contagio bajo ──────────────────────────
        if (simState.getContagionRate() > 0 && simState.getContagionRate() < 0.1) {
            issues.add("⚠ La tasa de contagio es muy baja (" +
                (int)(simState.getContagionRate() * 100) + "%). "
                + "La simulación podría ser muy lenta.");
        }

        return issues;
    }

    /**
     * Muestra los resultados de la validación al usuario.
     *
     * @return true si se puede proceder con la simulación
     */
    public boolean showValidationResults() {
        List<String> issues = validatePreSimulation();

        if (issues.isEmpty()) {
            return true; // Todo OK
        }

        // Separar errores de advertencias
        List<String> errors = issues.stream()
            .filter(i -> !i.startsWith("⚠"))
            .collect(Collectors.toList());

        List<String> warnings = issues.stream()
            .filter(i -> i.startsWith("⚠"))
            .collect(Collectors.toList());

        // Si solo hay advertencias, preguntar si continuar
        if (errors.isEmpty() && !warnings.isEmpty()) {
            return showWarningDialog(warnings);
        }

        // Si hay errores, no permitir continuar
        showErrorDialog(errors, warnings);
        return false;
    }

    /**
     * Muestra un diálogo de advertencia con opción de continuar.
     */
    private boolean showWarningDialog(List<String> warnings) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Advertencias de Simulación");
        alert.setHeaderText("Se encontraron advertencias:");

        StringBuilder content = new StringBuilder();
        for (String warning : warnings) {
            content.append("• ").append(warning).append("\n\n");
        }
        content.append("¿Deseas continuar de todas formas?");
        alert.setContentText(content.toString());

        ButtonType btnContinue = new ButtonType("Continuar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel   = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnContinue, btnCancel);

        styleAlert(alert);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == btnContinue;
    }

    /**
     * Muestra un diálogo de errores que impiden la simulación.
     */
    private void showErrorDialog(List<String> errors, List<String> warnings) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("No se puede simular");
        alert.setHeaderText("Se encontraron problemas:");

        StringBuilder content = new StringBuilder();

        if (!errors.isEmpty()) {
            content.append("❌ Errores:\n");
            for (String error : errors) {
                content.append("  • ").append(error).append("\n");
            }
        }

        if (!warnings.isEmpty()) {
            content.append("\n⚠ Advertencias:\n");
            for (String warning : warnings) {
                content.append("  • ").append(warning.replace("⚠ ", "")).append("\n");
            }
        }

        alert.setContentText(content.toString());
        styleAlert(alert);
        alert.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    //  ANÁLISIS POST-SIMULACIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Genera un resumen completo de la simulación que acaba de terminar.
     *
     * @return SimulationSummary con todos los datos analizados
     */
    public SimulationSummary generateSummary() {
        SimulationState simState = model.getSimulationState();
        List<IterationSnapshot> history = simState.getFullHistory();

        if (history.isEmpty()) return null;

        // ── Pico de infección ─────────────────────────────────────
        int peakIteration = 0;
        int peakCount = 0;

        for (int i = 0; i < history.size(); i++) {
            int infected = history.get(i).stats().infectedCount();
            if (infected > peakCount) {
                peakCount = infected;
                peakIteration = i;
            }
        }

        // ── Mayor propagador ──────────────────────────────────────
        String topSpreaderId = null;
        int topSpreaderCount = 0;

        for (NodeData node : model.getAllNodes()) {
            int spreadCount = node.getSpreadCount();
            if (spreadCount > topSpreaderCount) {
                topSpreaderCount = spreadCount;
                topSpreaderId = node.getId();
            }
        }

        // ── Conteos finales ───────────────────────────────────────
        IterationSnapshot lastSnapshot = history.get(history.size() - 1);
        IterationStats finalStats = lastSnapshot.stats();

        int totalInfected = 0;
        int totalRecovered = 0;
        for (NodeData node : model.getAllNodes()) {
            if (node.wasEverInfected()) totalInfected++;
            if (node.getStatus() == NodeStatus.RECOVERED) totalRecovered++;
        }

        // ── Calcular porcentaje de infección final ────────────────
        double finalPct = model.getNodeCount() > 0
            ? ((double) totalInfected / model.getNodeCount()) * 100.0
            : 0.0;

        // ── Construir resumen ─────────────────────────────────────
        SimulationSummary summary = new SimulationSummary(
            simState.getTotalIterations(),
            totalInfected,
            totalRecovered,
            (int) simState.getQuarantinedNodeIds().size(),
            model.getNodeCount(),
            simState.getContagionRate(),
            simState.getRecoveryTime(),
            peakIteration,
            peakCount,
            topSpreaderId,
            topSpreaderCount,
            finalPct,
            new ArrayList<>(simState.getPatientZeroIds()),
            new ArrayList<>(simState.getQuarantinedNodeIds())
        );

        this.lastSummary = summary;
        return summary;
    }

    /**
     * Muestra el resumen de la simulación al usuario en un diálogo.
     */
    public void showSummaryDialog() {
        SimulationSummary summary = generateSummary();
        if (summary == null) {
            showInfoAlert("Sin datos",
                "No hay simulación completada para analizar.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("📊 Resumen de Simulación");
        alert.setHeaderText(null);
        alert.setContentText(summary.toDisplayString());
        styleAlert(alert);

        // Hacer el diálogo más ancho para el contenido
        alert.getDialogPane().setMinWidth(450);
        alert.getDialogPane().setMinHeight(400);

        alert.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    //  ANÁLISIS DETALLADO — Métricas específicas
    // ═══════════════════════════════════════════════════════════════

    /**
     * Calcula la velocidad de propagación promedio.
     * (Nodos infectados por iteración en promedio)
     *
     * @return nodos/iteración
     */
    public double getAveragePropagationSpeed() {
        SimulationState simState = model.getSimulationState();
        List<IterationSnapshot> history = simState.getFullHistory();

        if (history.size() <= 1) return 0;

        int totalNewInfections = 0;
        for (int i = 1; i < history.size(); i++) {
            int prevInfected = history.get(i - 1).stats().infectedCount();
            int currInfected = history.get(i).stats().infectedCount();

            // Nuevas infecciones = infectados actuales - anteriores + recuperados
            int newInfections = (int) history.get(i).events().stream()
                .filter(e -> !e.wasBlocked())
                .count();
            totalNewInfections += newInfections;
        }

        return (double) totalNewInfections / (history.size() - 1);
    }

    /**
     * Calcula la efectividad de las cuarentenas.
     * Compara el alcance real vs. el alcance teórico sin cuarentenas.
     *
     * @return porcentaje de infecciones prevenidas (estimado)
     */
    public double getQuarantineEffectiveness() {
        SimulationState simState = model.getSimulationState();

        long quarantinedCount = simState.getQuarantinedNodeIds().size();
        if (quarantinedCount == 0) return 0.0;

        // Estimar cuántos nodos se salvaron por la cuarentena
        // Cada nodo en cuarentena "protege" a sus vecinos indirectamente
        int protectedEstimate = 0;
        for (String qId : simState.getQuarantinedNodeIds()) {
            NodeData qNode = model.getNode(qId);
            if (qNode != null) {
                protectedEstimate += qNode.getDegree();
            }
        }

        int totalNodes = model.getNodeCount();
        if (totalNodes == 0) return 0.0;

        return Math.min(100.0, ((double) protectedEstimate / totalNodes) * 100.0);
    }

    /**
     * Encuentra la iteración donde ocurrió el evento más significativo.
     * (Mayor número de nuevas infecciones en una sola iteración)
     *
     * @return número de la iteración, o -1 si no hay datos
     */
    public int findMostActiveIteration() {
        SimulationState simState = model.getSimulationState();
        List<IterationSnapshot> history = simState.getFullHistory();

        int maxEvents = 0;
        int maxIteration = -1;

        for (int i = 0; i < history.size(); i++) {
            long successfulInfections = history.get(i).events().stream()
                .filter(e -> !e.wasBlocked())
                .count();

            if (successfulInfections > maxEvents) {
                maxEvents = (int) successfulInfections;
                maxIteration = i;
            }
        }

        return maxIteration;
    }

    /**
     * Genera un reporte de propagación: quién infectó a quién.
     * Útil para construir el "árbol de transmisión".
     *
     * @return lista de strings con la cadena de propagación
     */
    public List<String> getTransmissionChain() {
        List<String> chain = new ArrayList<>();
        SimulationState simState = model.getSimulationState();

        for (IterationSnapshot snapshot : simState.getFullHistory()) {
            for (PropagationEvent event : snapshot.events()) {
                if (!event.wasBlocked()) {
                    String source = event.sourceId() != null
                        ? event.sourceId()
                        : "[ORIGEN]";
                    chain.add(String.format("Iter %d: %s → %s (nivel %d)",
                        event.iteration(), source, event.targetId(), event.depth()));
                }
            }
        }

        return chain;
    }

    /**
     * Obtiene las estadísticas de propagación por nivel de profundidad BFS.
     * Útil para el mapa de calor.
     *
     * @return mapa: nivel de profundidad → cantidad de nodos en ese nivel
     */
    public Map<Integer, Long> getInfectionsByDepth() {
        return model.getAllNodes().stream()
            .filter(NodeData::wasEverInfected)
            .collect(Collectors.groupingBy(
                NodeData::getDepthLevel,
                Collectors.counting()
            ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  COORDINACIÓN CON EL DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sincroniza completamente el dashboard con el estado actual
     * de la simulación. Útil después de saltar a una iteración
     * específica o al cargar una simulación guardada.
     */
    public void syncDashboard() {
        SimulationState simState = model.getSimulationState();
        StatsDashboardView dashboard = view.getDashboard();

        // Actualizar estadísticas actuales
        IterationSnapshot current = simState.getCurrentSnapshot();
        if (current != null) {
            dashboard.updateStats(current.stats());
            dashboard.updateBFSQueue(current.bfsQueueState());
        }

        // Reconstruir gráfica completa
        dashboard.rebuildChart(simState.getStatsTimeline());

        // Actualizar info del nodo seleccionado
        NodeData selected = model.getSelectedNode();
        dashboard.updateNodeInfo(selected);
    }

    /**
     * Actualiza el dashboard para una iteración específica.
     * Más eficiente que syncDashboard() para avances paso a paso.
     *
     * @param snapshot el snapshot de la iteración a mostrar
     */
    public void updateDashboardForIteration(IterationSnapshot snapshot) {
        if (snapshot == null) return;

        StatsDashboardView dashboard = view.getDashboard();

        // Actualizar stats
        dashboard.updateStats(snapshot.stats());

        // Actualizar cola BFS
        dashboard.updateBFSQueue(snapshot.bfsQueueState());

        // Agregar punto a la gráfica (solo si es un avance hacia adelante)
        SimulationState simState = model.getSimulationState();
        if (snapshot.iterationNumber() == simState.getCurrentIterationIndex()) {
            dashboard.addChartDataPoint(snapshot.stats());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  GESTIÓN DE RE-SIMULACIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Prepara el sistema para re-ejecutar la simulación con los
     * mismos parámetros pero diferente aleatoriedad.
     *
     * @return true si se reseteó correctamente
     */
    public boolean prepareResimulation() {
        SimulationState simState = model.getSimulationState();

        if (simState.getPatientZeroIds().isEmpty()) {
            showInfoAlert("Sin configuración",
                "No hay pacientes cero seleccionados para re-simular.");
            return false;
        }

        // Reset suave (mantiene P0 y cuarentena)
        model.resetSimulation();
        view.getDashboard().clearAll();

        System.out.println("[SimController] ✓ Preparado para re-simulación.");
        return true;
    }

    /**
     * Ofrece al usuario re-simular con parámetros diferentes.
     * Muestra un diálogo de confirmación.
     *
     * @return true si el usuario confirmó
     */
    public boolean confirmResimulation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Re-Simular");
        alert.setHeaderText("¿Ejecutar una nueva simulación?");

        SimulationState simState = model.getSimulationState();
        alert.setContentText(String.format(
            "Parámetros actuales:\n" +
            "  • Tasa de contagio: %.0f%%\n" +
            "  • Recuperación: %d iteraciones\n" +
            "  • Pacientes cero: %s\n" +
            "  • Nodos en cuarentena: %d\n\n" +
            "La simulación anterior se perderá.",
            simState.getContagionRate() * 100,
            simState.getRecoveryTime(),
            simState.getPatientZeroIds(),
            simState.getQuarantinedNodeIds().size()
        ));

        styleAlert(alert);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // ════════════════���══════════════════════════════════════════════
    //  LOG DE EVENTOS — Para debugging y exportación
    // ═══════════════════════════════════════════════════════════════

    /**
     * Genera un log detallado de todos los eventos de propagación.
     * Formato legible para debugging y para el reporte TXT.
     *
     * @return String con el log completo
     */
    public String generateEventLog() {
        StringBuilder log = new StringBuilder();
        SimulationState simState = model.getSimulationState();

        log.append("════════ LOG DE EVENTOS BFS ═════��══\n\n");

        for (IterationSnapshot snapshot : simState.getFullHistory()) {
            log.append(String.format("── Iteración %d ──\n",
                snapshot.iterationNumber()));

            if (snapshot.events().isEmpty()) {
                log.append("   (sin eventos)\n");
            } else {
                for (PropagationEvent event : snapshot.events()) {
                    if (event.wasBlocked()) {
                        log.append(String.format("   ✗ %s → %s [BLOQUEADO]\n",
                            event.sourceId() != null ? event.sourceId() : "ORIGEN",
                            event.targetId()));
                    } else {
                        log.append(String.format("   ✓ %s → %s (nivel %d)\n",
                            event.sourceId() != null ? event.sourceId() : "ORIGEN",
                            event.targetId(),
                            event.depth()));
                    }
                }
            }

            IterationStats stats = snapshot.stats();
            log.append(String.format("   [S:%d I:%d R:%d Q:%d] Cola: %s\n\n",
                stats.susceptibleCount(), stats.infectedCount(),
                stats.recoveredCount(), stats.quarantinedCount(),
                snapshot.bfsQueueState()));
        }

        log.append("════════ FIN DEL LOG ════════\n");
        return log.toString();
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    /**
     * @return el último resumen generado, o null
     */
    public SimulationSummary getLastSummary() {
        return lastSummary;
    }

    /**
     * Muestra una alerta informativa simple.
     */
    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        styleAlert(alert);
        alert.showAndWait();
    }

    /**
     * Aplica estilo dark a las alertas.
     */
    private void styleAlert(Alert alert) {
        var dialogPane = alert.getDialogPane();
        dialogPane.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-font-family: 'Consolas';",
            MainView.BG_SECONDARY
        ));

        try {
            dialogPane.lookup(".content").setStyle(String.format(
                "-fx-text-fill: %s; " +
                "-fx-font-family: 'Consolas'; " +
                "-fx-font-size: 12px;",
                MainView.TEXT_PRIMARY
            ));

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
            // Silenciar errores de styling
        }
    }
}