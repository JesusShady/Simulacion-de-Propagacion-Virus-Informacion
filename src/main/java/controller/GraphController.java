package controller;

import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import model.GraphModel;
import model.GraphModel.EdgeRecord;
import model.NodeData;
import model.enums.NodeStatus;
import view.GraphCanvasView;
import view.MainView;

import java.util.Set;

public class GraphController {

    // ═══════════════════════════════════════════════════════════════
    //  CONSTANTES
    // ═══════════════════════════════════════════════════════════════

    /** Distancia mínima para considerar un drag (evitar micro-movimientos) */
    private static final double DRAG_THRESHOLD = 5.0;

    /** Distancia máxima para detectar clic en una arista */
    private static final double EDGE_HIT_DISTANCE = 10.0;

    // ═══════════════════════════════════════════════════════════════
    //  REFERENCIAS
    // ═══════════════════════════════════════════════════════════════

    private final GraphModel     model;
    private final MainView       view;
    private final GraphCanvasView canvasView;

    /** Referencia al controlador padre para consultar el modo actual */
    private MainController mainController;

    // ═══════════════════════════════════════════════════════════════
    //  ESTADO DE INTERACCIÓN
    // ═══════════════════════════════════════════════════════════════

    /** Posición del mouse cuando se hizo clic (para detectar drags) */
    private double pressX, pressY;

    /** ¿Se superó el umbral de drag? */
    private boolean dragStarted;

    /** ¿Hubo un evento de press (para distinguir click de drag)? */
    private boolean mouseWasPressed;

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public GraphController(GraphModel model, MainView view) {
        this.model      = model;
        this.view       = view;
        this.canvasView = view.getCanvasView();
    }

    // ═════════════════════════════════════════════════��═════════════
    //  INICIALIZACIÓN — Registrar eventos del mouse
    // ═══════════════════════════════════════════════════════════════

    /**
     * Registra todos los event handlers del mouse sobre el canvas.
     * Llamado desde MainController.initialize().
     */
    public void initialize() {
        Canvas canvas = canvasView.getCanvas();

        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);
        canvas.setOnMouseReleased(this::onMouseReleased);
        canvas.setOnMouseMoved(this::onMouseMoved);

        System.out.println("[GraphController] ✓ Event handlers registrados en el canvas.");
    }

    /**
     * Establece la referencia al controlador principal.
     * Necesario para consultar el modo de interacción actual.
     *
     * @param controller el MainController
     */
    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    // ═══════════════════════════════════════════════════════════════
    //  MOUSE PRESSED
    // ═══════════════════════════════════════════════════════════════

    private void onMousePressed(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        pressX = x;
        pressY = y;
        dragStarted = false;
        mouseWasPressed = true;

        // ── Clic derecho: Eliminar ────────────────────────────────
        if (event.getButton() == MouseButton.SECONDARY) {
            handleRightClick(x, y);
            return;
        }

        // Solo procesar clic izquierdo
        if (event.getButton() != MouseButton.PRIMARY) return;

        // Buscar nodo bajo el cursor
        NodeData clickedNode = model.findNodeAt(x, y);

        // ── Despachar según el modo actual ────────────────────────
        if (isCreateNodeMode()) {
            handleCreateNodeClick(x, y, clickedNode);
        } else if (model.isEdgeCreationMode()) {
            handleEdgeStartPress(clickedNode);
        } else if (model.isPatientZeroMode()) {
            handlePatientZeroClick(clickedNode);
        } else if (model.isQuarantineMode()) {
            handleQuarantineClick(clickedNode);
        } else if (isFirewallMode()) {
            handleFirewallClick(x, y);
        } else {
            // Modo normal: seleccionar o iniciar drag
            handleNormalPress(x, y, clickedNode);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  MOUSE DRAGGED
    // ═══════════════════════════════════════════════════════════════

    private void onMouseDragged(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;

        double x = event.getX();
        double y = event.getY();

        // Actualizar posición del mouse en el canvas view
        canvasView.updateMousePosition(x, y);

        // Verificar si se superó el umbral de drag
        if (!dragStarted) {
            double dx = x - pressX;
            double dy = y - pressY;
            if (Math.sqrt(dx * dx + dy * dy) < DRAG_THRESHOLD) {
                return; // Aún no es un drag
            }
            dragStarted = true;
        }

        // ── Modo crear arista: dibujar preview ────────────────────
        if (canvasView.isDrawingEdge()) {
            // La línea de preview se dibuja automáticamente en el render loop
            // porque canvasView ya tiene las coordenadas del mouse
            return;
        }

        // ── Modo normal: arrastrar nodo ───────────────────────────
        if (canvasView.isDragging()) {
            // Clamp a los límites del canvas
            double clampedX = Math.max(20, Math.min(x, canvasView.getCanvasWidth() - 20));
            double clampedY = Math.max(20, Math.min(y, canvasView.getCanvasHeight() - 20));
            canvasView.updateDrag(clampedX, clampedY);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  MOUSE RELEASED
    // ═══════════════════════════════════════════════════════════════

    private void onMouseReleased(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;

        double x = event.getX();
        double y = event.getY();

        // ── Finalizar creación de arista ───────────────────────────
        if (canvasView.isDrawingEdge()) {
            handleEdgeEndRelease(x, y);
        }

        // ── Finalizar drag de nodo ────────────────────────────────
        if (canvasView.isDragging()) {
            canvasView.endDrag();
            updateCanvasCursor(x, y);
        }

        mouseWasPressed = false;
        dragStarted = false;
    }

    // ═══════════════════════════════════════════════════════════════
    //  MOUSE MOVED (hover)
    // ═══════════════════════════════════════════════════════════════

    private void onMouseMoved(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        canvasView.updateMousePosition(x, y);
        updateCanvasCursor(x, y);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HANDLERS ESPECÍFICOS POR MODO
    // ═══════════════════════════════════════════════════════════════

    // ── CREAR NODO ────────────────────────────────────────────────

    /**
     * Crea un nodo en la posición del clic.
     * Si se hizo clic sobre un nodo existente, lo selecciona en vez de crear uno nuevo.
     */
    private void handleCreateNodeClick(double x, double y, NodeData clickedNode) {
        if (clickedNode != null) {
            // Clic sobre nodo existente → seleccionar
            model.selectNode(clickedNode.getId());
            System.out.println("[GraphController] Nodo seleccionado: " + clickedNode.getId());
        } else {
            // Clic en espacio vacío → crear nodo
            NodeData newNode = model.createNode(x, y);
            model.selectNode(newNode.getId());
            System.out.println("[GraphController] Nodo creado: " + newNode.getId()
                + " en (" + x + ", " + y + ")");
        }
    }

    // ── CREAR ARISTA (Press) ──────────────────────────────────────

    /**
     * Inicia la creación de una arista desde un nodo.
     */
    private void handleEdgeStartPress(NodeData sourceNode) {
        if (sourceNode != null) {
            canvasView.startEdgeDrawing(sourceNode);
            model.setEdgeSourceNode(sourceNode);
            System.out.println("[GraphController] Arista iniciada desde: " + sourceNode.getId());
        }
    }

    // ── CREAR ARISTA (Release) ────────────────────────────────────

    /**
     * Finaliza la creación de una arista.
     * Si el mouse se soltó sobre otro nodo, crea la arista.
     */
    private void handleEdgeEndRelease(double x, double y) {
        NodeData startNode = canvasView.getEdgeStartNode();
        NodeData endNode   = model.findNodeAt(x, y);

        canvasView.endEdgeDrawing();
        model.setEdgeSourceNode(null);

        if (startNode != null && endNode != null && !startNode.equals(endNode)) {
            boolean created = model.addEdge(startNode.getId(), endNode.getId());

            if (created) {
                System.out.println("[GraphController] Arista creada: "
                    + startNode.getId() + " ↔ " + endNode.getId());
            } else {
                System.out.println("[GraphController] Arista ya existía: "
                    + startNode.getId() + " ↔ " + endNode.getId());
            }
        } else {
            System.out.println("[GraphController] Creación de arista cancelada.");
        }
    }

    // ── PACIENTE CERO ─────────────────────────────────────────────

    /**
     * Toggle de paciente cero en un nodo.
     */
    private void handlePatientZeroClick(NodeData node) {
        if (node == null) return;

        // No permitir marcar nodos en cuarentena como paciente cero
        if (node.getStatus() == NodeStatus.QUARANTINED) {
            System.out.println("[GraphController] No se puede marcar como P0: "
                + "nodo en cuarentena.");
            return;
        }

        boolean isNowPZ = model.togglePatientZero(node.getId());

        System.out.println("[GraphController] " + node.getId()
            + (isNowPZ ? " → PACIENTE CERO ☣" : " → SUSCEPTIBLE ⬡"));

        // Seleccionar para mostrar info
        model.selectNode(node.getId());
    }

    // ── CUARENTENA ────────────────────────────────────────────────

    /**
     * Toggle de cuarentena en un nodo.
     */
    private void handleQuarantineClick(NodeData node) {
        if (node == null) return;

        // No permitir poner en cuarentena nodos que son paciente cero
        if (node.getStatus() == NodeStatus.PATIENT_ZERO) {
            System.out.println("[GraphController] No se puede poner en cuarentena: "
                + "nodo es paciente cero.");
            return;
        }

        boolean isNowQ = model.toggleQuarantine(node.getId());

        System.out.println("[GraphController] " + node.getId()
            + (isNowQ ? " → CUARENTENA ⊘" : " → SUSCEPTIBLE ⬡"));

        // Seleccionar para mostrar info
        model.selectNode(node.getId());
    }

    // ── CORTAFUEGOS ───────────────────────────────────────────────

    /**
     * Toggle de cortafuegos en la arista más cercana al punto de clic.
     */
    private void handleFirewallClick(double x, double y) {
        // Buscar la arista más cercana al punto de clic
        EdgeRecord closestEdge = findClosestEdge(x, y);

        if (closestEdge != null) {
            boolean isNowDisabled = model.toggleEdgeFirewall(
                closestEdge.sourceId(), closestEdge.targetId()
            );

            System.out.println("[GraphController] Arista " + closestEdge.id()
                + (isNowDisabled ? " → CORTAFUEGOS 🔥" : " → ACTIVA ✓"));
        } else {
            System.out.println("[GraphController] No se encontró arista cercana al clic.");
        }
    }

    /**
     * Encuentra la arista más cercana a un punto (x, y).
     * Usa la distancia punto-a-segmento para determinar proximidad.
     *
     * @param x coordenada X del clic
     * @param y coordenada Y del clic
     * @return la EdgeRecord más cercana, o null si ninguna está dentro del umbral
     */
    private EdgeRecord findClosestEdge(double x, double y) {
        EdgeRecord closest = null;
        double minDistance = EDGE_HIT_DISTANCE;

        for (EdgeRecord edge : model.getAllEdges()) {
            NodeData source = model.getNode(edge.sourceId());
            NodeData target = model.getNode(edge.targetId());
            if (source == null || target == null) continue;

            double dist = pointToSegmentDistance(
                x, y,
                source.getPosX(), source.getPosY(),
                target.getPosX(), target.getPosY()
            );

            if (dist < minDistance) {
                minDistance = dist;
                closest = edge;
            }
        }

        return closest;
    }

    /**
     * Calcula la distancia mínima desde un punto P a un segmento AB.
     *
     *  Fórmula:
     *    1. Proyectar P sobre la recta AB → punto Q
     *    2. Si Q está dentro del segmento → distancia = |PQ|
     *    3. Si Q está fuera → distancia = min(|PA|, |PB|)
     *
     * @return distancia en píxeles
     */
    private double pointToSegmentDistance(double px, double py,
                                          double ax, double ay,
                                          double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double lengthSq = dx * dx + dy * dy;

        if (lengthSq == 0) {
            // A y B son el mismo punto
            return Math.sqrt((px - ax) * (px - ax) + (py - ay) * (py - ay));
        }

        // Parámetro t de la proyección de P sobre AB
        double t = ((px - ax) * dx + (py - ay) * dy) / lengthSq;
        t = Math.max(0, Math.min(1, t)); // Clamp al segmento

        // Punto más cercano en el segmento
        double closestX = ax + t * dx;
        double closestY = ay + t * dy;

        // Distancia P → punto más cercano
        double distX = px - closestX;
        double distY = py - closestY;
        return Math.sqrt(distX * distX + distY * distY);
    }

    // ── MODO NORMAL (Sin modo especial) ───────────────────────────

    /**
     * Maneja clic en modo normal: seleccionar nodo o iniciar drag.
     */
    private void handleNormalPress(double x, double y, NodeData clickedNode) {
        if (clickedNode != null) {
            // Seleccionar el nodo
            model.selectNode(clickedNode.getId());

            // Preparar para drag
            canvasView.startDrag(clickedNode);

            System.out.println("[GraphController] Nodo seleccionado: " + clickedNode.getId());
        } else {
            // Clic en espacio vacío → deseleccionar
            model.selectNode(null);
        }
    }

    // ── CLIC DERECHO (Eliminar) ───────────────────────────────────

    /**
     * Maneja clic derecho: elimina nodo o arista.
     */
    private void handleRightClick(double x, double y) {
        // Primero buscar nodo
        NodeData clickedNode = model.findNodeAt(x, y);

        if (clickedNode != null) {
            String nodeId = clickedNode.getId();
            System.out.println("[GraphController] Eliminando nodo: " + nodeId);
            model.removeNode(nodeId);
            return;
        }

        // Si no hay nodo, buscar arista
        EdgeRecord closestEdge = findClosestEdge(x, y);
        if (closestEdge != null) {
            System.out.println("[GraphController] Eliminando arista: " + closestEdge.id());
            model.removeEdge(closestEdge.sourceId(), closestEdge.targetId());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  CURSOR DINÁMICO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualiza el cursor del canvas según el contexto.
     */
    private void updateCanvasCursor(double x, double y) {
        Canvas canvas = canvasView.getCanvas();
        NodeData hovered = model.findNodeAt(x, y);

        if (isCreateNodeMode()) {
            // Modo crear nodo
            if (hovered != null) {
                canvas.setCursor(Cursor.HAND);       // Sobre un nodo existente
            } else {
                canvas.setCursor(Cursor.CROSSHAIR);  // Espacio vacío → crear
            }
        } else if (model.isEdgeCreationMode()) {
            // Modo crear arista
            if (hovered != null) {
                canvas.setCursor(Cursor.CROSSHAIR);  // Sobre un nodo → iniciar/terminar arista
            } else {
                canvas.setCursor(Cursor.DEFAULT);
            }
        } else if (model.isPatientZeroMode() || model.isQuarantineMode()) {
            // Modos de marcado
            if (hovered != null) {
                canvas.setCursor(Cursor.HAND);
            } else {
                canvas.setCursor(Cursor.DEFAULT);
            }
        } else if (isFirewallMode()) {
            // Modo cortafuegos
            EdgeRecord closestEdge = findClosestEdge(x, y);
            if (closestEdge != null) {
                canvas.setCursor(Cursor.HAND);
            } else {
                canvas.setCursor(Cursor.DEFAULT);
            }
        } else {
            // Modo normal
            if (hovered != null) {
                if (canvasView.isDragging()) {
                    canvas.setCursor(Cursor.CLOSED_HAND);
                } else {
                    canvas.setCursor(Cursor.OPEN_HAND);
                }
            } else {
                canvas.setCursor(Cursor.DEFAULT);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILIDADES — Consulta de modo al MainController
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica si estamos en modo "crear nodo".
     * Consulta al MainController ya que él gestiona los modos.
     */
    private boolean isCreateNodeMode() {
        return mainController != null && mainController.isCreateNodeMode();
    }

    /**
     * Verifica si estamos en modo "cortafuegos".
     */
    private boolean isFirewallMode() {
        return mainController != null && mainController.isFirewallMode();
    }
}