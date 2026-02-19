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

    //formateador de tiempos
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    //contador global para generar ids unicos
    private static int globalIdCounter = 0;

    //---identidad de nods

    //identificador unico del nodo
    private final String id;
    //Nombre visible en la interfaz el usuario lo puede editar
    private final StringProperty label;
    //descripcion opcional del nodo
    private final StringProperty description;

    //----Estador Observable

    private final ObjectProperty<EstadoNodo> status;

    private EstadoNodo previusStatus;

    //---Posicion en el lienzo drag & drop

    private final DoubleProperty posX;
    private final DoubleProperty posY;


    //-----AQUI SE TRAZA LA INFECCION - USO DEL BFS

      /** Nivel de profundidad en el BFS (0 = paciente cero, 1 = primer anillo, etc.) */
    private final IntegerProperty depthLevel;

    /** ID del nodo que infectó a este (null si es paciente cero o no infectado) */
    private final StringProperty infectedBy;

    /** Iteración del BFS en la que este nodo fue infectado (-1 si no ha sido infectado) */
    private final IntegerProperty infectionIteration;

    /** Iteración del BFS en la que este nodo se recuperó (-1 si no se ha recuperado) */
    private final IntegerProperty recoveryIteration;

    /** Número de iteraciones que lleva infectado (para transición a RECOVERED) */
    private final IntegerProperty infectedDuration;

    /** Lista de IDs de nodos que ESTE nodo infectó directamente */
    private final List<String> nodesInfectedByThis;


    //------------Timestamps

     /** Momento en que se creó el nodo */
    private final String createdAt;

    /** Momento en que fue infectado (null si nunca) */
    private String infectedAt;

    /** Momento en que se recuperó (null si nunca) */
    private String recoveredAt;

    /** Grado del nodo (número de conexiones directas) — se actualiza externamente */
    private final IntegerProperty degree;

    //-----constructor

    public DatosNodo(String id, String label, double x, double y) {
        // Identidad
        this.id          = Objects.requireNonNull(id, "El ID del nodo no puede ser null");
        this.label       = new SimpleStringProperty(label != null ? label : id);
        this.description = new SimpleStringProperty("");

        // Estado SIR
        this.status         = new SimpleObjectProperty<>(NodeStatus.SUSCEPTIBLE);
        this.previousStatus = NodeStatus.SUSCEPTIBLE;

        // Posición
        this.posX = new SimpleDoubleProperty(x);
        this.posY = new SimpleDoubleProperty(y);

        // Genealogía BFS
        this.depthLevel          = new SimpleIntegerProperty(-1);
        this.infectedBy          = new SimpleStringProperty(null);
        this.infectionIteration  = new SimpleIntegerProperty(-1);
        this.recoveryIteration   = new SimpleIntegerProperty(-1);
        this.infectedDuration    = new SimpleIntegerProperty(0);
        this.nodesInfectedByThis = new ArrayList<>();

        // Visual
        this.selected      = new SimpleBooleanProperty(false);
        this.hovered       = new SimpleBooleanProperty(false);
        this.visualSize    = new SimpleDoubleProperty(30.0);
        this.superSpreader = new SimpleBooleanProperty(false);

        // Metadatos
        this.createdAt   = LocalDateTime.now().format(TIMESTAMP_FMT);
        this.infectedAt  = null;
        this.recoveredAt = null;
        this.degree      = new SimpleIntegerProperty(0);
    }

    public NodeData(double x, double y){
        this("N" + (globalIdCounter++), null, x,y);
    }

    public NodeData(String id, double x, double y){
        this(id, id, x, y);
    }

 //------- LOGICA DE TRANSICION DE ESTADOS

     /**
     * Infecta este nodo durante la simulación BFS.
     *
     * @param sourceNodeId  ID del nodo que lo infecta (puede ser null si es paciente cero)
     * @param depth         nivel de profundidad BFS
     * @param iteration     número de iteración actual del BFS
     * @param asPatientZero true si debe marcarse como PATIENT_ZERO
     * @return true si la infección fue exitosa, false si el nodo no podía ser infectado
     */

    public boolean infect(String sourceNodeId, int depth, int iteration, boolean asPatientZero) {
        // Solo los nodos SUSCEPTIBLE pueden infectarse
        if (!getStatus().canBeInfected() && !asPatientZero) {
            return false;
        }

        // Guardar estado previo para el historial
        this.previousStatus = getStatus();

        // Transicionar estado
        setStatus(asPatientZero ? NodeStatus.PATIENT_ZERO : NodeStatus.INFECTED);

        // Registrar genealogía
        setDepthLevel(depth);
        setInfectedBy(sourceNodeId);
        setInfectionIteration(iteration);
        setInfectedDuration(0);

        // Timestamp
        this.infectedAt = LocalDateTime.now().format(TIMESTAMP_FMT);

        return true;
    }

    //verision simplificada para infectar como vecino
    public boolean infect(String sourceNodeId, int depth, int iteration) {
        return infect(sourceNodeId, depth, iteration, false);
    }

      /**
     * Transiciona el nodo a RECOVERED.
     * Solo puede recuperarse si está INFECTED o es PATIENT_ZERO.
     *
     * @param iteration iteración en la que se recupera
     * @return true si la recuperación fue exitosa
     */
    
      public boolean recover(int iteration){
        if(getStatus() != NodeStatus.INFECTED && getStatus() != NodeStatus.PATIENT_ZERO){
            return false;
        }

        this.previusStatus = getStatus();
        setStatus(NodeStatus.RECOVERED);
        setRecoveryIteration(iteration);
        this.recoveredAt = LocalDateTime.now().format(TIMESTAMP_FMT);

        return true;
      }

      /**
     * Pone el nodo en cuarentena (bloquea propagación).
     * Se puede poner en cuarentena desde cualquier estado excepto INFECTED/PATIENT_ZERO
     * (opcionalmente, se podría permitir — decisión de diseño).
     *
     * @return true si se puso en cuarentena exitosamente
     */

      public boolean quarentine(){
        if(getStatus() == NodeStatus.QUARANTINED){
            return false;
        }

        this.previusStatus = getStatus();
        setStatus(NodeStatus.QUARANTINED);
        return true;
      }


    /**
     * Saca el nodo de cuarentena, restaurando su estado previo.
     *
     * @return true si se sacó de cuarentena exitosamente
     */

    public boolean unquarantine(){
        if(getStatus() != NodeStatus.QUARANTINED){
            return false;
        }

        setStatus(previusStatus != NodeStatus.QUARANTINED ? previusStatus : NodeStatus.SUSCEPTIBLE);
        return true;
    }

    /**
     * Incrementa el contador de duración de infección.
     * Usado por el motor BFS para saber cuándo transicionar a RECOVERED.
     */
    public void tickInfection() {
        if (getStatus().canSpread()) {
            setInfectedDuration(getInfectedDuration() + 1);
        }
    }

      /**
     * Registra que este nodo infectó a otro nodo.
     *
     * @param targetNodeId ID del nodo que fue infectado por este
     */
    public void addInfectedNode(String targetNodeId) {
        if (targetNodeId != null && !nodesInfectedByThis.contains(targetNodeId)) {
            nodesInfectedByThis.add(targetNodeId);
        }
    }

    // resetea el nodo a su estado original
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


    //restaura el estado previo
    public void restorePreviousState() {
        NodeStatus current = getStatus();
        setStatus(previousStatus);
        this.previousStatus = current;
    }

    //-----CONSULTAS
    
    /**
     * @return true si el nodo puede propagar la infección a sus vecinos
     */
    public boolean canSpread() {
        return getStatus().canSpread();
    }

    /**
     * @return true si el nodo puede ser infectado
     */
    public boolean canBeInfected() {
        return getStatus().canBeInfected();
    }

    /**
     * @return true si el nodo está activo en la simulación (no en cuarentena)
     */
    public boolean isActive() {
        return getStatus().isActive();
    }

    /**
     * @return true si el nodo fue infectado en algún momento
     */
    public boolean wasEverInfected() {
        return getInfectionIteration() >= 0;
    }

    /**
     * @return cantidad de nodos que este infectó directamente
     */
    public int getSpreadCount() {
        return nodesInfectedByThis.size();
    }

    /**
     * @return lista inmutable de IDs de nodos infectados por este
     */
    public List<String> getNodesInfectedByThis() {
        return Collections.unmodifiableList(nodesInfectedByThis);
    }


    //---- Getters, setters, 

     // ── ID (inmutable) ────────────────────────────────────────────
    public String getId() { return id; }

    // ── Label ─────────────────────────────────────────────────────
    public String getLabel()                { return label.get(); }
    public void setLabel(String label)      { this.label.set(label); }
    public StringProperty labelProperty()   { return label; }

    // ── Description ───────────────────────────────────────────────
    public String getDescription()                  { return description.get(); }
    public void setDescription(String description)  { this.description.set(description); }
    public StringProperty descriptionProperty()     { return description; }

    // ── Status ────────────────────────────────────────────────────
    public NodeStatus getStatus()                      { return status.get(); }
    public void setStatus(NodeStatus status)           { this.status.set(status); }
    public ObjectProperty<NodeStatus> statusProperty() { return status; }
    public NodeStatus getPreviousStatus()              { return previousStatus; }

    // ── Posición X ────────────────────────────────────────────────
    public double getPosX()              { return posX.get(); }
    public void setPosX(double x)        { posX.set(x); }
    public DoubleProperty posXProperty() { return posX; }

    // ── Posición Y ────────────────────────────────────────────────
    public double getPosY()              { return posY.get(); }
    public void setPosY(double y)        { posY.set(y); }
    public DoubleProperty posYProperty() { return posY; }

    // ── Depth Level ───────────────────────────────────────────────
    public int getDepthLevel()                    { return depthLevel.get(); }
    public void setDepthLevel(int level)          { depthLevel.set(level); }
    public IntegerProperty depthLevelProperty()   { return depthLevel; }

    // ── Infected By ───────────────────────────────────────────────
    public String getInfectedBy()                   { return infectedBy.get(); }
    public void setInfectedBy(String id)            { infectedBy.set(id); }
    public StringProperty infectedByProperty()      { return infectedBy; }

    // ── Infection Iteration ───────────────────────────────────────
    public int getInfectionIteration()                        { return infectionIteration.get(); }
    public void setInfectionIteration(int iter)               { infectionIteration.set(iter); }
    public IntegerProperty infectionIterationProperty()       { return infectionIteration; }

    // ── Recovery Iteration ────────────────────────────────────────
    public int getRecoveryIteration()                         { return recoveryIteration.get(); }
    public void setRecoveryIteration(int iter)                { recoveryIteration.set(iter); }
    public IntegerProperty recoveryIterationProperty()        { return recoveryIteration; }

    // ── Infected Duration ─────────────────────────────────────────
    public int getInfectedDuration()                          { return infectedDuration.get(); }
    public void setInfectedDuration(int duration)             { infectedDuration.set(duration); }
    public IntegerProperty infectedDurationProperty()         { return infectedDuration; }

    // ── Selected ──────────────────────────────────────────────────
    public boolean isSelected()                      { return selected.get(); }
    public void setSelected(boolean val)             { selected.set(val); }
    public BooleanProperty selectedProperty()        { return selected; }

    // ── Hovered ───────────────────────────────────────────────────
    public boolean isHovered()                       { return hovered.get(); }
    public void setHovered(boolean val)              { hovered.set(val); }
    public BooleanProperty hoveredProperty()         { return hovered; }

    // ── Visual Size ───────────────────────────────────────────────
    public double getVisualSize()                    { return visualSize.get(); }
    public void setVisualSize(double size)           { visualSize.set(size); }
    public DoubleProperty visualSizeProperty()       { return visualSize; }

    // ── Super Spreader ────────────────────────────────────────────
    public boolean isSuperSpreader()                     { return superSpreader.get(); }
    public void setSuperSpreader(boolean val)            { superSpreader.set(val); }
    public BooleanProperty superSpreaderProperty()       { return superSpreader; }

    // ── Degree ────────────────────────────────────────────────────
    public int getDegree()                        { return degree.get(); }
    public void setDegree(int deg)                { degree.set(deg); }
    public IntegerProperty degreeProperty()       { return degree; }

    // ── Timestamps ────────────────────────────────────────────────
    public String getCreatedAt()   { return createdAt; }
    public String getInfectedAt()  { return infectedAt; }
    public String getRecoveredAt() { return recoveredAt; }

    /**
     * Calcula la distancia euclidiana desde este nodo hasta un punto (x, y).
     * Útil para detectar clics del mouse sobre el nodo.
     *
     * @param x coordenada X del punto
     * @param y coordenada Y del punto
     * @return distancia en píxeles
     */
    public double distanceTo(double x, double y) {
        double dx = getPosX() - x;
        double dy = getPosY() - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Verifica si un punto (x, y) cae dentro del área visual del nodo.
     *
     * @param x      coordenada X del punto
     * @param y      coordenada Y del punto
     * @param margin margen adicional de tolerancia (en px)
     * @return true si el punto está dentro del nodo
     */
    public boolean containsPoint(double x, double y, double margin) {
        return distanceTo(x, y) <= (getVisualSize() / 2.0) + margin;
    }

    /**
     * Verifica si un punto (x, y) cae dentro del nodo (sin margen extra).
     */
    public boolean containsPoint(double x, double y) {
        return containsPoint(x, y, 4.0); // 4px de tolerancia por defecto
    }

    /**
     * Genera un snapshot del estado actual para el historial.
     * Usado por la función "Paso Atrás" para restaurar estados.
     *
     * @return un objeto Snapshot con los datos actuales clonados
     */
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

    /**
     * Restaura este nodo desde un snapshot previo.
     *
     * @param snapshot el estado guardado previamente
     */
    public void restoreFromSnapshot(Snapshot snapshot) {
        setStatus(snapshot.status());
        setDepthLevel(snapshot.depthLevel());
        setInfectedBy(snapshot.infectedBy());
        setInfectionIteration(snapshot.infectionIteration());
        setRecoveryIteration(snapshot.recoveryIteration());
        setInfectedDuration(snapshot.infectedDuration());
        nodesInfectedByThis.clear();
        nodesInfectedByThis.addAll(snapshot.infectedNodes());
        this.infectedAt  = snapshot.infectedAt();
        this.recoveredAt = snapshot.recoveredAt();
    }

    /**
     * Snapshot inmutable del estado de un nodo en un momento dado.
     * Implementado como Record de Java 17 para máxima limpieza.
     */
    public record Snapshot(
        NodeStatus   status,
        int          depthLevel,
        String       infectedBy,
        int          infectionIteration,
        int          recoveryIteration,
        int          infectedDuration,
        List<String> infectedNodes,
        String       infectedAt,
        String       recoveredAt
    ) {}

    /**
     * Resetea el contador global de IDs.
     * Útil al limpiar toda la red para empezar de cero.
     */
    public static void resetIdCounter() {
        globalIdCounter = 0;
    }

    /**
     * Retorna el valor actual del contador de IDs (para debugging).
     */
    public static int getCurrentIdCounter() {
        return globalIdCounter;
    }

    // ═══════════════════════════════════════════════════════════════
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
