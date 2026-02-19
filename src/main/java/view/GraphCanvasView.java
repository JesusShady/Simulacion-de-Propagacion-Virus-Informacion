package view;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import model.GraphModel;
import model.GraphModel.EdgeRecord;
import model.NodeData;
import model.enums.NodeStatus;

import java.util.List;
import java.util.Set;

public class GraphCanvasView {

    // ═══════════════════════════════════════════════════════════════
    //  CONSTANTES VISUALES
    // ═══════════════════════════════════════════════════════════════

    // Nodos
    private static final double NODE_RADIUS           = 18.0;
    private static final double NODE_RADIUS_HOVER     = 22.0;
    private static final double NODE_RADIUS_SELECTED  = 24.0;
    private static final double GLOW_RADIUS_FACTOR    = 2.2;
    private static final double LABEL_OFFSET_Y        = 28.0;
    private static final double HIT_TOLERANCE         = 6.0;

    // Aristas
    private static final double EDGE_WIDTH_NORMAL     = 2.0;
    private static final double EDGE_WIDTH_DISABLED   = 1.0;
    private static final double EDGE_DASH_LENGTH      = 8.0;
    private static final double EDGE_DASH_GAP         = 6.0;

    // Grid de fondo
    private static final double GRID_SPACING          = 40.0;
    private static final double GRID_OPACITY          = 0.04;

    // Animación
    private static final double PULSE_SPEED           = 2.5;
    private static final double PARTICLE_SPEED        = 1.8;

    // Colores del canvas
    private static final Color  BG_COLOR       = Color.web(MainView.BG_CANVAS);
    private static final Color  GRID_COLOR     = Color.web(MainView.TEXT_MUTED, GRID_OPACITY);
    private static final Color  EDGE_DEFAULT   = Color.web("#30363D", 0.7);
    private static final Color  EDGE_DISABLED  = Color.web("#FF1744", 0.3);
    private static final Color  DRAG_LINE      = Color.web(MainView.ACCENT_CYAN, 0.6);

    // ═══════════════════════════════════════════════════════════════
    //  COMPONENTES
    // ═══════════════════════════════════════════════════════════════

    /** Contenedor raíz del canvas */
    private final StackPane root;

    /** Canvas principal de renderizado */
    private final Canvas canvas;

    /** Contexto gráfico del canvas */
    private final GraphicsContext gc;

    /** Referencia al modelo */
    private final GraphModel model;

    /** Loop de renderizado a 60 FPS */
    private final AnimationTimer renderLoop;

    /** Tiempo para animaciones */
    private double animTime = 0;

    /** ¿El render loop está activo? */
    private boolean rendering = false;

    // ── Estado de interacción ─────────────────────────────────────

    /** Coordenadas del mouse */
    private double mouseX = 0, mouseY = 0;

    /** ¿Se está arrastrando un nodo? */
    private boolean dragging = false;

    /** ¿Se está creando una arista? (arrastrando desde un nodo) */
    private boolean drawingEdge = false;

    /** Nodo que se está arrastrando */
    private NodeData draggedNode = null;

    /** Nodo origen de la arista que se está creando */
    private NodeData edgeStartNode = null;

    /** Nodo sobre el que está el mouse (hover) */
    private NodeData hoveredNode = null;

    // ═══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public GraphCanvasView(GraphModel model) {
        this.model = model;

        // Crear canvas dentro de un Pane resizable
        canvas = new Canvas(800, 600);
        gc     = canvas.getGraphicsContext2D();

        // Contenedor que permite que el canvas se redimensione
        Pane canvasContainer = new Pane(canvas);
        canvasContainer.setStyle(String.format(
            "-fx-background-color: %s;", MainView.BG_CANVAS
        ));

        root = new StackPane(canvasContainer);
        root.setPadding(Insets.EMPTY);
        root.setStyle(String.format(
            "-fx-background-color: %s;", MainView.BG_CANVAS
        ));

        // Hacer que el canvas se redimensione con su contenedor
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());

        // Re-renderizar cuando cambie el tamaño
        canvas.widthProperty().addListener((obs, o, n) -> render());
        canvas.heightProperty().addListener((obs, o, n) -> render());

        // ── Configurar el render loop (60 FPS) ───────────────────
        renderLoop = new AnimationTimer() {
            private long lastFrame = 0;
            @Override
            public void handle(long now) {
                // Limitar a ~60 FPS
                if (now - lastFrame < 16_666_666L) return; // 16.67ms
                lastFrame = now;
                animTime += 0.016; // ~deltaTime en segundos
                render();
            }
        };

        // Iniciar renderizado
        startRendering();
    }

    // ═══════════════════════════════════════════════════════════════
    //  RENDER LOOP PRINCIPAL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Método principal de renderizado.
     * Se llama a 60 FPS y dibuja TODO el estado actual del canvas.
     *
     * Orden de dibujo (de atrás hacia adelante):
     *   1. Fondo + grid
     *   2. Aristas (líneas entre nodos)
     *   3. Línea de arista en creación (drag)
     *   4. Glow de nodos (capa de resplandor)
     *   5. Cuerpo de nodos (círculos sólidos)
     *   6. Labels de nodos (texto)
     *   7. Indicadores especiales (paciente cero, súper spreader)
     *   8. Overlay de información (modo actual, instrucciones)
     */
    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        if (w <= 0 || h <= 0) return;

        // ── 1. Limpiar y dibujar fondo ────────────────────────────
        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, w, h);

        // ── 2. Grid de fondo ──────────────────────────────────────
        drawGrid(w, h);

        // ── 3. Aristas ────────────────────────────────────────────
        drawEdges();

        // ── 4. Línea de arista en creación ────────────────────────
        if (drawingEdge && edgeStartNode != null) {
            drawEdgePreview();
        }

        // ── 5. Glow de nodos (capa inferior) ─────────────────────
        drawNodeGlows();

        // ── 6. Cuerpo de nodos ────────────────────────────────────
        drawNodes();

        // ── 7. Labels ──────────────��──────────────────────────────
        drawLabels();

        // ── 8. Overlay de modo ────────────────────────────────────
        drawModeOverlay(w, h);
    }

    // ═══════════════════════════════════════════════════════════════
    //  DIBUJO: GRID DE FONDO
    // ═══════════════════════════════════════════════════════════════

    private void drawGrid(double w, double h) {
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(1.0);
        gc.setLineDashes(); // Sin dashes

        // Líneas verticales
        for (double x = GRID_SPACING; x < w; x += GRID_SPACING) {
            gc.strokeLine(x, 0, x, h);
        }

        // Líneas horizontales
        for (double y = GRID_SPACING; y < h; y += GRID_SPACING) {
            gc.strokeLine(0, y, w, y);
        }

        // Punto central de referencia
        double cx = w / 2, cy = h / 2;
        gc.setFill(Color.web(MainView.TEXT_MUTED, 0.08));
        gc.fillOval(cx - 3, cy - 3, 6, 6);
    }

    // ═══════════════════════════════════════════════════════════════
    //  DIBUJO: ARISTAS
    // ═══════════════════════════════════════════════════════════════

    private void drawEdges() {
        List<EdgeRecord> allEdges = model.getAllEdges();

        for (EdgeRecord edge : allEdges) {
            NodeData source = model.getNode(edge.sourceId());
            NodeData target = model.getNode(edge.targetId());
            if (source == null || target == null) continue;

            double x1 = source.getPosX();
            double y1 = source.getPosY();
            double x2 = target.getPosX();
            double y2 = target.getPosY();

            boolean disabled = model.getSimulationState().isEdgeDisabled(edge.id());

            if (disabled) {
                // ── Arista desactivada (cortafuegos) ──────────────
                gc.setStroke(EDGE_DISABLED);
                gc.setLineWidth(EDGE_WIDTH_DISABLED);
                gc.setLineDashes(EDGE_DASH_LENGTH, EDGE_DASH_GAP);
                gc.strokeLine(x1, y1, x2, y2);

                // Ícono de fuego en el centro
                double mx = (x1 + x2) / 2;
                double my = (y1 + y2) / 2;
                gc.setFill(Color.web(MainView.ACCENT_RED, 0.7));
                gc.setFont(Font.font("Segoe UI Emoji", 12));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                gc.fillText("🔥", mx, my);
            } else {
                // ── Arista normal ─────────────────────────────────
                Color edgeColor = getEdgeColor(source, target);
                gc.setStroke(edgeColor);
                gc.setLineWidth(EDGE_WIDTH_NORMAL);
                gc.setLineDashes(); // Sólida

                gc.strokeLine(x1, y1, x2, y2);

                // Si hay propagación activa, dibujar partícula viajera
                if (source.canSpread() || target.canSpread()) {
                    drawEdgeParticle(x1, y1, x2, y2, edgeColor);
                }
            }
        }

        // Resetear dashes
        gc.setLineDashes();
    }

    /**
     * Determina el color de una arista según el estado de sus nodos.
     */
    private Color getEdgeColor(NodeData source, NodeData target) {
        boolean sourceInfected = source.canSpread();
        boolean targetInfected = target.canSpread();

        if (sourceInfected && targetInfected) {
            // Ambos infectados: rojo brillante
            return Color.web(MainView.ACCENT_RED, 0.8);
        } else if (sourceInfected || targetInfected) {
            // Uno infectado: naranja (propagación en progreso)
            double pulse = 0.4 + 0.3 * Math.sin(animTime * PULSE_SPEED);
            return Color.web(MainView.ACCENT_ORANGE, pulse);
        } else if (source.getStatus() == NodeStatus.RECOVERED
                && target.getStatus() == NodeStatus.RECOVERED) {
            // Ambos recuperados: verde suave
            return Color.web(MainView.ACCENT_GREEN, 0.3);
        }

        return EDGE_DEFAULT;
    }

    /**
     * Dibuja una partícula que viaja por la arista (efecto de propagación).
     */
    private void drawEdgeParticle(double x1, double y1, double x2, double y2, Color color) {
        // La partícula va y viene usando una función seno
        double t = (Math.sin(animTime * PARTICLE_SPEED) + 1.0) / 2.0;
        double px = x1 + (x2 - x1) * t;
        double py = y1 + (y2 - y1) * t;

        double particleSize = 4.0;

        // Glow de la partícula
        gc.setFill(color.deriveColor(0, 1, 1, 0.3));
        gc.fillOval(px - particleSize * 2, py - particleSize * 2,
                    particleSize * 4, particleSize * 4);

        // Núcleo de la partícula
        gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.9));
        gc.fillOval(px - particleSize / 2, py - particleSize / 2,
                    particleSize, particleSize);
    }

    /**
     * Dibuja la línea de previsualización cuando se está creando una arista.
     */
    private void drawEdgePreview() {
        double x1 = edgeStartNode.getPosX();
        double y1 = edgeStartNode.getPosY();

        gc.setStroke(DRAG_LINE);
        gc.setLineWidth(2.0);
        gc.setLineDashes(6, 4);
        gc.strokeLine(x1, y1, mouseX, mouseY);
        gc.setLineDashes();

        // Círculo de destino
        gc.setStroke(DRAG_LINE);
        gc.setLineWidth(1.5);
        gc.strokeOval(mouseX - 10, mouseY - 10, 20, 20);
    }

    // ═══════════════════════════════════════════════════════════════
    //  DIBUJO: GLOW DE NODOS (Capa inferior de resplandor)
    // ═══════════════════════════════════════════════════════════════

    private void drawNodeGlows() {
        for (NodeData node : model.getAllNodes()) {
            double x = node.getPosX();
            double y = node.getPosY();
            double radius = getNodeRadius(node);
            NodeStatus status = node.getStatus();

            // Solo dibujar glow para nodos que no sean susceptibles
            if (status == NodeStatus.SUSCEPTIBLE && !node.isHovered() && !node.isSelected()) {
                continue;
            }

            double glowRadius = radius * GLOW_RADIUS_FACTOR;
            double glowOpacity = 0.25;

            // Pulso para nodos infectados
            if (status.canSpread()) {
                glowOpacity = 0.15 + 0.2 * Math.abs(Math.sin(animTime * PULSE_SPEED));
                glowRadius *= 1.0 + 0.15 * Math.abs(Math.sin(animTime * PULSE_SPEED));
            }

            // Glow más intenso para hover/selected
            if (node.isSelected()) {
                glowOpacity = 0.5;
                glowRadius *= 1.3;
            } else if (node.isHovered()) {
                glowOpacity = 0.35;
                glowRadius *= 1.15;
            }

            Color glowColor = status.getGlowColor().deriveColor(0, 1, 1, glowOpacity);

            RadialGradient glow = new RadialGradient(
                0, 0, x, y, glowRadius, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, glowColor),
                new Stop(0.5, glowColor.deriveColor(0, 1, 1, glowOpacity * 0.5)),
                new Stop(1.0, Color.TRANSPARENT)
            );

            gc.setFill(glow);
            gc.fillOval(x - glowRadius, y - glowRadius,
                        glowRadius * 2, glowRadius * 2);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DIBUJO: CUERPO DE NODOS (Círculos sólidos)
    // ═══════════════════════════════════════════════════════════════

    private void drawNodes() {
        for (NodeData node : model.getAllNodes()) {
            double x = node.getPosX();
            double y = node.getPosY();
            double radius = getNodeRadius(node);
            NodeStatus status = node.getStatus();

            // ── Borde exterior ────────────────────────────────────
            double borderWidth = node.isSelected() ? 3.0 : 2.0;
            Color borderColor = node.isSelected()
                ? Color.web(MainView.ACCENT_CYAN)
                : status.getBorderColor();

            gc.setStroke(borderColor);
            gc.setLineWidth(borderWidth);
            gc.setLineDashes();
            gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);

            // ── Relleno con gradiente radial ──────────────────────
            RadialGradient fill = new RadialGradient(
                0, 0, x, y, radius, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, status.getGlowColor().deriveColor(0, 1, 1.2, 0.9)),
                new Stop(0.4, status.getPrimaryColor()),
                new Stop(1.0, status.getBorderColor())
            );

            gc.setFill(fill);
            gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

            // ── Reflejo de luz (highlight) ────────────────────────
            double hlSize = radius * 0.5;
            double hlOffset = radius * 0.3;
            RadialGradient highlight = new RadialGradient(
                0, 0,
                x - hlOffset, y - hlOffset, hlSize,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE.deriveColor(0, 1, 1, 0.3)),
                new Stop(1.0, Color.TRANSPARENT)
            );
            gc.setFill(highlight);
            gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

            // ── Ícono del estado (centro del nodo) ────────────────
            gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.9));
            gc.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, radius * 0.7));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(status.getIcon(), x, y);

            // ── Indicador de profundidad BFS (esquina) ────────────
            if (node.getDepthLevel() >= 0) {
                drawDepthBadge(x, y, radius, node.getDepthLevel());
            }

            // ── Corona de súper propagador ────────────────────────
            if (node.isSuperSpreader()) {
                drawSuperSpreaderCrown(x, y, radius);
            }
        }
    }

    /**
     * Dibuja la insignia de profundidad BFS en la esquina inferior derecha del nodo.
     */
    private void drawDepthBadge(double x, double y, double radius, int depth) {
        double badgeX = x + radius * 0.55;
        double badgeY = y + radius * 0.55;
        double badgeR = 8.0;

        // Fondo del badge
        Color badgeColor = getDepthColor(depth);
        gc.setFill(badgeColor);
        gc.fillOval(badgeX - badgeR, badgeY - badgeR, badgeR * 2, badgeR * 2);

        // Borde
        gc.setStroke(Color.web(MainView.BG_CANVAS));
        gc.setLineWidth(1.5);
        gc.strokeOval(badgeX - badgeR, badgeY - badgeR, badgeR * 2, badgeR * 2);

        // Número
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(String.valueOf(depth), badgeX, badgeY);
    }

    /**
     * Retorna el color del mapa de calor según la profundidad BFS.
     *
     *   Profundidad 0: Rojo oscuro (origen)
     *   Profundidad 1: Rojo
     *   Profundidad 2: Naranja
     *   Profundidad 3: Amarillo
     *   Profundidad 4: Verde amarillo
     *   Profundidad 5+: Verde → Cyan
     */
    public static Color getDepthColor(int depth) {
        return switch (depth) {
            case 0  -> Color.web("#B71C1C");      // Rojo oscuro (origen)
            case 1  -> Color.web("#D32F2F");      // Rojo
            case 2  -> Color.web("#F57C00");      // Naranja
            case 3  -> Color.web("#FBC02D");      // Amarillo
            case 4  -> Color.web("#C0CA33");      // Verde amarillo
            case 5  -> Color.web("#7CB342");      // Verde claro
            case 6  -> Color.web("#43A047");      // Verde
            case 7  -> Color.web("#00897B");      // Teal
            case 8  -> Color.web("#0097A7");      // Cyan oscuro
            default -> Color.web("#00ACC1");      // Cyan (profundo)
        };
    }

    /**
     * Dibuja una corona dorada sobre el nodo súper propagador.
     */
    private void drawSuperSpreaderCrown(double x, double y, double radius) {
        double crownY = y - radius - 8;
        gc.setFill(Color.web("#FFD700", 0.9));
        gc.setFont(Font.font("Segoe UI Emoji", 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("👑", x, crownY);
    }

    // ═══════════════════════════════════════════════════════════════
    //  DIBUJO: LABELS DE NODOS
    // ═══════════════════════════════════════════════════════════════

    private void drawLabels() {
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.TOP);

        for (NodeData node : model.getAllNodes()) {
            double x = node.getPosX();
            double y = node.getPosY() + LABEL_OFFSET_Y;
            String label = node.getLabel();

            // Fondo semi-transparente del label
            double textWidth = label.length() * 7.0 + 12;
            double textHeight = 16.0;
            double bgX = x - textWidth / 2;
            double bgY = y - 2;

            gc.setFill(Color.web(MainView.BG_PRIMARY, 0.85));
            gc.fillRoundRect(bgX, bgY, textWidth, textHeight, 6, 6);

            // Borde sutil del label
            gc.setStroke(Color.web(MainView.BORDER_COLOR, 0.5));
            gc.setLineWidth(0.5);
            gc.strokeRoundRect(bgX, bgY, textWidth, textHeight, 6, 6);

            // Texto
            Color textColor = node.isSelected()
                ? Color.web(MainView.ACCENT_CYAN)
                : Color.web(MainView.TEXT_PRIMARY, 0.9);
            gc.setFill(textColor);
            gc.fillText(label, x, y);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DIBUJO: OVERLAY DE MODO ACTUAL
    // ═══════════════════════════════════════════════════════════════

    private void drawModeOverlay(double w, double h) {
        String modeText = null;
        String modeColor = MainView.TEXT_MUTED;

        if (model.isEdgeCreationMode()) {
            modeText = "🔗 MODO: CREAR ARISTA — Arrastra entre nodos";
            modeColor = MainView.ACCENT_CYAN;
        } else if (model.isPatientZeroMode()) {
            modeText = "☣ MODO: PACIENTE CERO — Clic en nodos para seleccionar origen";
            modeColor = MainView.ACCENT_RED;
        } else if (model.isQuarantineMode()) {
            modeText = "⊘ MODO: CUARENTENA — Clic en nodos para aislar";
            modeColor = MainView.ACCENT_YELLOW;
        }

        if (modeText != null) {
            // Barra de modo en la parte superior del canvas
            double barH = 32;
            gc.setFill(Color.web(MainView.BG_PRIMARY, 0.88));
            gc.fillRect(0, 0, w, barH);

            // Línea de acento
            gc.setFill(Color.web(modeColor, 0.8));
            gc.fillRect(0, barH - 2, w, 2);

            // Texto
            gc.setFill(Color.web(modeColor));
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(modeText, w / 2, barH / 2);
        }

        // Mensaje cuando el grafo está vacío
        if (model.isEmpty()) {
            gc.setFill(Color.web(MainView.TEXT_MUTED, 0.4));
            gc.setFont(Font.font("Consolas", FontWeight.LIGHT, 18));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText("Clic en '⬡ Crear Nodo' y luego clic aquí", w / 2, h / 2 - 16);
            gc.fillText("para agregar nodos a la red", w / 2, h / 2 + 16);

            gc.setFont(Font.font("Consolas", FontWeight.LIGHT, 13));
            gc.setFill(Color.web(MainView.TEXT_MUTED, 0.25));
            gc.fillText("o usa '🌐 Topologías' para generar una red automática", w / 2, h / 2 + 52);
        }
    }

    // ══════════════════════════════════���════════════════════════════
    //  UTILIDADES DE RENDERIZADO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Calcula el radio visual de un nodo según su estado.
     */
    private double getNodeRadius(NodeData node) {
        double base = NODE_RADIUS;

        // Escalar según grado (nodos con más conexiones son más grandes)
        int degree = node.getDegree();
        if (degree > 3) {
            base += Math.min(degree * 0.8, 8.0);
        }

        // Estado de interacción
        if (node.isSelected()) {
            return Math.max(base, NODE_RADIUS_SELECTED);
        } else if (node.isHovered()) {
            return Math.max(base, NODE_RADIUS_HOVER);
        }

        return base;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONTROL DEL RENDER LOOP
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inicia el loop de renderizado.
     */
    public void startRendering() {
        if (!rendering) {
            renderLoop.start();
            rendering = true;
        }
    }

    /**
     * Detiene el loop de renderizado.
     */
    public void stopRendering() {
        if (rendering) {
            renderLoop.stop();
            rendering = false;
        }
    }

    /**
     * Fuerza un re-render inmediato (fuera del loop).
     */
    public void forceRender() {
        render();
    }

    // ═══════════════════════════════════════════════════════════════
    //  INTERACCIÓN — Estado del mouse
    //  (Los eventos se registran desde el Controller)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualiza las coordenadas del mouse.
     * Llamado desde el controller en mouseMoved/mouseDragged.
     */
    public void updateMousePosition(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;

        // Detectar hover
        NodeData newHover = model.findNodeAt(x, y);

        if (hoveredNode != newHover) {
            // Limpiar hover anterior
            if (hoveredNode != null) {
                hoveredNode.setHovered(false);
            }
            // Aplicar nuevo hover
            hoveredNode = newHover;
            if (hoveredNode != null) {
                hoveredNode.setHovered(true);
            }
        }
    }

    /**
     * Inicia el arrastre de un nodo.
     */
    public void startDrag(NodeData node) {
        this.dragging = true;
        this.draggedNode = node;
    }

    /**
     * Actualiza la posición del nodo siendo arrastrado.
     */
    public void updateDrag(double x, double y) {
        if (dragging && draggedNode != null) {
            draggedNode.setPosX(x);
            draggedNode.setPosY(y);
        }
    }

    /**
     * Finaliza el arrastre.
     */
    public void endDrag() {
        this.dragging = false;
        this.draggedNode = null;
    }

    /**
     * Inicia el dibujo de una nueva arista.
     */
    public void startEdgeDrawing(NodeData sourceNode) {
        this.drawingEdge = true;
        this.edgeStartNode = sourceNode;
    }

    /**
     * Finaliza el dibujo de arista.
     */
    public void endEdgeDrawing() {
        this.drawingEdge = false;
        this.edgeStartNode = null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  GETTERS
    // ═══════════════════════════════════════════════════════════════

    public StackPane getRoot()           { return root; }
    public Canvas    getCanvas()         { return canvas; }
    public double    getMouseX()         { return mouseX; }
    public double    getMouseY()         { return mouseY; }
    public boolean   isDragging()        { return dragging; }
    public boolean   isDrawingEdge()     { return drawingEdge; }
    public NodeData  getDraggedNode()    { return draggedNode; }
    public NodeData  getEdgeStartNode()  { return edgeStartNode; }
    public NodeData  getHoveredNode()    { return hoveredNode; }
    public double    getCanvasWidth()    { return canvas.getWidth(); }
    public double    getCanvasHeight()   { return canvas.getHeight(); }
}