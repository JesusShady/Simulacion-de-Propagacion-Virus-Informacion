package model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.enums.NodeStatus;
import model.SimulationState.IterationSnapshot;
import model.SimulationState.IterationStats;
import model.SimulationState.NodeSnapshotRecord;
import model.SimulationState.PropagationEvent;
import model.TopologyGenerator.EdgeInfo;
import model.TopologyGenerator.NodeInfo;
import model.TopologyGenerator.TopologyResult;
import model.TopologyGenerator.TopologyType;

import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GraphModel {
    
//---Estructura del grafo

/** Mapa de nodos: ID → NodeData */
    private final Map<String, NodeData> nodes;

    /** Lista de adyacencia: nodeId → Set de IDs de vecinos */
    private final Map<String, Set<String>> adjacencyList;

    /** Registro de aristas: edgeId → EdgeRecord */
    private final Map<String, EdgeRecord> edges;

    /** Lista observable de IDs de nodos (para binding con la Vista) */
    private final ObservableList<String> nodeIds;

    /** Lista observable de IDs de aristas */
    private final ObservableList<String> edgeIds;

//Estado de la simulacion

 /** Número total de nodos */
    private final IntegerProperty nodeCount;

    /** Numero total de aristas */
    private final IntegerProperty edgeCount;

    /** ¿Hay cambios sin guardar en el grafo? */
    private final BooleanProperty modified;

    /** ¿El grafo está vacío? */
    private final BooleanProperty empty;

    /** Nodo actualmente seleccionado por el usuario */
    private final ObjectProperty<NodeData> selectedNode;

    /** Nodo origen temporal para crear aristas (drag) */
    private final ObjectProperty<NodeData> edgeSourceNode;

    /** ¿Estamos en modo "crear arista"? */
    private final BooleanProperty edgeCreationMode;

    /** ¿Estamos en modo "cuarentena"? */
    private final BooleanProperty quarantineMode;

    /** ¿Estamos en modo "seleccionar paciente cero"? */
    private final BooleanProperty patientZeroMode;

    /** Estado de la simulacion */
    private final SimulationState simulationState;

    /** Generador de topologias */
    private final TopologyGenerator topologyGenerator;

    /** Generador de numeros aleatorios */
    private final Random random;

    //---Record interno para aristas


        //Representacion de una arista en el grafo
    public record EdgeRecord(
        String id,
        String sourceId,
        String targetId,
        boolean disabled
    ){
         public static String generateId(String source, String target) {
        if (source.compareTo(target) <= 0) {
            return source + "-" + target;
        }
        return target + "-" + source;
    }
    }


    


//Constructor

public GraphModel() {
        // Estructura del grafo
        this.nodes          = new LinkedHashMap<>();
        this.adjacencyList  = new ConcurrentHashMap<>();
        this.edges          = new LinkedHashMap<>();
        this.nodeIds        = FXCollections.observableArrayList();
        this.edgeIds        = FXCollections.observableArrayList();

        // Simulación
        this.simulationState   = new SimulationState();
        this.topologyGenerator = new TopologyGenerator();
        this.random            = new Random();

        // Propiedades observables
        this.nodeCount        = new SimpleIntegerProperty(0);
        this.edgeCount        = new SimpleIntegerProperty(0);
        this.modified         = new SimpleBooleanProperty(false);
        this.empty            = new SimpleBooleanProperty(true);
        this.selectedNode     = new SimpleObjectProperty<>(null);
        this.edgeSourceNode   = new SimpleObjectProperty<>(null);
        this.edgeCreationMode = new SimpleBooleanProperty(false);
        this.quarantineMode   = new SimpleBooleanProperty(false);
        this.patientZeroMode  = new SimpleBooleanProperty(false);
    }


    //---Operaciones CRUD - NODOS


    public boolean addNode(NodeData node) {
        Objects.requireNonNull(node, "El nodo no puede ser null");

        if (nodes.containsKey(node.getId())) {
            System.out.println("[GraphModel] Nodo duplicado ignorado: " + node.getId());
            return false;
        }

        nodes.put(node.getId(), node);
        adjacencyList.put(node.getId(), new HashSet<>());
        nodeIds.add(node.getId());

        updateCounts();
        setModified(true);

        System.out.println("[GraphModel] Nodo agregado: " + node.getId()
            + " en (" + node.getPosX() + ", " + node.getPosY() + ")");
        return true;
    }

    public NodeData createNode(double x, double y) {
        NodeData node = new NodeData(x, y);
        addNode(node);
        return node;
    }


    /*Crea y agrega un nodo con ID y posicion especificos */

    public NodeData createNode(String id, String label, double x, double y){
        NodeData node = new NodeData(id, label, x, y);
        addNode(node);
        return node;
    }


    /*Elimina un nodo y todas sus aristas */
    public boolean removeNode(String nodeId) {
        NodeData node = nodes.get(nodeId);
        if (node == null) return false;

        // Eliminar todas las aristas conectadas a este nodo
        Set<String> neighbors = new HashSet<>(adjacencyList.getOrDefault(nodeId, Set.of()));
        for (String neighborId : neighbors) {
            removeEdge(nodeId, neighborId);
        }

        // Eliminar de las listas de simulación
        simulationState.removePatientZero(nodeId);
        simulationState.removeQuarantinedNode(nodeId);

        // Eliminar el nodo
        nodes.remove(nodeId);
        adjacencyList.remove(nodeId);
        nodeIds.remove(nodeId);

        // Limpiar selección si era este nodo
        if (getSelectedNode() != null && getSelectedNode().getId().equals(nodeId)) {
            setSelectedNode(null);
        }

        updateCounts();
        setModified(true);

        System.out.println("[GraphModel] Nodo eliminado: " + nodeId);
        return true;
    }

    //Obtener un nodo por su id
      public NodeData getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * @return colección inmutable de todos los nodos
     */
    public List<NodeData> getAllNodes() {
        return List.copyOf(nodes.values());
    }

    /*Busca el nodo que contiene el punto (x, y) en el canvas.
     * Útil para detectar clics del mouse. */


    public NodeData findNodeAt(double x, double y) {
        // Recorrer en orden inverso para que los nodos "de arriba" tengan prioridad
        List<NodeData> allNodes = new ArrayList<>(nodes.values());
        Collections.reverse(allNodes);

        for (NodeData node : allNodes) {
            if (node.containsPoint(x, y)) {
                return node;
            }
        }
        return null;
    }

    //OPERACIONES CRUD - ARISTAS

    /** Agrega una arista entre dos nodos.
     * El grafo es NO dirigido: A-B == B-A. */

    public boolean addEdge(String sourceId, String targetId) {
        // Validaciones
        if (sourceId == null || targetId == null) return false;
        if (sourceId.equals(targetId)) return false; // No self-loops
        if (!nodes.containsKey(sourceId) || !nodes.containsKey(targetId)) return false;

        String edgeId = EdgeRecord.generateId(sourceId, targetId);

        // Verificar que no exista ya
        if (edges.containsKey(edgeId)) return false;

        // Crear la arista
        EdgeRecord edge = new EdgeRecord(edgeId, sourceId, targetId, false);
        edges.put(edgeId, edge);
        edgeIds.add(edgeId);

        // Actualizar lista de adyacencia (bidireccional)
        adjacencyList.computeIfAbsent(sourceId, k -> new HashSet<>()).add(targetId);
        adjacencyList.computeIfAbsent(targetId, k -> new HashSet<>()).add(sourceId);

        // Actualizar grados de los nodos
        updateNodeDegree(sourceId);
        updateNodeDegree(targetId);

        updateCounts();
        setModified(true);

        System.out.println("[GraphModel] Arista creada: " + edgeId);
        return true;
    }

    //Elimina una arista entre dos nodos.

    public boolean removeEdge(String sourceId, String targetId) {
        String edgeId = EdgeRecord.generateId(sourceId, targetId);
        EdgeRecord edge = edges.get(edgeId);
        if (edge == null) return false;

        edges.remove(edgeId);
        edgeIds.remove(edgeId);

        // Actualizar lista de adyacencia
        Set<String> sourceNeighbors = adjacencyList.get(sourceId);
        if (sourceNeighbors != null) sourceNeighbors.remove(targetId);

        Set<String> targetNeighbors = adjacencyList.get(targetId);
        if (targetNeighbors != null) targetNeighbors.remove(sourceId);

        // Actualizar grados
        updateNodeDegree(sourceId);
        updateNodeDegree(targetId);

        // También quitar de aristas desactivadas
        simulationState.enableEdge(edgeId);

        updateCounts();
        setModified(true);

        return true;
    }


    /**
     * Obtiene una arista por su ID.
     */
    public EdgeRecord getEdge(String edgeId) {
        return edges.get(edgeId);
    }

    /**
     * Obtiene la arista entre dos nodos (si existe).
     */
    public EdgeRecord getEdgeBetween(String nodeA, String nodeB) {
        String edgeId = EdgeRecord.generateId(nodeA, nodeB);
        return edges.get(edgeId);
    }

    /**
     * @return lista inmutable de todas las aristas
     */
    public List<EdgeRecord> getAllEdges() {
        return List.copyOf(edges.values());
    }

    /**
     * Obtiene los vecinos de un nodo.
     *
     * @param nodeId ID del nodo
     * @return Set inmutable de IDs de vecinos
     */
    public Set<String> getNeighbors(String nodeId) {
        return Collections.unmodifiableSet(
            adjacencyList.getOrDefault(nodeId, Set.of())
        );
    }

    /**
     * Obtiene los vecinos ACTIVOS de un nodo (excluyendo cuarentena
     * y aristas desactivadas).
     *
     * @param nodeId ID del nodo
     * @return lista de IDs de vecinos activos
     */
    public List<String> getActiveNeighbors(String nodeId) {
        Set<String> allNeighbors = adjacencyList.getOrDefault(nodeId, Set.of());

        return allNeighbors.stream()
            .filter(neighborId -> {
                // Verificar que la arista no esté desactivada
                String edgeId = EdgeRecord.generateId(nodeId, neighborId);
                if (simulationState.isEdgeDisabled(edgeId)) return false;

                // Verificar que el vecino no esté en cuarentena
                NodeData neighbor = nodes.get(neighborId);
                return neighbor != null && neighbor.isActive();
            })
            .collect(Collectors.toList());
    }


    //alterna el estado de una arista como cortafuegos

    public boolean toggleEdgeFirewall(String sourceId, String targetId) {
        String edgeId = EdgeRecord.generateId(sourceId, targetId);
        if (!edges.containsKey(edgeId)) return false;

        if (simulationState.isEdgeDisabled(edgeId)) {
            simulationState.enableEdge(edgeId);
            return false;
        } else {
            simulationState.disableEdge(edgeId);
            return true;
        }
    }


    //----Generacion de topologias
    public TopologyResult generateTopology(TopologyType type, int nodeCount,
                                           double canvasW, double canvasH) {
        // Limpiar todo
        clearGraph();

        // Generar la topología
        TopologyResult result = topologyGenerator.generate(type, nodeCount, canvasW, canvasH);

        // Cargar nodos
        for (NodeInfo info : result.nodes()) {
            createNode(info.id(), info.label(), info.x(), info.y());
        }

        // Cargar aristas
        for (EdgeInfo info : result.edges()) {
            addEdge(info.sourceId(), info.targetId());
        }

        // Marcar el hub sugerido
        if (result.suggestedHubId() != null) {
            NodeData hub = getNode(result.suggestedHubId());
            if (hub != null) {
                hub.setSuperSpreader(true);
            }
        }

        setModified(true);

        System.out.println("[GraphModel] Topología generada: " + type
            + " (" + result.nodes().size() + " nodos, "
            + result.edges().size() + " aristas)");

        return result;
    }

    //Analisis del grafo


    //Identifica el nodo, el que mas propaga

    public NodeData findSuperSpreader() {
        if (nodes.isEmpty()) return null;

        NodeData superSpreader = null;
        int maxDegree = -1;

        for (NodeData node : nodes.values()) {
            int degree = adjacencyList.getOrDefault(node.getId(), Set.of()).size();
            if (degree > maxDegree) {
                maxDegree = degree;
                superSpreader = node;
            }
        }

        // Marcar visualmente
        if (superSpreader != null) {
            // Limpiar marca anterior
            nodes.values().forEach(n -> n.setSuperSpreader(false));
            superSpreader.setSuperSpreader(true);
        }

        return superSpreader;
    }


    //Retorna ranking de centralidad de grado

    public List<NodeData> getDegreeCentralityRanking(int topN) {
        return nodes.values().stream()
            .sorted((a, b) -> Integer.compare(
                adjacencyList.getOrDefault(b.getId(), Set.of()).size(),
                adjacencyList.getOrDefault(a.getId(), Set.of()).size()
            ))
            .limit(topN)
            .collect(Collectors.toList());
    }

    //calcula el grado promedio de la red

    public double getAverageDegree(){
        if(nodes.isEmpty()) return 0.0;
        int totalDegree = adjacencyList.values().stream().mapToInt(Set::size).sum();
        return (double) totalDegree / nodes.size();
    }

    // ═══════════════════════════════════════════════════════════════
    // MOTOR BFS — PRE-COMPUTACIÓN COMPLETA
    // ═══════════════════════════════════════════════════════════════


    public boolean precomputeSimulation() {
        // Validaciones
        if (nodes.isEmpty()) {
            System.out.println("[BFS] Error: No hay nodos en el grafo.");
            return false;
        }

        List<String> patientZeros = simulationState.getPatientZeroIds();
        if (patientZeros.isEmpty()) {
            System.out.println("[BFS] Error: No se seleccionaron pacientes cero.");
            return false;
        }

        // Verificar que los pacientes cero existen
        for (String pzId : patientZeros) {
            if (!nodes.containsKey(pzId)) {
                System.out.println("[BFS] Error: Paciente cero no encontrado: " + pzId);
                return false;
            }
        }

        System.out.println("══════════════════════════════════════════");
        System.out.println("  [BFS] Iniciando pre-computación...");
        System.out.println("  Pacientes cero: " + patientZeros);
        System.out.println("  Tasa de contagio: " + (simulationState.getContagionRate() * 100) + "%");
        System.out.println("  Tiempo de recuperación: " + simulationState.getRecoveryTime() + " iter.");
        System.out.println("══════════════════════════════════════════");

        // Limpiar simulación anterior (mantener parámetros)
        simulationState.softReset();
        resetAllNodeStates();

        // Aplicar cuarentenas previamente configuradas
        for (String qId : simulationState.getQuarantinedNodeIds()) {
            NodeData qNode = nodes.get(qId);
            if (qNode != null) qNode.quarantine();
        }

        // ── INICIALIZACIÓN: Cola BFS con pacientes cero ──────────
        Deque<String> bfsQueue = new ArrayDeque<>();
        int iterationCount = 0;

        // Infectar pacientes cero
        List<PropagationEvent> initEvents = new ArrayList<>();
        for (String pzId : patientZeros) {
            NodeData pzNode = nodes.get(pzId);
            if (pzNode != null && pzNode.getStatus() != NodeStatus.QUARANTINED) {
                pzNode.infect(null, 0, 0, true);
                bfsQueue.add(pzId);
                initEvents.add(new PropagationEvent(null, pzId, 0, 0, false));
            }
        }

        // Snapshot de la iteración 0 (solo pacientes cero)
        simulationState.recordIteration(createSnapshot(iterationCount, bfsQueue, initEvents));

        // ── BUCLE PRINCIPAL BFS ───────────────────────────────────
        int maxIterations = nodes.size() * 3; // Safeguard contra loops infinitos

        while (!bfsQueue.isEmpty() && iterationCount < maxIterations) {
            iterationCount++;
            List<PropagationEvent> events = new ArrayList<>();
            Deque<String> nextQueue = new ArrayDeque<>();

            // Nodos a procesar en ESTE nivel del BFS
            int levelSize = bfsQueue.size();

            for (int i = 0; i < levelSize; i++) {
                String currentId = bfsQueue.poll();
                NodeData currentNode = nodes.get(currentId);

                if (currentNode == null || !currentNode.canSpread()) continue;

                // Explorar vecinos activos
                List<String> activeNeighbors = getActiveNeighbors(currentId);

                for (String neighborId : activeNeighbors) {
                    NodeData neighbor = nodes.get(neighborId);
                    if (neighbor == null) continue;

                    if (neighbor.canBeInfected()) {
                        // ── Propagación Probabilística ────────────
                        boolean willInfect = random.nextDouble() < simulationState.getContagionRate();

                        if (willInfect) {
                            // ¡Infección exitosa!
                            neighbor.infect(currentId,
                                currentNode.getDepthLevel() + 1,
                                iterationCount);
                            currentNode.addInfectedNode(neighborId);
                            nextQueue.add(neighborId);

                            events.add(new PropagationEvent(
                                currentId, neighborId,
                                neighbor.getDepthLevel(),
                                iterationCount, false
                            ));
                        } else {
                            // Infección bloqueada por probabilidad
                            events.add(new PropagationEvent(
                                currentId, neighborId,
                                currentNode.getDepthLevel() + 1,
                                iterationCount, true
                            ));
                        }
                    }
                }
            }

            // ── Tick de infección + recuperación SIR ──────────────
            List<String> toRecover = new ArrayList<>();
            for (NodeData node : nodes.values()) {
                if (node.canSpread()) {
                    node.tickInfection();

                    // ¿Ya cumplió su tiempo de infección?
                    if (node.getInfectedDuration() >= simulationState.getRecoveryTime()) {
                        toRecover.add(node.getId());
                    }
                }
            }

            // Recuperar nodos
            for (String recoverId : toRecover) {
                NodeData recoverNode = nodes.get(recoverId);
                if (recoverNode != null) {
                    recoverNode.recover(iterationCount);
                }
            }

            // Re-agregar nodos que siguen infectados y tienen vecinos susceptibles
            for (NodeData node : nodes.values()) {
                if (node.canSpread() && !nextQueue.contains(node.getId())) {
                    List<String> susceptibleNeighbors = getActiveNeighbors(node.getId()).stream()
                        .filter(nId -> {
                            NodeData n = nodes.get(nId);
                            return n != null && n.canBeInfected();
                        })
                        .collect(Collectors.toList());

                    if (!susceptibleNeighbors.isEmpty()) {
                        nextQueue.add(node.getId());
                    }
                }
            }

            // Guardar snapshot de esta iteración
            bfsQueue = nextQueue;
            simulationState.recordIteration(
                createSnapshot(iterationCount, bfsQueue, events)
            );

            // Si no hay más eventos y la cola está vacía, terminar
            if (nextQueue.isEmpty() && events.stream().noneMatch(e -> !e.wasBlocked())) {
                break;
            }
        }

        // ── Finalizar ─��───────────────────────────────────────────
        simulationState.setPrecomputed(true);
        simulationState.finish();
        // Posicionar al inicio para reproducción
        simulationState.goToIteration(0);

        System.out.println("══════════════════════════════════════════");
        System.out.println("  [BFS] Pre-computación completada.");
        System.out.println("  Total iteraciones: " + simulationState.getTotalIterations());
        System.out.println("══════════════════════════════════════════");

        return true;
    }

    /**
     * Crea un snapshot completo del estado actual de todos los nodos.
     */
    private IterationSnapshot createSnapshot(int iteration, Deque<String> bfsQueue,
                                             List<PropagationEvent> events) {
        // Snapshot de cada nodo
        List<NodeSnapshotRecord> nodeSnapshots = new ArrayList<>();
        for (NodeData node : nodes.values()) {
            nodeSnapshots.add(new NodeSnapshotRecord(
                node.getId(),
                node.getLabel(),
                node.getStatus(),
                node.getDepthLevel(),
                node.getInfectedBy(),
                node.getInfectedDuration()
            ));
        }

        // Estado de la cola BFS
        List<String> queueState = new ArrayList<>(bfsQueue);

        // Estadísticas
        IterationStats stats = IterationStats.fromNodeSnapshots(nodeSnapshots);

        return new IterationSnapshot(iteration, nodeSnapshots, queueState, events, stats);
    }




    //-----------Aplicacion de snapshots

    /*Aplica un snapshot a los nodos del grafo.
     * Restaura el estado de cada nodo al que tenía en esa iteración.
     * Usado por los controles de reproducción (play, paso, retroceso). */

     public void applySnapshot(IterationSnapshot snapshot) {
        if (snapshot == null) return;

        for (NodeSnapshotRecord ns : snapshot.nodeSnapshots()) {
            NodeData node = nodes.get(ns.nodeId());
            if (node != null) {
                node.setStatus(ns.status());
                node.setDepthLevel(ns.depthLevel());
                node.setInfectedBy(ns.infectedBy());
                node.setInfectedDuration(ns.infectedDuration());
            }
        }
    }

    //Avanza la simulacion un paso y aplica el snapshot resultante


    public IterationSnapshot stepForwardAndApply() {
        IterationSnapshot snapshot = simulationState.stepForward();
        if (snapshot != null) {
            applySnapshot(snapshot);
        }
        return snapshot;
    }


    /**
     * Retrocede la simulación un paso y aplica el snapshot resultante.
     *
     * @return el snapshot aplicado, o null si ya está al inicio
     */
    public IterationSnapshot stepBackwardAndApply() {
        IterationSnapshot snapshot = simulationState.stepBackward();
        if (snapshot != null) {
            applySnapshot(snapshot);
        }
        return snapshot;
    }

    /**
     * Salta a una iteración específica y aplica su snapshot.
     *
     * @param index índice de la iteración
     * @return el snapshot aplicado, o null si el índice es inválido
     */
    public IterationSnapshot goToIterationAndApply(int index) {
        IterationSnapshot snapshot = simulationState.goToIteration(index);
        if (snapshot != null) {
            applySnapshot(snapshot);
        }
        return snapshot;
    }



    //Gestion de modos de interaccion


    /*alterna un nodo como paciente cero */

    public boolean togglePatientZero(String nodeId) {
        NodeData node = nodes.get(nodeId);
        if (node == null) return false;

        boolean isNowPZ = simulationState.togglePatientZero(nodeId);

        if (isNowPZ) {
            node.setStatus(NodeStatus.PATIENT_ZERO);
        } else {
            node.setStatus(NodeStatus.SUSCEPTIBLE);
        }

        return isNowPZ;
    }

    //Alterna un nodo como en cuarentena

    public boolean toggleQuarantine(String nodeId) {
        NodeData node = nodes.get(nodeId);
        if (node == null) return false;

        boolean isNowQ = simulationState.toggleQuarantine(nodeId);

        if (isNowQ) {
            node.quarantine();
        } else {
            node.unquarantine();
        }

        return isNowQ;
    }


    //Selecciona un nodo


    public void selectNode(String nodeId) {
        // Deseleccionar anterior
        NodeData current = getSelectedNode();
        if (current != null) {
            current.setSelected(false);
        }

        if (nodeId == null) {
            setSelectedNode(null);
            return;
        }

        NodeData node = nodes.get(nodeId);
        if (node != null) {
            node.setSelected(true);
            setSelectedNode(node);
        }
    }


    //Limpieza y reset

    //Limpia el grafo completamente

      public void clearGraph() {
        nodes.clear();
        adjacencyList.clear();
        edges.clear();
        nodeIds.clear();
        edgeIds.clear();
        simulationState.reset();
        setSelectedNode(null);
        setEdgeSourceNode(null);
        NodeData.resetIdCounter();
        updateCounts();
        setModified(false);

        System.out.println("[GraphModel] Grafo limpiado completamente.");
    }

    /**
     * Resetea solo los estados de los nodos (mantiene estructura del grafo).
     * Usado para re-ejecutar la simulación con la misma red.
     */
    public void resetAllNodeStates() {
        for (NodeData node : nodes.values()) {
            node.reset();
        }
    }

    /**
     * Resetea la simulación pero mantiene el grafo y los parámetros.
     */
    public void resetSimulation() {
        resetAllNodeStates();
        simulationState.softReset();

        // Re-aplicar pacientes cero
        for (String pzId : simulationState.getPatientZeroIds()) {
            NodeData node = nodes.get(pzId);
            if (node != null) {
                node.setStatus(NodeStatus.PATIENT_ZERO);
            }
        }

        // Re-aplicar cuarentenas
        for (String qId : simulationState.getQuarantinedNodeIds()) {
            NodeData node = nodes.get(qId);
            if (node != null) {
                node.quarantine();
            }
        }

        System.out.println("[GraphModel] Simulación reseteada (grafo intacto).");
    }



    //UTILIDADES INTERNAS

    /**
     * Actualiza el grado de un nodo a partir de la lista de adyacencia.
     */
    private void updateNodeDegree(String nodeId) {
        NodeData node = nodes.get(nodeId);
        if (node != null) {
            node.setDegree(adjacencyList.getOrDefault(nodeId, Set.of()).size());
        }
    }

    /**
     * Actualiza los conteos observables.
     */
    private void updateCounts() {
        setNodeCount(nodes.size());
        setEdgeCount(edges.size());
        setEmpty(nodes.isEmpty());
    }



    //---------GETTERS - SETTERS

    // ── SimulationState (acceso directo) ──────────────────────────
    public SimulationState getSimulationState() { return simulationState; }

    // ── Node Count ────────────────────────────────────────────────
    public int getNodeCount()                         { return nodeCount.get(); }
    private void setNodeCount(int v)                  { nodeCount.set(v); }
    public IntegerProperty nodeCountProperty()        { return nodeCount; }

    // ── Edge Count ────────────────────────────────────────────────
    public int getEdgeCount()                         { return edgeCount.get(); }
    private void setEdgeCount(int v)                  { edgeCount.set(v); }
    public IntegerProperty edgeCountProperty()        { return edgeCount; }

    // ── Modified ──────────────────────────────────────────────────
    public boolean isModified()                       { return modified.get(); }
    public void setModified(boolean v)                { modified.set(v); }
    public BooleanProperty modifiedProperty()         { return modified; }

    // ── Empty ─────────────────────────────────────────────────────
    public boolean isEmpty()                          { return empty.get(); }
    private void setEmpty(boolean v)                  { empty.set(v); }
    public BooleanProperty emptyProperty()            { return empty; }

    // ── Selected Node ─────────────────────────────────────────────
    public NodeData getSelectedNode()                           { return selectedNode.get(); }
    public void setSelectedNode(NodeData node)                  { selectedNode.set(node); }
    public ObjectProperty<NodeData> selectedNodeProperty()      { return selectedNode; }

    // ── Edge Source Node ──────────────────────────────────────────
    public NodeData getEdgeSourceNode()                         { return edgeSourceNode.get(); }
    public void setEdgeSourceNode(NodeData node)                { edgeSourceNode.set(node); }
    public ObjectProperty<NodeData> edgeSourceNodeProperty()    { return edgeSourceNode; }

    // ── Edge Creation Mode ────────────────────────────────────────
    public boolean isEdgeCreationMode()                    { return edgeCreationMode.get(); }
    public void setEdgeCreationMode(boolean v)             { edgeCreationMode.set(v); }
    public BooleanProperty edgeCreationModeProperty()      { return edgeCreationMode; }

    // ── Quarantine Mode ───────────────────────────────────────────
    public boolean isQuarantineMode()                      { return quarantineMode.get(); }
    public void setQuarantineMode(boolean v)               { quarantineMode.set(v); }
    public BooleanProperty quarantineModeProperty()        { return quarantineMode; }

    // ── Patient Zero Mode ─────────────────────────────────────────
    public boolean isPatientZeroMode()                     { return patientZeroMode.get(); }
    public void setPatientZeroMode(boolean v)              { patientZeroMode.set(v); }
    public BooleanProperty patientZeroModeProperty()       { return patientZeroMode; }

    // ── Observable Lists ──────────────────────────────────────────
    public ObservableList<String> getNodeIds() { return nodeIds; }
    public ObservableList<String> getEdgeIds() { return edgeIds; }

    // ═══════════════════════════════════════════════════════════════
    //  toString
    // ═══════════════════════════════════════════════════════════════

    @Override
    public String toString() {
        return String.format(
            "GraphModel{ nodes=%d, edges=%d, avgDegree=%.1f, simulation=%s }",
            getNodeCount(), getEdgeCount(), getAverageDegree(), simulationState
        );
    }

}
