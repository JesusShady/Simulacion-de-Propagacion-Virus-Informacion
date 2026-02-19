package model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import model.enums.NodeStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NodeData {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static int globalIdCounter = 0;

    // ── Identidad ─────────────────────────────────────────────────
    private final String id;
    private final StringProperty label;
    private final StringProperty description;

    // ── Estado SIR ────────────────────────────────────────────────
    private final ObjectProperty<NodeStatus> status;
    private NodeStatus previousStatus;

    // ── Posición ──────────────────────────────────────────────────
    private final DoubleProperty posX;
    private final DoubleProperty posY;

    // ── Genealogía BFS ────────────────────────────────────────────
    private final IntegerProperty depthLevel;
    private final StringProperty infectedBy;
    private final IntegerProperty infectionIteration;
    private final IntegerProperty recoveryIteration;
    private final IntegerProperty infectedDuration;
    private final List<String> nodesInfectedByThis;

    // ── Visual ────────────────────────────────────────────────────
    private final BooleanProperty selected;
    private final BooleanProperty hovered;
    private final DoubleProperty visualSize;
    private final BooleanProperty superSpreader;

    // ── Metadatos ─────────────────────────────────────────────────
    private final String createdAt;
    private String infectedAt;
    private String recoveredAt;
    private final IntegerProperty degree;

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════

    public NodeData(String id, String label, double x, double y) {
        this.id          = Objects.requireNonNull(id, "El ID del nodo no puede ser null");
        this.label       = new SimpleStringProperty(label != null ? label : id);
        this.description = new SimpleStringProperty("");

        this.status         = new SimpleObjectProperty<>(NodeStatus.SUSCEPTIBLE);
        this.previousStatus = NodeStatus.SUSCEPTIBLE;

        this.posX = new SimpleDoubleProperty(x);
        this.posY = new SimpleDoubleProperty(y);

        this.depthLevel          = new SimpleIntegerProperty(-1);
        this.infectedBy          = new SimpleStringProperty(null);
        this.infectionIteration  = new SimpleIntegerProperty(-1);
        this.recoveryIteration   = new SimpleIntegerProperty(-1);
        this.infectedDuration    = new SimpleIntegerProperty(0);
        this.nodesInfectedByThis = new ArrayList<>();

        this.selected      = new SimpleBooleanProperty(false);
        this.hovered       = new SimpleBooleanProperty(false);
        this.visualSize    = new SimpleDoubleProperty(30.0);
        this.superSpreader = new SimpleBooleanProperty(false);

        this.createdAt   = LocalDateTime.now().format(TIMESTAMP_FMT);
        this.infectedAt  = null;
        this.recoveredAt = null;
        this.degree      = new SimpleIntegerProperty(0);
    }

    public NodeData(double x, double y) {
        this("N" + (globalIdCounter++), null, x, y);
    }

    public NodeData(String id, double x, double y) {
        this(id, id, x, y);
    }

    // ═══════════════════════════════════════════════════════════════
    //  LÓGICA DE TRANSICIÓN SIR
    // ═══════════════════════════════════════════════════════════════

    public boolean infect(String sourceNodeId, int depth, int iteration, boolean asPatientZero) {
        if (!getStatus().canBeInfected() && !asPatientZero) {
            return false;
        }
        this.previousStatus = getStatus();
        setStatus(asPatientZero ? NodeStatus.PATIENT_ZERO : NodeStatus.INFECTED);
        setDepthLevel(depth);
        setInfectedBy(sourceNodeId);
        setInfectionIteration(iteration);
        setInfectedDuration(0);
        this.infectedAt = LocalDateTime.now().format(TIMESTAMP_FMT);
        return true;
    }

    public boolean infect(String sourceNodeId, int depth, int iteration) {
        return infect(sourceNodeId, depth, iteration, false);
    }

    public boolean recover(int iteration) {
        if (getStatus() != NodeStatus.INFECTED && getStatus() != NodeStatus.PATIENT_ZERO) {
            return false;
        }
        this.previousStatus = getStatus();
        setStatus(NodeStatus.RECOVERED);
        setRecoveryIteration(iteration);
        this.recoveredAt = LocalDateTime.now().format(TIMESTAMP_FMT);
        return true;
    }

    public boolean quarantine() {
        if (getStatus() == NodeStatus.QUARANTINED) {
            return false;
        }
        this.previousStatus = getStatus();
        setStatus(NodeStatus.QUARANTINED);
        return true;
    }

    public boolean unquarantine() {
        if (getStatus() != NodeStatus.QUARANTINED) {
            return false;
        }
        setStatus(previousStatus != NodeStatus.QUARANTINED ? previousStatus : NodeStatus.SUSCEPTIBLE);
        return true;
    }

    public void tickInfection() {
        if (getStatus().canSpread()) {
            setInfectedDuration(getInfectedDuration() + 1);
        }
    }

    public void addInfectedNode(String targetNodeId) {
        if (targetNodeId != null && !nodesInfectedByThis.contains(targetNodeId)) {
            nodesInfectedByThis.add(targetNodeId);
        }
    }

    public void reset() {
        this.previousStatus = NodeStatus.SUSCEPTIBLE;
        setStatus(NodeStatus.SUSCEPTIBLE);
        setDepthLevel(-1);
        setInfectedBy(null);
        setInfectionIteration(-1);
        setRecoveryIteration(-1);
        setInfectedDuration(0);
        nodesInfectedByThis.clear();
        this.infectedAt  = null;
        this.recoveredAt = null;
        setSelected(false);
        setSuperSpreader(false);
    }

    public void restorePreviousState() {
        NodeStatus current = getStatus();
        setStatus(previousStatus);
        this.previousStatus = current;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSULTAS
    // ═══════════════════════════════════════════════════════════════

    public boolean canSpread()       { return getStatus().canSpread(); }
    public boolean canBeInfected()   { return getStatus().canBeInfected(); }
    public boolean isActive()        { return getStatus().isActive(); }
    public boolean wasEverInfected() { return getInfectionIteration() >= 0; }
    public int getSpreadCount()      { return nodesInfectedByThis.size(); }

    public List<String> getNodesInfectedByThis() {
        return Collections.unmodifiableList(nodesInfectedByThis);
    }

    // ═══════════════════════════════════════════════════════════════
    //  JAVAFX PROPERTIES
    // ═══════════════════════════════════════════════════════════════

    public String getId() { return id; }

    public String getLabel()                { return label.get(); }
    public void setLabel(String label)      { this.label.set(label); }
    public StringProperty labelProperty()   { return label; }

    public String getDescription()                  { return description.get(); }
    public void setDescription(String description)  { this.description.set(description); }
    public StringProperty descriptionProperty()     { return description; }

    public NodeStatus getStatus()                      { return status.get(); }
    public void setStatus(NodeStatus status)           { this.status.set(status); }
    public ObjectProperty<NodeStatus> statusProperty() { return status; }
    public NodeStatus getPreviousStatus()              { return previousStatus; }

    public double getPosX()              { return posX.get(); }
    public void setPosX(double x)        { posX.set(x); }
    public DoubleProperty posXProperty() { return posX; }

    public double getPosY()              { return posY.get(); }
    public void setPosY(double y)        { posY.set(y); }
    public DoubleProperty posYProperty() { return posY; }

    public int getDepthLevel()                    { return depthLevel.get(); }
    public void setDepthLevel(int level)          { depthLevel.set(level); }
    public IntegerProperty depthLevelProperty()   { return depthLevel; }

    public String getInfectedBy()                   { return infectedBy.get(); }
    public void setInfectedBy(String id)            { infectedBy.set(id); }
    public StringProperty infectedByProperty()      { return infectedBy; }

    public int getInfectionIteration()                        { return infectionIteration.get(); }
    public void setInfectionIteration(int iter)               { infectionIteration.set(iter); }
    public IntegerProperty infectionIterationProperty()       { return infectionIteration; }

    public int getRecoveryIteration()                         { return recoveryIteration.get(); }
    public void setRecoveryIteration(int iter)                { recoveryIteration.set(iter); }
    public IntegerProperty recoveryIterationProperty()        { return recoveryIteration; }

    public int getInfectedDuration()                          { return infectedDuration.get(); }
    public void setInfectedDuration(int duration)             { infectedDuration.set(duration); }
    public IntegerProperty infectedDurationProperty()         { return infectedDuration; }

    public boolean isSelected()                      { return selected.get(); }
    public void setSelected(boolean val)             { selected.set(val); }
    public BooleanProperty selectedProperty()        { return selected; }

    public boolean isHovered()                       { return hovered.get(); }
    public void setHovered(boolean val)              { hovered.set(val); }
    public BooleanProperty hoveredProperty()         { return hovered; }

    public double getVisualSize()                    { return visualSize.get(); }
    public void setVisualSize(double size)           { visualSize.set(size); }
    public DoubleProperty visualSizeProperty()       { return visualSize; }

    public boolean isSuperSpreader()                     { return superSpreader.get(); }
    public void setSuperSpreader(boolean val)            { superSpreader.set(val); }
    public BooleanProperty superSpreaderProperty()       { return superSpreader; }

    public int getDegree()                        { return degree.get(); }
    public void setDegree(int deg)                { degree.set(deg); }
    public IntegerProperty degreeProperty()       { return degree; }

    public String getCreatedAt()   { return createdAt; }
    public String getInfectedAt()  { return infectedAt; }
    public String getRecoveredAt() { return recoveredAt; }

    // ═══════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    public double distanceTo(double x, double y) {
        double dx = getPosX() - x;
        double dy = getPosY() - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean containsPoint(double x, double y, double margin) {
        return distanceTo(x, y) <= (getVisualSize() / 2.0) + margin;
    }

    public boolean containsPoint(double x, double y) {
        return containsPoint(x, y, 4.0);
    }

    // ═══════════════════════════════════════════════════════════════
    //  SNAPSHOT (Clase interna en vez de record para compatibilidad)
    // ═══════════════════════════════════════════════════════════════

    public Snapshot takeSnapshot() {
        return new Snapshot(
                getStatus(),
                getDepthLevel(),
                getInfectedBy(),
                getInfectionIteration(),
                getRecoveryIteration(),
                getInfectedDuration(),
                new ArrayList<>(nodesInfectedByThis),
                infectedAt,
                recoveredAt
        );
    }

    public void restoreFromSnapshot(Snapshot snapshot) {
        setStatus(snapshot.status);
        setDepthLevel(snapshot.depthLevel);
        setInfectedBy(snapshot.infectedBy);
        setInfectionIteration(snapshot.infectionIteration);
        setRecoveryIteration(snapshot.recoveryIteration);
        setInfectedDuration(snapshot.infectedDuration);
        nodesInfectedByThis.clear();
        nodesInfectedByThis.addAll(snapshot.infectedNodes);
        this.infectedAt  = snapshot.infectedAt;
        this.recoveredAt = snapshot.recoveredAt;
    }

    /**
     * Snapshot del estado de un nodo. Clase estática interna.
     */
    public static class Snapshot {
        private final NodeStatus   status;
        private final int          depthLevel;
        private final String       infectedBy;
        private final int          infectionIteration;
        private final int          recoveryIteration;
        private final int          infectedDuration;
        private final List<String> infectedNodes;
        private final String       infectedAt;
        private final String       recoveredAt;

        public Snapshot(NodeStatus status, int depthLevel, String infectedBy,
                        int infectionIteration, int recoveryIteration,
                        int infectedDuration, List<String> infectedNodes,
                        String infectedAt, String recoveredAt) {
            this.status             = status;
            this.depthLevel         = depthLevel;
            this.infectedBy         = infectedBy;
            this.infectionIteration = infectionIteration;
            this.recoveryIteration  = recoveryIteration;
            this.infectedDuration   = infectedDuration;
            this.infectedNodes      = infectedNodes;
            this.infectedAt         = infectedAt;
            this.recoveredAt        = recoveredAt;
        }

        public NodeStatus   status()             { return status; }
        public int          depthLevel()         { return depthLevel; }
        public String       infectedBy()         { return infectedBy; }
        public int          infectionIteration() { return infectionIteration; }
        public int          recoveryIteration()  { return recoveryIteration; }
        public int          infectedDuration()   { return infectedDuration; }
        public List<String> infectedNodes()      { return infectedNodes; }
        public String       infectedAt()         { return infectedAt; }
        public String       recoveredAt()        { return recoveredAt; }
    }

    // ═══════════════════════════════════════════════════════════════
    //  STATIC UTILS
    // ═══════════════════════════════════════════════════════════════

    public static void resetIdCounter() {
        globalIdCounter = 0;
    }

    public static int getCurrentIdCounter() {
        return globalIdCounter;
    }

    // ════════��══════════════════════════════════════════════════════
    //  equals, hashCode, toString
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeData nodeData = (NodeData) o;
        return Objects.equals(id, nodeData.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "NodeData{ id='%s', label='%s', status=%s, depth=%d, " +
                        "infectedBy='%s', pos=(%.1f, %.1f), degree=%d, spread=%d }",
                id, getLabel(), getStatus().getDisplayName(), getDepthLevel(),
                getInfectedBy(), getPosX(), getPosY(), getDegree(), getSpreadCount()
        );
    }
}