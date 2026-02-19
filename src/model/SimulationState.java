package model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.enums.NodeStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimulationState{
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    //-----Enums internos
    public enum PlaybackState{
        IDLE,
        RUNNING,
        PAUSED,
        FINISHED,
    }

    private final List<IterationSnapshot> history;

    private final IntegerProperty currentIterationIndex;

    private final IntegerProperty totalIterations;

   
//---ESTADO DE REPRODUCCION

    
    /** Estado actual del reproductor */
    private final ObjectProperty<PlaybackState> playbackState;

    /** Velocidad de reproducción (ms entre iteraciones) */
    private final DoubleProperty playbackSpeed;

    /** Velocidad mínima (más lento) en ms */
    public static final double SPEED_MIN = 100.0;

    /** Velocidad máxima (más rápido) en ms */
    public static final double SPEED_MAX = 3000.0;

    /** Velocidad por defecto en ms */
    public static final double SPEED_DEFAULT = 800.0;

//----PARAMETROS DE SIMULACION

 /** Tasa de contagio (0.0 a 1.0) — probabilidad de infectar a un vecino */
    private final DoubleProperty contagionRate;

    /** Duración de la infección en iteraciones antes de recuperarse */
    private final IntegerProperty recoveryTime;

    /** IDs de los nodos seleccionados como pacientes cero */
    private final ObservableList<String> patientZeroIds;

    /** IDs de los nodos marcados como en cuarentena */
    private final ObservableList<String> quarantinedNodeIds;

    /** IDs de las aristas desconectadas (cortafuegos) */
    private final ObservableList<String> disabledEdgeIds;

    //----ESTADISTICAS EN TIEMPO REAL
    private final IntegerProperty statSusceptible;
    private final IntegerProperty statInfected;
    private final IntegerProperty statRecovered;
    private final IntegerProperty statQuarantined;
    private final IntegerProperty statTotalNodes;
    private final DoubleProperty  statInfectionPercentage;

    private final BooleanProperty precomputed;

    //METADATOS

    private String simulationStartTime;
    private String simulationEndTime;


    //-----CONSTRUCTOR
     public SimulationState() {
        // Historial
        this.history                = new ArrayList<>();
        this.currentIterationIndex  = new SimpleIntegerProperty(-1);
        this.totalIterations        = new SimpleIntegerProperty(0);

        // Reproducción
        this.playbackState = new SimpleObjectProperty<>(PlaybackState.IDLE);
        this.playbackSpeed = new SimpleDoubleProperty(SPEED_DEFAULT);

        // Parámetros
        this.contagionRate      = new SimpleDoubleProperty(1.0); // 100% por defecto
        this.recoveryTime       = new SimpleIntegerProperty(5);  // 5 iteraciones
        this.patientZeroIds     = FXCollections.observableArrayList();
        this.quarantinedNodeIds = FXCollections.observableArrayList();
        this.disabledEdgeIds    = FXCollections.observableArrayList();

        // Estadísticas
        this.statSusceptible        = new SimpleIntegerProperty(0);
        this.statInfected           = new SimpleIntegerProperty(0);
        this.statRecovered          = new SimpleIntegerProperty(0);
        this.statQuarantined        = new SimpleIntegerProperty(0);
        this.statTotalNodes         = new SimpleIntegerProperty(0);
        this.statInfectionPercentage = new SimpleDoubleProperty(0.0);

        // Control
        this.precomputed = new SimpleBooleanProperty(false);

        // Timestamps
        this.simulationStartTime = null;
        this.simulationEndTime   = null;
    }

    //--------GESTION DE HISTORIAL

    public void recordIteration(IterationSnapshot snapshot){
        history.add(snapshot);
        setTotalIterations(history.size());

        if(history.size() == 1){
            simulationStartTime = LocalDateTime.now().format(TS_FMT);
        }
    }

    public IterationSnapshot goToIteration(int index) {
        if (index < 0 || index >= history.size()) {
            return null;
        }

        setCurrentIterationIndex(index);
        IterationSnapshot snapshot = history.get(index);

        // Actualizar estadísticas del dashboard
        updateStatsFromSnapshot(snapshot);

        return snapshot;
    }

    //Avanza a la siguiente iteracion
    public IterationSnapshot stepForward() {
        int next = getCurrentIterationIndex() + 1;

        if (next >= history.size()) {
            setPlaybackState(PlaybackState.FINISHED);
            return null;
        }

        return goToIteration(next);
    }
    
    /**
     * Retrocede a la iteración anterior.
     *
     * @return el snapshot de la iteración anterior, o null si ya está al inicio
     */
    public IterationSnapshot stepBackward() {
        int prev = getCurrentIterationIndex() - 1;

        if (prev < 0) {
            return null;
        }

        // Si estaba en FINISHED, volver a PAUSED
        if (getPlaybackState() == PlaybackState.FINISHED) {
            setPlaybackState(PlaybackState.PAUSED);
        }

        return goToIteration(prev);
    }
     
      /**
     * Salta al inicio de la simulación.
     *
     * @return snapshot de la iteración 0
     */
    public IterationSnapshot goToStart() {
        if (history.isEmpty()) return null;

        if (getPlaybackState() == PlaybackState.FINISHED) {
            setPlaybackState(PlaybackState.PAUSED);
        }

        return goToIteration(0);
    }

    /**
     * Salta al final de la simulación.
     *
     * @return snapshot de la última iteración
     */
    public IterationSnapshot goToEnd() {
        if (history.isEmpty()) return null;
        return goToIteration(history.size() - 1);
    }

    /**
     * Obtiene un snapshot específico sin cambiar la iteración actual.
     *
     * @param index índice de la iteración
     * @return snapshot o null
     */
    public IterationSnapshot peekIteration(int index) {
        if (index < 0 || index >= history.size()) return null;
        return history.get(index);
    }

    /**
     * @return snapshot de la iteración actual
     */
    public IterationSnapshot getCurrentSnapshot() {
        return peekIteration(getCurrentIterationIndex());
    }

    /**
     * @return true si hay una iteración siguiente disponible
     */
    public boolean hasNext() {
        return getCurrentIterationIndex() < history.size() - 1;
    }

    /**
     * @return true si hay una iteración anterior disponible
     */
    public boolean hasPrevious() {
        return getCurrentIterationIndex() > 0;
    }

    /**
     * @return true si la simulación está al final
     */
    public boolean isAtEnd() {
        return !history.isEmpty() && getCurrentIterationIndex() >= history.size() - 1;
    }

    /**
     * @return lista inmutable del historial completo
     */
    public List<IterationSnapshot> getFullHistory() {
        return Collections.unmodifiableList(history);
    }


    //----CONTROLES DE REPRODUCCION
     /**
     * Inicia o reanuda la reproducción automática.
     */
    public void play() {
        if (getPlaybackState() == PlaybackState.FINISHED) {
            // Si terminó, reiniciar desde el inicio
            goToIteration(0);
        }
        setPlaybackState(PlaybackState.RUNNING);
    }

    /**
     * Pausa la reproducción automática.
     */
    public void pause() {
        if (getPlaybackState() == PlaybackState.RUNNING) {
            setPlaybackState(PlaybackState.PAUSED);
        }
    }

    /**
     * Alterna entre Play y Pausa.
     */
    public void togglePlayPause() {
        if (getPlaybackState() == PlaybackState.RUNNING) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Detiene completamente y marca como finalizada.
     */
    public void finish() {
        setPlaybackState(PlaybackState.FINISHED);
        simulationEndTime = LocalDateTime.now().format(TS_FMT);
    }

    /**
     * @return true si la simulación está corriendo
     */
    public boolean isRunning() {
        return getPlaybackState() == PlaybackState.RUNNING;
    }

    /**
     * @return true si la simulación está pausada
     */
    public boolean isPaused() {
        return getPlaybackState() == PlaybackState.PAUSED;
    }

    /**
     * @return true si la simulación terminó
     */
    public boolean isFinished() {
        return getPlaybackState() == PlaybackState.FINISHED;
    }

    /**
     * @return true si la simulación no ha comenzado
     */
    public boolean isIdle() {
        return getPlaybackState() == PlaybackState.IDLE;
    }

    //-----GESTION DE PACIENTE 0


    /**
     * Agrega un nodo como paciente cero.
     *
     * @param nodeId ID del nodo
     * @return true si se agregó (no era duplicado)
     */
    public boolean addPatientZero(String nodeId) {
        if (nodeId != null && !patientZeroIds.contains(nodeId)) {
            patientZeroIds.add(nodeId);
            return true;
        }
        return false;
    }

    /**
     * Elimina un nodo de la lista de pacientes cero.
     *
     * @param nodeId ID del nodo
     * @return true si se eliminó
     */
    public boolean removePatientZero(String nodeId) {
        return patientZeroIds.remove(nodeId);
    }

    /**
     * Alterna un nodo como paciente cero (add/remove).
     *
     * @param nodeId ID del nodo
     * @return true si quedó como paciente cero, false si fue removido
     */
    public boolean togglePatientZero(String nodeId) {
        if (patientZeroIds.contains(nodeId)) {
            patientZeroIds.remove(nodeId);
            return false;
        } else {
            patientZeroIds.add(nodeId);
            return true;
        }
    }

    //-----GESTION DE CUARENTENA / CORTAFUEGOS

     /**
     * Marca un nodo como en cuarentena.
     *
     * @param nodeId ID del nodo
     * @return true si se agregó
     */
    public boolean addQuarantinedNode(String nodeId) {
        if (nodeId != null && !quarantinedNodeIds.contains(nodeId)) {
            quarantinedNodeIds.add(nodeId);
            return true;
        }
        return false;
    }

    /**
     * Saca un nodo de cuarentena.
     *
     * @param nodeId ID del nodo
     * @return true si se eliminó
     */
    public boolean removeQuarantinedNode(String nodeId) {
        return quarantinedNodeIds.remove(nodeId);
    }

    /**
     * Alterna cuarentena de un nodo.
     */
    public boolean toggleQuarantine(String nodeId) {
        if (quarantinedNodeIds.contains(nodeId)) {
            quarantinedNodeIds.remove(nodeId);
            return false;
        } else {
            quarantinedNodeIds.add(nodeId);
            return true;
        }
    }

    /**
     * Desactiva una arista (cortafuegos).
     *
     * @param edgeId ID de la arista
     * @return true si se desactivó
     */
    public boolean disableEdge(String edgeId) {
        if (edgeId != null && !disabledEdgeIds.contains(edgeId)) {
            disabledEdgeIds.add(edgeId);
            return true;
        }
        return false;
    }

    /**
     * Reactiva una arista desactivada.
     */
    public boolean enableEdge(String edgeId) {
        return disabledEdgeIds.remove(edgeId);
    }

    /**
     * @return true si la arista está desactivada
     */
    public boolean isEdgeDisabled(String edgeId) {
        return disabledEdgeIds.contains(edgeId);
    }

    /**
     * @return true si el nodo está en cuarentena
     */
    public boolean isNodeQuarantined(String nodeId) {
        return quarantinedNodeIds.contains(nodeId);
    }

    //----ESTADISTICAS
     /**
     * Actualiza todas las propiedades de estadísticas a partir de un snapshot.
     * Las propiedades son observables → el dashboard se actualiza automáticamente.
     *
     * @param snapshot el snapshot del que extraer estadísticas
     */
    private void updateStatsFromSnapshot(IterationSnapshot snapshot) {
        if (snapshot == null) return;

        IterationStats stats = snapshot.stats();
        setStatSusceptible(stats.susceptibleCount());
        setStatInfected(stats.infectedCount());
        setStatRecovered(stats.recoveredCount());
        setStatQuarantined(stats.quarantinedCount());
        setStatTotalNodes(stats.totalNodes());
        setStatInfectionPercentage(stats.infectionPercentage());
    }

    /**
     * Genera la serie de datos para la gráfica del dashboard.
     * Retorna un mapa: iteración → conteos SIR.
     *
     * @return LinkedHashMap preservando el orden de iteraciones
     */
    public LinkedHashMap<Integer, IterationStats> getStatsTimeline() {
        LinkedHashMap<Integer, IterationStats> timeline = new LinkedHashMap<>();
        for (int i = 0; i < history.size(); i++) {
            timeline.put(i, history.get(i).stats());
        }
        return timeline;
    }

    //-----EXPORTACION DE DATOS


    /**
     * Genera el contenido completo para exportar como CSV.
     * Formato: Iteración, NodoID, Estado, Profundidad, InfectadoPor
     *
     * @return String con el contenido CSV completo
     */
    public String toCSV() {
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Iteracion,NodoID,Label,Estado,Profundidad,InfectadoPor,")
           .append("Susceptibles,Infectados,Recuperados,Cuarentena,PorcentajeInfeccion\n");

        for (int i = 0; i < history.size(); i++) {
            IterationSnapshot snap = history.get(i);

            for (NodeSnapshotRecord nodeSnap : snap.nodeSnapshots()) {
                csv.append(String.format("%d,%s,%s,%s,%d,%s,%d,%d,%d,%d,%.2f\n",
                    i,
                    nodeSnap.nodeId(),
                    escapeCsv(nodeSnap.label()),
                    nodeSnap.status().getDisplayName(),
                    nodeSnap.depthLevel(),
                    nodeSnap.infectedBy() != null ? nodeSnap.infectedBy() : "N/A",
                    snap.stats().susceptibleCount(),
                    snap.stats().infectedCount(),
                    snap.stats().recoveredCount(),
                    snap.stats().quarantinedCount(),
                    snap.stats().infectionPercentage()
                ));
            }
        }

        return csv.toString();
    }

    /**
     * Genera el contenido para exportar como TXT legible.
     *
     * @return String con el reporte en texto plano
     */
    public String toTXT() {
        StringBuilder txt = new StringBuilder();

        txt.append("═══════════════════════════════════════════════════════\n");
        txt.append("  REPORTE DE SIMULACIÓN BFS — PROPAGACIÓN DE VIRUS\n");
        txt.append("═══════════════════════════════════════════════════════\n\n");

        txt.append(String.format("  Inicio:           %s\n",
            simulationStartTime != null ? simulationStartTime : "N/A"));
        txt.append(String.format("  Fin:              %s\n",
            simulationEndTime != null ? simulationEndTime : "N/A"));
        txt.append(String.format("  Total iteraciones: %d\n", history.size()));
        txt.append(String.format("  Tasa de contagio:  %.0f%%\n", getContagionRate() * 100));
        txt.append(String.format("  Tiempo recuperac.: %d iteraciones\n", getRecoveryTime()));
        txt.append(String.format("  Pacientes cero:    %s\n", patientZeroIds));
        txt.append(String.format("  Nodos cuarentena:  %s\n", quarantinedNodeIds));
        txt.append("\n");

        for (int i = 0; i < history.size(); i++) {
            IterationSnapshot snap = history.get(i);
            txt.append(String.format("──── ITERACIÓN %d ────────────────────────────────\n", i));

            // Eventos
            if (!snap.events().isEmpty()) {
                txt.append("  Eventos:\n");
                for (PropagationEvent event : snap.events()) {
                    txt.append(String.format("    %s → %s  (profundidad %d)\n",
                        event.sourceId() != null ? event.sourceId() : "[ORIGEN]",
                        event.targetId(),
                        event.depth()
                    ));
                }
            }

            // Stats
            IterationStats stats = snap.stats();
            txt.append(String.format("  S: %d | I: %d | R: %d | Q: %d | Infección: %.1f%%\n\n",
                stats.susceptibleCount(), stats.infectedCount(),
                stats.recoveredCount(), stats.quarantinedCount(),
                stats.infectionPercentage()
            ));
        }

        txt.append("═══════════════════════════════════════════════════════\n");
        txt.append("  FIN DEL REPORTE\n");
        txt.append("═══════════════════════════════════════════════════════\n");

        return txt.toString();
    }

    /**
     * Escapa un valor para CSV (comillas si contiene comas).
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    //----reset completo

     public void reset() {
        history.clear();
        setCurrentIterationIndex(-1);
        setTotalIterations(0);
        setPlaybackState(PlaybackState.IDLE);
        patientZeroIds.clear();
        quarantinedNodeIds.clear();
        disabledEdgeIds.clear();
        setPrecomputed(false);

        // Reset stats
        setStatSusceptible(0);
        setStatInfected(0);
        setStatRecovered(0);
        setStatQuarantined(0);
        setStatTotalNodes(0);
        setStatInfectionPercentage(0.0);

        simulationStartTime = null;
        simulationEndTime   = null;
    }

    /**
     * Reset suave: limpia historial pero mantiene pacientes cero y cuarentena.
     * Útil para re-ejecutar con los mismos parámetros.
     */
    public void softReset() {
        history.clear();
        setCurrentIterationIndex(-1);
        setTotalIterations(0);
        setPlaybackState(PlaybackState.IDLE);
        setPrecomputed(false);

        setStatSusceptible(0);
        setStatInfected(0);
        setStatRecovered(0);
        setStatQuarantined(0);
        setStatTotalNodes(0);
        setStatInfectionPercentage(0.0);

        simulationStartTime = null;
        simulationEndTime   = null;
    }

    //----getters y setters
     // ── Iteration Index ───────────────────────────────────────────
    public int getCurrentIterationIndex()                            { return currentIterationIndex.get(); }
    public void setCurrentIterationIndex(int idx)                    { currentIterationIndex.set(idx); }
    public IntegerProperty currentIterationIndexProperty()           { return currentIterationIndex; }

    // ── Total Iterations ──────────────────────────────────────────
    public int getTotalIterations()                                  { return totalIterations.get(); }
    public void setTotalIterations(int total)                        { totalIterations.set(total); }
    public IntegerProperty totalIterationsProperty()                 { return totalIterations; }

    // ── Playback State ────────────────────────────────────────────
    public PlaybackState getPlaybackState()                          { return playbackState.get(); }
    public void setPlaybackState(PlaybackState state)                { playbackState.set(state); }
    public ObjectProperty<PlaybackState> playbackStateProperty()     { return playbackState; }

    // ── Playback Speed ────────────────────────────────────────────
    public double getPlaybackSpeed()                                 { return playbackSpeed.get(); }
    public void setPlaybackSpeed(double ms)                          { playbackSpeed.set(Math.clamp(ms, SPEED_MIN, SPEED_MAX)); }
    public DoubleProperty playbackSpeedProperty()                    { return playbackSpeed; }

    // ── Contagion Rate ────────────────────────────────────────────
    public double getContagionRate()                                 { return contagionRate.get(); }
    public void setContagionRate(double rate)                        { contagionRate.set(Math.clamp(rate, 0.0, 1.0)); }
    public DoubleProperty contagionRateProperty()                    { return contagionRate; }

    // ── Recovery Time ─────────────────────────────────────────────
    public int getRecoveryTime()                                     { return recoveryTime.get(); }
    public void setRecoveryTime(int time)                            { recoveryTime.set(Math.max(1, time)); }
    public IntegerProperty recoveryTimeProperty()                    { return recoveryTime; }

    // ── Patient Zero IDs ──────────────────────────────────────────
    public ObservableList<String> getPatientZeroIds()                { return patientZeroIds; }

    // ── Quarantined Node IDs ──────────────────────────────────────
    public ObservableList<String> getQuarantinedNodeIds()            { return quarantinedNodeIds; }

    // ── Disabled Edge IDs ─────────────────────────────────────────
    public ObservableList<String> getDisabledEdgeIds()               { return disabledEdgeIds; }

    // ── Stats ─────────────────────────────────────────────────────
    public int getStatSusceptible()                                  { return statSusceptible.get(); }
    public void setStatSusceptible(int v)                            { statSusceptible.set(v); }
    public IntegerProperty statSusceptibleProperty()                 { return statSusceptible; }

    public int getStatInfected()                                     { return statInfected.get(); }
    public void setStatInfected(int v)                               { statInfected.set(v); }
    public IntegerProperty statInfectedProperty()                    { return statInfected; }

    public int getStatRecovered()                                    { return statRecovered.get(); }
    public void setStatRecovered(int v)                              { statRecovered.set(v); }
    public IntegerProperty statRecoveredProperty()                   { return statRecovered; }

    public int getStatQuarantined()                                  { return statQuarantined.get(); }
    public void setStatQuarantined(int v)                            { statQuarantined.set(v); }
    public IntegerProperty statQuarantinedProperty()                 { return statQuarantined; }

    public int getStatTotalNodes()                                   { return statTotalNodes.get(); }
    public void setStatTotalNodes(int v)                             { statTotalNodes.set(v); }
    public IntegerProperty statTotalNodesProperty()                  { return statTotalNodes; }

    public double getStatInfectionPercentage()                       { return statInfectionPercentage.get(); }
    public void setStatInfectionPercentage(double v)                 { statInfectionPercentage.set(v); }
    public DoubleProperty statInfectionPercentageProperty()          { return statInfectionPercentage; }

    // ── Precomputed ───────────────────────────────────────────────
    public boolean isPrecomputed()                                   { return precomputed.get(); }
    public void setPrecomputed(boolean v)                            { precomputed.set(v); }
    public BooleanProperty precomputedProperty()                     { return precomputed; }

    // ── Timestamps ────────────────────────────────────────────────
    public String getSimulationStartTime()  { return simulationStartTime; }
    public String getSimulationEndTime()    { return simulationEndTime; }

    //---RECORDS INTERNOS

       public record IterationSnapshot(
        int                       iterationNumber,
        List<NodeSnapshotRecord>  nodeSnapshots,
        List<String>              bfsQueueState,
        List<PropagationEvent>    events,
        IterationStats            stats
    ) {
        /**
         * Constructor compacto con validación.
         */
        public IterationSnapshot {
            nodeSnapshots = List.copyOf(nodeSnapshots);
            bfsQueueState = List.copyOf(bfsQueueState);
            events        = List.copyOf(events);
        }
    }

    /**
     * Snapshot del estado de un nodo individual en una iteración.
     */
    public record NodeSnapshotRecord(
        String      nodeId,
        String      label,
        NodeStatus  status,
        int         depthLevel,
        String      infectedBy,
        int         infectedDuration
    ) {}

    /**
     * Evento de propagación: quién infectó a quién.
     */
    public record PropagationEvent(
        String  sourceId,
        String  targetId,
        int     depth,
        int     iteration,
        boolean wasBlocked
    ) {
        /**
         * @return representación legible del evento
         */
        @Override
        public String toString() {
            if (wasBlocked) {
                return String.format("[BLOQUEADO] %s -X→ %s (profundidad %d)",
                    sourceId != null ? sourceId : "ORIGEN", targetId, depth);
            }
            return String.format("%s → %s (profundidad %d)",
                sourceId != null ? sourceId : "ORIGEN", targetId, depth);
        }
    }

    /**
     * Estadísticas agregadas de una iteración.
     */
    public record IterationStats(
        int     susceptibleCount,
        int     infectedCount,
        int     recoveredCount,
        int     quarantinedCount,
        int     totalNodes,
        double  infectionPercentage
    ) {
        /**
         * Factory method: calcula estadísticas a partir de una lista de snapshots.
         */
        public static IterationStats fromNodeSnapshots(List<NodeSnapshotRecord> snapshots) {
            int s = 0, i = 0, r = 0, q = 0;

            for (NodeSnapshotRecord ns : snapshots) {
                switch (ns.status()) {
                    case SUSCEPTIBLE  -> s++;
                    case INFECTED     -> i++;
                    case PATIENT_ZERO -> i++; // Paciente cero cuenta como infectado
                    case RECOVERED    -> r++;
                    case QUARANTINED  -> q++;
                }
            }

            int total = snapshots.size();
            double pct = total > 0 ? ((double) i / total) * 100.0 : 0.0;

            return new IterationStats(s, i, r, q, total, pct);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "SimulationState{ state=%s, iteration=%d/%d, speed=%.0fms, " +
            "contagion=%.0f%%, recovery=%d, patients0=%s, history=%d snapshots }",
            getPlaybackState(), getCurrentIterationIndex(), getTotalIterations(),
            getPlaybackSpeed(), getContagionRate() * 100, getRecoveryTime(),
            patientZeroIds, history.size()
        );
    }


}
