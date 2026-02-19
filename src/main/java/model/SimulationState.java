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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class SimulationState {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Enum de estados ───────────────────────────────────────────
    public enum PlaybackState {
        IDLE, RUNNING, PAUSED, FINISHED
    }

    // ── Historial ─────────────────────────────────────────────────
    private final List<IterationSnapshot> history;
    private final IntegerProperty currentIterationIndex;
    private final IntegerProperty totalIterations;

    // ── Reproducción ──────────────────────────────────────────────
    private final ObjectProperty<PlaybackState> playbackState;
    private final DoubleProperty playbackSpeed;

    public static final double SPEED_MIN = 100.0;
    public static final double SPEED_MAX = 3000.0;
    public static final double SPEED_DEFAULT = 800.0;

    // ── Parámetros ────────────────────────────────────────────────
    private final DoubleProperty contagionRate;
    private final IntegerProperty recoveryTime;
    private final ObservableList<String> patientZeroIds;
    private final ObservableList<String> quarantinedNodeIds;
    private final ObservableList<String> disabledEdgeIds;

    // ── Estadísticas ──────────────────────────────────────────────
    private final IntegerProperty statSusceptible;
    private final IntegerProperty statInfected;
    private final IntegerProperty statRecovered;
    private final IntegerProperty statQuarantined;
    private final IntegerProperty statTotalNodes;
    private final DoubleProperty  statInfectionPercentage;
    private final BooleanProperty precomputed;

    // ── Timestamps ────────────────────────────────────────────────
    private String simulationStartTime;
    private String simulationEndTime;

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public SimulationState() {
        this.history                = new ArrayList<>();
        this.currentIterationIndex  = new SimpleIntegerProperty(-1);
        this.totalIterations        = new SimpleIntegerProperty(0);

        this.playbackState = new SimpleObjectProperty<>(PlaybackState.IDLE);
        this.playbackSpeed = new SimpleDoubleProperty(SPEED_DEFAULT);

        this.contagionRate      = new SimpleDoubleProperty(1.0);
        this.recoveryTime       = new SimpleIntegerProperty(5);
        this.patientZeroIds     = FXCollections.observableArrayList();
        this.quarantinedNodeIds = FXCollections.observableArrayList();
        this.disabledEdgeIds    = FXCollections.observableArrayList();

        this.statSusceptible        = new SimpleIntegerProperty(0);
        this.statInfected           = new SimpleIntegerProperty(0);
        this.statRecovered          = new SimpleIntegerProperty(0);
        this.statQuarantined        = new SimpleIntegerProperty(0);
        this.statTotalNodes         = new SimpleIntegerProperty(0);
        this.statInfectionPercentage = new SimpleDoubleProperty(0.0);

        this.precomputed = new SimpleBooleanProperty(false);
        this.simulationStartTime = null;
        this.simulationEndTime   = null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  HISTORIAL
    // ═══════════════════════════════════════════════════════════════

    public void recordIteration(IterationSnapshot snapshot) {
        history.add(snapshot);
        setTotalIterations(history.size());
        if (history.size() == 1) {
            simulationStartTime = LocalDateTime.now().format(TS_FMT);
        }
    }

    public IterationSnapshot goToIteration(int index) {
        if (index < 0 || index >= history.size()) return null;
        setCurrentIterationIndex(index);
        IterationSnapshot snapshot = history.get(index);
        updateStatsFromSnapshot(snapshot);
        return snapshot;
    }

    public IterationSnapshot stepForward() {
        int next = getCurrentIterationIndex() + 1;
        if (next >= history.size()) {
            setPlaybackState(PlaybackState.FINISHED);
            return null;
        }
        return goToIteration(next);
    }

    public IterationSnapshot stepBackward() {
        int prev = getCurrentIterationIndex() - 1;
        if (prev < 0) return null;
        if (getPlaybackState() == PlaybackState.FINISHED) {
            setPlaybackState(PlaybackState.PAUSED);
        }
        return goToIteration(prev);
    }

    public IterationSnapshot goToStart() {
        if (history.isEmpty()) return null;
        if (getPlaybackState() == PlaybackState.FINISHED) {
            setPlaybackState(PlaybackState.PAUSED);
        }
        return goToIteration(0);
    }

    public IterationSnapshot goToEnd() {
        if (history.isEmpty()) return null;
        return goToIteration(history.size() - 1);
    }

    public IterationSnapshot peekIteration(int index) {
        if (index < 0 || index >= history.size()) return null;
        return history.get(index);
    }

    public IterationSnapshot getCurrentSnapshot() {
        return peekIteration(getCurrentIterationIndex());
    }

    public boolean hasNext()     { return getCurrentIterationIndex() < history.size() - 1; }
    public boolean hasPrevious() { return getCurrentIterationIndex() > 0; }
    public boolean isAtEnd()     { return !history.isEmpty() && getCurrentIterationIndex() >= history.size() - 1; }

    public List<IterationSnapshot> getFullHistory() {
        return Collections.unmodifiableList(history);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONTROLES DE REPRODUCCIÓN
    // ═══════════════════════════════════════════════════════════════

    public void play() {
        if (getPlaybackState() == PlaybackState.FINISHED) goToIteration(0);
        setPlaybackState(PlaybackState.RUNNING);
    }

    public void pause() {
        if (getPlaybackState() == PlaybackState.RUNNING) setPlaybackState(PlaybackState.PAUSED);
    }

    public void togglePlayPause() {
        if (getPlaybackState() == PlaybackState.RUNNING) pause(); else play();
    }

    public void finish() {
        setPlaybackState(PlaybackState.FINISHED);
        simulationEndTime = LocalDateTime.now().format(TS_FMT);
    }

    public boolean isRunning()  { return getPlaybackState() == PlaybackState.RUNNING; }
    public boolean isPaused()   { return getPlaybackState() == PlaybackState.PAUSED; }
    public boolean isFinished() { return getPlaybackState() == PlaybackState.FINISHED; }
    public boolean isIdle()     { return getPlaybackState() == PlaybackState.IDLE; }

    // ═══════════════════════════════════════════════════════════════
    //  PACIENTES CERO
    // ═══════════════════════════════════════════════════════════════

    public boolean addPatientZero(String nodeId) {
        if (nodeId != null && !patientZeroIds.contains(nodeId)) {
            patientZeroIds.add(nodeId);
            return true;
        }
        return false;
    }

    public boolean removePatientZero(String nodeId) {
        return patientZeroIds.remove(nodeId);
    }

    public boolean togglePatientZero(String nodeId) {
        if (patientZeroIds.contains(nodeId)) {
            patientZeroIds.remove(nodeId);
            return false;
        } else {
            patientZeroIds.add(nodeId);
            return true;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  CUARENTENA / CORTAFUEGOS
    // ═══════════════════════════════════════════════════════════════

    public boolean addQuarantinedNode(String nodeId) {
        if (nodeId != null && !quarantinedNodeIds.contains(nodeId)) {
            quarantinedNodeIds.add(nodeId);
            return true;
        }
        return false;
    }

    public boolean removeQuarantinedNode(String nodeId) {
        return quarantinedNodeIds.remove(nodeId);
    }

    public boolean toggleQuarantine(String nodeId) {
        if (quarantinedNodeIds.contains(nodeId)) {
            quarantinedNodeIds.remove(nodeId);
            return false;
        } else {
            quarantinedNodeIds.add(nodeId);
            return true;
        }
    }

    public boolean disableEdge(String edgeId) {
        if (edgeId != null && !disabledEdgeIds.contains(edgeId)) {
            disabledEdgeIds.add(edgeId);
            return true;
        }
        return false;
    }

    public boolean enableEdge(String edgeId)          { return disabledEdgeIds.remove(edgeId); }
    public boolean isEdgeDisabled(String edgeId)      { return disabledEdgeIds.contains(edgeId); }
    public boolean isNodeQuarantined(String nodeId)   { return quarantinedNodeIds.contains(nodeId); }

    // ═══════════════════════════════════════════════════════════════
    //  ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════

    private void updateStatsFromSnapshot(IterationSnapshot snapshot) {
        if (snapshot == null) return;
        IterationStats stats = snapshot.getStats();
        setStatSusceptible(stats.getSusceptibleCount());
        setStatInfected(stats.getInfectedCount());
        setStatRecovered(stats.getRecoveredCount());
        setStatQuarantined(stats.getQuarantinedCount());
        setStatTotalNodes(stats.getTotalNodes());
        setStatInfectionPercentage(stats.getInfectionPercentage());
    }

    public LinkedHashMap<Integer, IterationStats> getStatsTimeline() {
        LinkedHashMap<Integer, IterationStats> timeline = new LinkedHashMap<>();
        for (int i = 0; i < history.size(); i++) {
            timeline.put(i, history.get(i).getStats());
        }
        return timeline;
    }

    // ═══════════════════════════════════════════════════════════════
    //  EXPORTACIÓN
    // ═══════════════════════════════════════════════════════════════

    public String toCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("Iteracion,NodoID,Label,Estado,Profundidad,InfectadoPor,")
                .append("Susceptibles,Infectados,Recuperados,Cuarentena,PorcentajeInfeccion\n");

        for (int i = 0; i < history.size(); i++) {
            IterationSnapshot snap = history.get(i);
            for (NodeSnapshotRecord nodeSnap : snap.getNodeSnapshots()) {
                csv.append(String.format("%d,%s,%s,%s,%d,%s,%d,%d,%d,%d,%.2f\n",
                        i,
                        nodeSnap.getNodeId(),
                        escapeCsv(nodeSnap.getLabel()),
                        nodeSnap.getStatus().getDisplayName(),
                        nodeSnap.getDepthLevel(),
                        nodeSnap.getInfectedBy() != null ? nodeSnap.getInfectedBy() : "N/A",
                        snap.getStats().getSusceptibleCount(),
                        snap.getStats().getInfectedCount(),
                        snap.getStats().getRecoveredCount(),
                        snap.getStats().getQuarantinedCount(),
                        snap.getStats().getInfectionPercentage()
                ));
            }
        }
        return csv.toString();
    }

    public String toTXT() {
        StringBuilder txt = new StringBuilder();
        txt.append("═══════════════════════════════════════════════════════\n");
        txt.append("  REPORTE DE SIMULACIÓN BFS — PROPAGACIÓN DE VIRUS\n");
        txt.append("═══════════════════════════════════════════════════════\n\n");
        txt.append(String.format("  Inicio:           %s\n", simulationStartTime != null ? simulationStartTime : "N/A"));
        txt.append(String.format("  Fin:              %s\n", simulationEndTime != null ? simulationEndTime : "N/A"));
        txt.append(String.format("  Total iteraciones: %d\n", history.size()));
        txt.append(String.format("  Tasa de contagio:  %.0f%%\n", getContagionRate() * 100));
        txt.append(String.format("  Tiempo recuperac.: %d iteraciones\n", getRecoveryTime()));
        txt.append(String.format("  Pacientes cero:    %s\n", patientZeroIds));
        txt.append(String.format("  Nodos cuarentena:  %s\n", quarantinedNodeIds));
        txt.append("\n");

        for (int i = 0; i < history.size(); i++) {
            IterationSnapshot snap = history.get(i);
            txt.append(String.format("──── ITERACIÓN %d ────────────────────────────────\n", i));
            if (!snap.getEvents().isEmpty()) {
                txt.append("  Eventos:\n");
                for (PropagationEvent event : snap.getEvents()) {
                    txt.append(String.format("    %s → %s  (profundidad %d)\n",
                            event.getSourceId() != null ? event.getSourceId() : "[ORIGEN]",
                            event.getTargetId(), event.getDepth()));
                }
            }
            IterationStats stats = snap.getStats();
            txt.append(String.format("  S: %d | I: %d | R: %d | Q: %d | Infección: %.1f%%\n\n",
                    stats.getSusceptibleCount(), stats.getInfectedCount(),
                    stats.getRecoveredCount(), stats.getQuarantinedCount(),
                    stats.getInfectionPercentage()));
        }
        txt.append("═══════════════════════════════════════════════════════\n");
        txt.append("  FIN DEL REPORTE\n");
        txt.append("═══════════════════════════════════════════════���═══════\n");
        return txt.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ═══════════════════════════════════════════════════════════════
    //  RESET
    // ══════════════════════════��════════════════════════════════════

    public void reset() {
        history.clear();
        setCurrentIterationIndex(-1);
        setTotalIterations(0);
        setPlaybackState(PlaybackState.IDLE);
        patientZeroIds.clear();
        quarantinedNodeIds.clear();
        disabledEdgeIds.clear();
        setPrecomputed(false);
        setStatSusceptible(0); setStatInfected(0);
        setStatRecovered(0);   setStatQuarantined(0);
        setStatTotalNodes(0);  setStatInfectionPercentage(0.0);
        simulationStartTime = null;
        simulationEndTime   = null;
    }

    public void softReset() {
        history.clear();
        setCurrentIterationIndex(-1);
        setTotalIterations(0);
        setPlaybackState(PlaybackState.IDLE);
        setPrecomputed(false);
        setStatSusceptible(0); setStatInfected(0);
        setStatRecovered(0);   setStatQuarantined(0);
        setStatTotalNodes(0);  setStatInfectionPercentage(0.0);
        simulationStartTime = null;
        simulationEndTime   = null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  PROPERTIES — Getters, Setters
    // ═══════════════════════════════════════════════════════════════

    public int getCurrentIterationIndex()                   { return currentIterationIndex.get(); }
    public void setCurrentIterationIndex(int idx)           { currentIterationIndex.set(idx); }
    public IntegerProperty currentIterationIndexProperty()  { return currentIterationIndex; }

    public int getTotalIterations()                         { return totalIterations.get(); }
    public void setTotalIterations(int total)               { totalIterations.set(total); }
    public IntegerProperty totalIterationsProperty()        { return totalIterations; }

    public PlaybackState getPlaybackState()                     { return playbackState.get(); }
    public void setPlaybackState(PlaybackState state)           { playbackState.set(state); }
    public ObjectProperty<PlaybackState> playbackStateProperty(){ return playbackState; }

    public double getPlaybackSpeed()                        { return playbackSpeed.get(); }
    public void setPlaybackSpeed(double ms)                 { playbackSpeed.set(Math.max(SPEED_MIN, Math.min(ms, SPEED_MAX))); }
    public DoubleProperty playbackSpeedProperty()           { return playbackSpeed; }

    public double getContagionRate()                        { return contagionRate.get(); }
    public void setContagionRate(double rate)               { contagionRate.set(Math.max(0.0, Math.min(rate, 1.0))); }
    public DoubleProperty contagionRateProperty()           { return contagionRate; }

    public int getRecoveryTime()                            { return recoveryTime.get(); }
    public void setRecoveryTime(int time)                   { recoveryTime.set(Math.max(1, time)); }
    public IntegerProperty recoveryTimeProperty()           { return recoveryTime; }

    public ObservableList<String> getPatientZeroIds()       { return patientZeroIds; }
    public ObservableList<String> getQuarantinedNodeIds()   { return quarantinedNodeIds; }
    public ObservableList<String> getDisabledEdgeIds()      { return disabledEdgeIds; }

    public int getStatSusceptible()                         { return statSusceptible.get(); }
    public void setStatSusceptible(int v)                   { statSusceptible.set(v); }
    public IntegerProperty statSusceptibleProperty()        { return statSusceptible; }

    public int getStatInfected()                            { return statInfected.get(); }
    public void setStatInfected(int v)                      { statInfected.set(v); }
    public IntegerProperty statInfectedProperty()           { return statInfected; }

    public int getStatRecovered()                           { return statRecovered.get(); }
    public void setStatRecovered(int v)                     { statRecovered.set(v); }
    public IntegerProperty statRecoveredProperty()          { return statRecovered; }

    public int getStatQuarantined()                         { return statQuarantined.get(); }
    public void setStatQuarantined(int v)                   { statQuarantined.set(v); }
    public IntegerProperty statQuarantinedProperty()        { return statQuarantined; }

    public int getStatTotalNodes()                          { return statTotalNodes.get(); }
    public void setStatTotalNodes(int v)                    { statTotalNodes.set(v); }
    public IntegerProperty statTotalNodesProperty()         { return statTotalNodes; }

    public double getStatInfectionPercentage()              { return statInfectionPercentage.get(); }
    public void setStatInfectionPercentage(double v)        { statInfectionPercentage.set(v); }
    public DoubleProperty statInfectionPercentageProperty() { return statInfectionPercentage; }

    public boolean isPrecomputed()                          { return precomputed.get(); }
    public void setPrecomputed(boolean v)                   { precomputed.set(v); }
    public BooleanProperty precomputedProperty()            { return precomputed; }

    public String getSimulationStartTime() { return simulationStartTime; }
    public String getSimulationEndTime()   { return simulationEndTime; }

    // ═══════════════════════════════════════════════════════════════
    //  CLASES INTERNAS (en vez de records para compatibilidad)
    // ════════════════════════════════��══════════════════════════════

    /**
     * Snapshot completo de una iteración del BFS.
     */
    public static class IterationSnapshot {
        private final int                       iterationNumber;
        private final List<NodeSnapshotRecord>  nodeSnapshots;
        private final List<String>              bfsQueueState;
        private final List<PropagationEvent>    events;
        private final IterationStats            stats;

        public IterationSnapshot(int iterationNumber,
                                 List<NodeSnapshotRecord> nodeSnapshots,
                                 List<String> bfsQueueState,
                                 List<PropagationEvent> events,
                                 IterationStats stats) {
            this.iterationNumber = iterationNumber;
            this.nodeSnapshots   = Collections.unmodifiableList(new ArrayList<>(nodeSnapshots));
            this.bfsQueueState   = Collections.unmodifiableList(new ArrayList<>(bfsQueueState));
            this.events          = Collections.unmodifiableList(new ArrayList<>(events));
            this.stats           = stats;
        }

        public int                      iterationNumber()  { return iterationNumber; }
        public List<NodeSnapshotRecord> nodeSnapshots()    { return nodeSnapshots; }
        public List<String>             bfsQueueState()    { return bfsQueueState; }
        public List<PropagationEvent>   events()           { return events; }
        public IterationStats           stats()            { return stats; }

        // Alias con get para compatibilidad
        public int                      getIterationNumber()  { return iterationNumber; }
        public List<NodeSnapshotRecord> getNodeSnapshots()    { return nodeSnapshots; }
        public List<String>             getBfsQueueState()    { return bfsQueueState; }
        public List<PropagationEvent>   getEvents()           { return events; }
        public IterationStats           getStats()            { return stats; }
    }

    /**
     * Snapshot de un nodo individual.
     */
    public static class NodeSnapshotRecord {
        private final String      nodeId;
        private final String      label;
        private final NodeStatus  status;
        private final int         depthLevel;
        private final String      infectedBy;
        private final int         infectedDuration;

        public NodeSnapshotRecord(String nodeId, String label, NodeStatus status,
                                  int depthLevel, String infectedBy, int infectedDuration) {
            this.nodeId           = nodeId;
            this.label            = label;
            this.status           = status;
            this.depthLevel       = depthLevel;
            this.infectedBy       = infectedBy;
            this.infectedDuration = infectedDuration;
        }

        public String     nodeId()           { return nodeId; }
        public String     label()            { return label; }
        public NodeStatus status()           { return status; }
        public int        depthLevel()       { return depthLevel; }
        public String     infectedBy()       { return infectedBy; }
        public int        infectedDuration() { return infectedDuration; }

        public String     getNodeId()           { return nodeId; }
        public String     getLabel()            { return label; }
        public NodeStatus getStatus()           { return status; }
        public int        getDepthLevel()       { return depthLevel; }
        public String     getInfectedBy()       { return infectedBy; }
        public int        getInfectedDuration() { return infectedDuration; }
    }

    /**
     * Evento de propagación.
     */
    public static class PropagationEvent {
        private final String  sourceId;
        private final String  targetId;
        private final int     depth;
        private final int     iteration;
        private final boolean wasBlocked;

        public PropagationEvent(String sourceId, String targetId,
                                int depth, int iteration, boolean wasBlocked) {
            this.sourceId   = sourceId;
            this.targetId   = targetId;
            this.depth      = depth;
            this.iteration  = iteration;
            this.wasBlocked = wasBlocked;
        }

        public String  sourceId()   { return sourceId; }
        public String  targetId()   { return targetId; }
        public int     depth()      { return depth; }
        public int     iteration()  { return iteration; }
        public boolean wasBlocked() { return wasBlocked; }

        public String  getSourceId()   { return sourceId; }
        public String  getTargetId()   { return targetId; }
        public int     getDepth()      { return depth; }
        public int     getIteration()  { return iteration; }
        public boolean isWasBlocked()  { return wasBlocked; }

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
     * Estadísticas de una iteración.
     */
    public static class IterationStats {
        private final int     susceptibleCount;
        private final int     infectedCount;
        private final int     recoveredCount;
        private final int     quarantinedCount;
        private final int     totalNodes;
        private final double  infectionPercentage;

        public IterationStats(int susceptibleCount, int infectedCount,
                              int recoveredCount, int quarantinedCount,
                              int totalNodes, double infectionPercentage) {
            this.susceptibleCount    = susceptibleCount;
            this.infectedCount       = infectedCount;
            this.recoveredCount      = recoveredCount;
            this.quarantinedCount    = quarantinedCount;
            this.totalNodes          = totalNodes;
            this.infectionPercentage = infectionPercentage;
        }

        public int    susceptibleCount()    { return susceptibleCount; }
        public int    infectedCount()       { return infectedCount; }
        public int    recoveredCount()      { return recoveredCount; }
        public int    quarantinedCount()    { return quarantinedCount; }
        public int    totalNodes()          { return totalNodes; }
        public double infectionPercentage() { return infectionPercentage; }

        public int    getSusceptibleCount()    { return susceptibleCount; }
        public int    getInfectedCount()       { return infectedCount; }
        public int    getRecoveredCount()      { return recoveredCount; }
        public int    getQuarantinedCount()    { return quarantinedCount; }
        public int    getTotalNodes()          { return totalNodes; }
        public double getInfectionPercentage() { return infectionPercentage; }

        public static IterationStats fromNodeSnapshots(List<NodeSnapshotRecord> snapshots) {
            int s = 0, i = 0, r = 0, q = 0;
            for (NodeSnapshotRecord ns : snapshots) {
                switch (ns.getStatus()) {
                    case SUSCEPTIBLE  -> s++;
                    case INFECTED     -> i++;
                    case PATIENT_ZERO -> i++;
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